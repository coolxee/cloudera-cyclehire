package com.cloudera.cycelhire.main.ingress.copy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.cycelhire.main.common.Counter;
import com.cloudera.cycelhire.main.common.Driver;
import com.cloudera.cyclehire.main.common.hdfs.HDFSClientUtil;

public class CopyDriver extends Driver {

  public static final String CONF_DIR_INCLUDE = "dir.include";
  public static final String CONF_BLOCK_SINGLE = "block.single";
  public static final String CONF_TIMEOUT_SECS = "timeout.secs";
  public static final String CONF_THREAD_NUMBER = "thread.number";
  public static final String CONF_THREAD_QUEUE = "thread.queue";
  public static final String CONF_THREAD_QUEUE_FILE = "file";
  public static final String CONF_THREAD_QUEUE_DIR = "dir";

  private static final Logger log = LoggerFactory.getLogger(CopyDriver.class);

  private Path hdfsLandingPath;
  private List<FileSetCopy> localLandingFileSets;

  private Map<Long, FileSystem> fileSystems = new HashMap<Long, FileSystem>();

  public CopyDriver() {
    super();
  }

  public CopyDriver(Configuration confguration) {
    super(confguration);
  }

  @Override
  public String description() {
    return "Copy a set of files into HDFS in an rsync manner";
  }

  @Override
  public String[] options() {
    return new String[] { CONF_DIR_INCLUDE + "=true|false", CONF_BLOCK_SINGLE + "=true|false",
        CONF_THREAD_QUEUE + "=" + CONF_THREAD_QUEUE_FILE + "|" + CONF_THREAD_QUEUE_DIR,
        CONF_THREAD_NUMBER + "=integer", CONF_TIMEOUT_SECS + "=integer" };
  }

  @Override
  public String[] paramaters() {
    return new String[] { "local-dir-landing ...", "hdfs-dir-landing" };
  }

  @Override
  public void reset() {
    super.reset();
    for (Counter counter : new Counter[] { Counter.FILES_PENDING, Counter.FILES_SKIPPED, Counter.FILES_FAILED,
        Counter.FILES_SUCCESSFUL, Counter.FILES }) {
      incramentCounter(CopyDriver.class.getCanonicalName(), counter, 0);
    }
    localLandingFileSets = new ArrayList<FileSetCopy>();
  }

  @Override
  public int prepare(String... arguments) throws Exception {

    if (arguments == null || arguments.length < 2) {
      throw new Exception("Invalid number of arguments");
    }

    FileSystem hdfs = getFileSystem();

    hdfsLandingPath = new Path(arguments[arguments.length - 1]);
    if (hdfs.exists(hdfsLandingPath)) {
      if (!hdfs.isDirectory(hdfsLandingPath)) {
        throw new Exception("HDFS landing directory [" + hdfsLandingPath + "] is not a directory");
      }
      if (!HDFSClientUtil.canDoAction(hdfs, UserGroupInformation.getCurrentUser().getUserName(), UserGroupInformation
          .getCurrentUser().getGroupNames(), hdfsLandingPath, FsAction.ALL)) {
        throw new Exception("HDFS landing directory [" + hdfsLandingPath
            + "] has too restrictive permissions to read/write as user ["
            + UserGroupInformation.getCurrentUser().getUserName() + "]");
      }
    } else {
      hdfs.mkdirs(hdfsLandingPath, new FsPermission(FsAction.ALL, FsAction.READ_EXECUTE, FsAction.READ_EXECUTE));
    }
    if (log.isInfoEnabled()) {
      log.info("HDFS landing directory [" + hdfsLandingPath + "] validated");
    }

    List<File> localLandingDirs = new ArrayList<File>();
    for (int i = 0; i < arguments.length - 1; i++) {
      File file = new File(arguments[i]);
      if (!file.exists() || !file.canRead() || !file.isDirectory()) {
        throw new Exception("Local directory [" + arguments[i] + "] cannot be read");
      }
      localLandingDirs.add(file);
    }
    boolean isDirIncluded = getConf().getBoolean(CONF_DIR_INCLUDE, false);
    boolean isFileQueue = getConf().get(CONF_THREAD_QUEUE, CONF_THREAD_QUEUE_FILE).equals(CONF_THREAD_QUEUE_FILE);
    for (File localLandingDir : localLandingDirs) {
      FileSetCopy localLandingFileSet = new FileSetCopy(hdfsLandingPath.toString()
          + (isDirIncluded ? "/" + localLandingDir.getName() : ""));
      localLandingFileSets.add(localLandingFileSet);
      for (File localLandingFile : localLandingDir.listFiles()) {
        if (localLandingFile.isFile() && localLandingFile.canRead()) {
          localLandingFileSet.addFile(localLandingFile);
          if (isFileQueue) {
            localLandingFileSets.add(localLandingFileSet = new FileSetCopy(hdfsLandingPath.toString()
                + (isDirIncluded ? "/" + localLandingDir.getName() : "")));
          }
        }
      }
    }
    Set<String> hdfsLandingNamespace = new HashSet<String>();
    List<String> hdfsLandingNamespaceClash = new ArrayList<String>();
    for (int i = 0; i < localLandingFileSets.size(); i++) {
      if (localLandingFileSets.get(i).getFiles().isEmpty()) {
        localLandingFileSets.remove(i);
      } else if (!isDirIncluded) {
        for (File file : localLandingFileSets.get(i).getFiles()) {
          if (hdfsLandingNamespace.contains(file.getName())) {
            hdfsLandingNamespaceClash.add(file.getName());
          } else {
            hdfsLandingNamespace.add(file.getName());
          }
        }
      }
    }
    if (!hdfsLandingNamespaceClash.isEmpty()) {
      throw new Exception("File namespace clashes detected with " + hdfsLandingNamespaceClash
          + ", consider including directories in ingress path or file renaming");
    }
    if (log.isInfoEnabled()) {
      log.info("Local landing directories " + localLandingDirs + " validated");
    }

    return RETURN_SUCCESS;
  }

  @Override
  public int execute() throws InterruptedException, ExecutionException {

    int numberThreads = getConf().getInt(CONF_THREAD_NUMBER, 1);
    List<Future<FileSetCopy>> fileSetCopyFutures = new ArrayList<Future<FileSetCopy>>();
    ExecutorService fileSetCopyExecutor = new ThreadPoolExecutor(numberThreads, numberThreads, 0L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>());
    for (FileSetCopy fileSetCopy : localLandingFileSets) {
      fileSetCopyFutures.add(fileSetCopyExecutor.submit(fileSetCopy));
    }
    fileSetCopyExecutor.shutdown();
    if (!fileSetCopyExecutor.awaitTermination(getConf().getInt(CONF_TIMEOUT_SECS, 600) * localLandingFileSets.size(),
        TimeUnit.SECONDS)) {
      fileSetCopyExecutor.shutdownNow();
    }
    for (Future<FileSetCopy> fileSetCopyFuture : fileSetCopyFutures) {
      FileSetCopy fileSetCopy = fileSetCopyFuture.get();
      for (File file : fileSetCopy.getFiles()) {
        incramentCounter(CopyDriver.class.getCanonicalName(), fileSetCopy.getFileStatus(file), 1);
        incramentCounter(CopyDriver.class.getCanonicalName(), Counter.FILES, 1);
      }
    }

    return RETURN_SUCCESS;
  }

  @Override
  public int cleanup() throws IOException {
    closeFileSystems();
    return RETURN_SUCCESS;
  }

  public static void main(String... arguments) throws Exception {
    System.exit(new CopyDriver().runner(arguments));
  }

  private synchronized FileSystem getFileSystem() throws IOException {
    FileSystem fileSystem = fileSystems.get(Thread.currentThread().getId());
    if (fileSystem == null) {
      fileSystems.put(Thread.currentThread().getId(), fileSystem = FileSystem.newInstance(getConf()));
    }
    return fileSystem;
  }

  private synchronized void closeFileSystems() throws IOException {
    for (FileSystem fileSystem : fileSystems.values()) {
      fileSystem.close();
    }
    fileSystems.clear();
  }

  @SuppressWarnings("unused")
  private class FileSetCopy implements Callable<FileSetCopy> {

    private String dir;
    private Map<File, Counter> files = new HashMap<File, Counter>();

    public FileSetCopy(String dir) {
      super();
      this.dir = dir;
    }

    public String getDir() {
      return dir;
    }

    public Set<File> getFiles() {
      return files.keySet();
    }

    public Counter getFileStatus(File file) {
      return files.get(file);
    }

    public void addFile(File file) {
      files.put(file, Counter.FILES_PENDING);
    }

    @Override
    public FileSetCopy call() throws Exception {
      FileSystem hdfs = getFileSystem();
      for (File file : files.keySet()) {
        long fileSize = file.length();
        String fileName = file.getName();
        Path dirTo = new Path(dir
            + (fileName.indexOf('.') == -1 ? "" : fileName.substring(fileName.indexOf('.'), fileName.length()).replace(
                '.', '/')), fileName);
        Path fileFrom = new Path(file.getAbsolutePath());
        Path fileTo = new Path(dirTo, fileName);
        try {
          if (HDFSClientUtil.copyFromLocalFile(
              hdfs,
              fileFrom,
              fileTo,
              true,
              fileSize,
              getConf().getInt("io.file.buffer.size", 8192),
              getConf().getInt("dfs.replication", 3),
              getConf().getBoolean(CONF_BLOCK_SINGLE, false) ? fileSize + 1024 : getConf().getLong(
                  "fs.local.block.size", 33554432L))) {
            files.put(file, Counter.FILES_SUCCESSFUL);
          } else {
            files.put(file, Counter.FILES_SKIPPED);
          }
        } catch (Exception exception) {
          files.put(file, Counter.FILES_FAILED);
          if (log.isErrorEnabled()) {
            log.error("File could not be processed with fatal error", exception);
          }
        } finally {
          if (log.isInfoEnabled()) {
            log.info("File ingress [" + files.get(file) + "] from [" + fileFrom + "] to [" + fileTo + "]");
          }
        }
      }
      return this;
    }

    @Override
    public String toString() {
      StringBuilder string = new StringBuilder();
      string.append("{");
      string.append(dir);
      string.append(", [");
      string.append(StringUtils.join(files.keySet(), ","));
      string.append("]]");
      return string.toString();
    }

  }

}
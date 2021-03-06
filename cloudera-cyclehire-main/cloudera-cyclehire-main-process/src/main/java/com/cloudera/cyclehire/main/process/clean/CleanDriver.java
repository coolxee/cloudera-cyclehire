package com.cloudera.cyclehire.main.process.clean;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.cyclehire.main.common.Counter;
import com.cloudera.cyclehire.main.common.Driver;
import com.cloudera.cyclehire.main.common.hdfs.HDFSClientUtil;
import com.cloudera.cyclehire.main.common.model.PartitionFlag;
import com.cloudera.cyclehire.main.common.model.PartitionKey;

public class CleanDriver extends Driver {

  public static final Counter[] COUNTERS = new Counter[] {
      Counter.FILES_SKIPPED, Counter.FILES_FAILED, Counter.FILES_SUCCESSFUL,
      Counter.FILES, Counter.BATCHES_SKIPPED, Counter.BATCHES_FAILED,
      Counter.BATCHES_SUCCESSFUL, Counter.BATCHES, Counter.PARTITIONS_SKIPPED,
      Counter.PARTITIONS_FAILED, Counter.PARTITIONS_SUCCESSFUL,
      Counter.PARTITIONS };

  private static final Logger log = LoggerFactory.getLogger(CleanDriver.class);

  private Path inputLandedPath;
  private Path inputStagedPath;

  public CleanDriver() {
    super();
  }

  public CleanDriver(Configuration confguration) {
    super(confguration);
  }

  @Override
  public String description() {
    return "Clean a set of files";
  }

  @Override
  public String[] options() {
    return new String[] {};
  }

  @Override
  public String[] paramaters() {
    return new String[] { "hdfs-dir-landed", "hdfs-dir-staged" };
  }

  @Override
  public void reset() {
    super.reset();
    for (Counter counter : COUNTERS) {
      incrementCounter(counter, 0);
    }
  }

  @Override
  public int prepare(String... arguments) throws Exception {

    if (arguments == null || arguments.length != 2) {
      throw new Exception("Invalid number of arguments");
    }

    FileSystem hdfs = FileSystem.newInstance(getConf());

    inputLandedPath = new Path(arguments[0]);
    if (!hdfs.exists(inputLandedPath)
        || !HDFSClientUtil.canDoAction(hdfs, UserGroupInformation
            .getCurrentUser().getUserName(), UserGroupInformation
            .getCurrentUser().getGroupNames(), inputLandedPath, FsAction.READ)) {
      throw new Exception("HDFS landed directory [" + inputLandedPath
          + "] not available to user ["
          + UserGroupInformation.getCurrentUser().getUserName() + "]");
    }
    if (log.isInfoEnabled()) {
      log.info("HDFS landed directory [" + inputLandedPath + "] validated");
    }

    inputStagedPath = new Path(arguments[1]);
    if (!hdfs.exists(inputStagedPath)
        || !HDFSClientUtil.canDoAction(hdfs, UserGroupInformation
            .getCurrentUser().getUserName(), UserGroupInformation
            .getCurrentUser().getGroupNames(), inputStagedPath, FsAction.READ)) {
      throw new Exception("HDFS landed directory [" + inputStagedPath
          + "] not available to user ["
          + UserGroupInformation.getCurrentUser().getUserName() + "]");
    }
    if (log.isInfoEnabled()) {
      log.info("HDFS staged directory [" + inputStagedPath + "] validated");
    }

    return RETURN_SUCCESS;
  }

  @Override
  public int execute() throws InterruptedException, ExecutionException,
      IOException, ClassNotFoundException {

    FileSystem hdfs = FileSystem.newInstance(getConf());

    Set<String> counterFiles = new HashSet<String>();
    Set<String> counterBatches = new HashSet<String>();
    Set<String> counterPartitions = new HashSet<String>();
    Map<Path, PartitionKey> stagedCleaned = new HashMap<>();
    Map<Path, PartitionKey> landedCleaned = new HashMap<>();
    Map<Path, PartitionKey> stagedTodo = new HashMap<>();
    for (Path landedPath : HDFSClientUtil
        .listFiles(hdfs, inputLandedPath, true)) {
      if (!PartitionFlag.isValue(landedPath.getName())) {
        for (PartitionKey partitionKey : PartitionKey.getKeys(landedPath
            .getParent().getName(), landedPath.getName())) {
          boolean landedPathExists = hdfs.exists(new Path(landedPath
              .getParent(), partitionKey.getRecord()));
          Path stagedPath = new Path(
              new StringBuilder(PartitionKey.PATH_NOMINAL_LENGTH)
                  .append(inputStagedPath)
                  .append('/')
                  .append(
                      partitionKey.isValid() && landedPathExists ? Counter.BATCHES_SUCCESSFUL
                          .getPath() : Counter.BATCHES_FAILED.getPath())
                  .append('/').append(partitionKey.getPath()).toString());
          if (PartitionFlag.list(hdfs, landedPath, PartitionFlag._SUCCESS)
              && !PartitionFlag
                  .list(hdfs, stagedPath, PartitionFlag._PARTITION)) {
            landedCleaned.put(landedPath, partitionKey);
            stagedCleaned.put(stagedPath, partitionKey);
            if (landedPathExists) {
              incrementCounter(Counter.FILES_SUCCESSFUL, 1,
                  partitionKey.getBatch() + '/' + partitionKey.getRecord(),
                  counterFiles);
            }
          } else {
            stagedTodo.put(stagedPath, partitionKey);
            if (landedPathExists) {
              incrementCounter(Counter.FILES_SKIPPED, 1,
                  partitionKey.getBatch() + '/' + partitionKey.getRecord(),
                  counterFiles);
            }
          }
        }
      }
    }
    for (Path stagedPath : stagedCleaned.keySet()) {
      PartitionKey partitionKey = stagedCleaned.get(stagedPath);
      hdfs.delete(stagedPath.getParent(), true);
      incrementCounter(Counter.BATCHES_SUCCESSFUL, 1,
          partitionKey.getPartition() + '/' + partitionKey.getBatch(),
          counterBatches);
      incrementCounter(Counter.PARTITIONS_SUCCESSFUL, 1,
          partitionKey.getPartition(), counterPartitions);
    }
    for (Path landedPath : landedCleaned.keySet()) {
      hdfs.delete(landedPath.getParent(), true);
    }
    for (Path stagedPath : stagedTodo.keySet()) {
      PartitionKey partitionKey = stagedTodo.get(stagedPath);
      incrementCounter(Counter.BATCHES_SKIPPED, 1, partitionKey.getPartition()
          + '/' + partitionKey.getBatch(), counterBatches);
      incrementCounter(Counter.PARTITIONS_SKIPPED, 1,
          partitionKey.getPartition(), counterPartitions);
    }
    incrementCounter(Counter.FILES, counterFiles.size());
    incrementCounter(Counter.BATCHES, counterBatches.size());
    incrementCounter(Counter.PARTITIONS, counterPartitions.size());

    return RETURN_SUCCESS;

  }

  @Override
  public int cleanup() throws IOException {
    return RETURN_SUCCESS;
  }

  public static void main(String... arguments) throws Exception {
    System.exit(new CleanDriver().runner(arguments));
  }

}

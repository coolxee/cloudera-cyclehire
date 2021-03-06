package com.cloudera.cyclehire.main.process;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.cyclehire.main.common.Counter;
import com.cloudera.cyclehire.main.common.Driver;
import com.cloudera.cyclehire.main.common.hdfs.HDFSClientUtil;
import com.cloudera.cyclehire.main.process.clean.CleanDriver;
import com.cloudera.cyclehire.main.process.cleanse.CleanseDriver;
import com.cloudera.cyclehire.main.process.partition.PartitionDriver;
import com.cloudera.cyclehire.main.process.stage.StageDriver;
import com.cloudera.cyclehire.main.test.BaseTestCase;

public class ProcessTest extends BaseTest {

  protected Driver cleanDriver;
  protected Driver processDriver;

  public ProcessTest() throws IOException {
    super();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cleanDriver = new CleanDriver(getFileSystem().getConf());
    processDriver = new ProcessDriver(getFileSystem().getConf());
  }

  @Test
  public void testProcessInvalid() {
    Assert.assertEquals(Driver.RETURN_FAILURE_RUNTIME,
        processDriver.runner(new String[0]));
    Assert.assertEquals(Driver.RETURN_FAILURE_RUNTIME, processDriver
        .runner(new String[] { BaseTestCase.PATH_HDFS_DIR_RAW_STAGED }));
    Assert.assertEquals(
        Driver.RETURN_FAILURE_RUNTIME,
        processDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_NON_EXISTANT,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED }));
    Assert.assertEquals(
        Driver.RETURN_FAILURE_RUNTIME,
        processDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDED,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED }));
  }

  @Test
  public void testProcessValid() throws FileNotFoundException,
      IllegalArgumentException, IOException {

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        processDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDED,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED }));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS_MALFORMED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_DUPLICATE)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_CLEANSED));

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testProcessValidRinseRepeat() throws FileNotFoundException,
      IllegalArgumentException, IOException {

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        processDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDED,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED }));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS_MALFORMED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_DUPLICATE)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_CLEANSED));

    processDriver.reset();

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        processDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDED,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED }));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS_MALFORMED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_DUPLICATE)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_CLEANSED));

    processDriver.reset();

    List<Path> stagedPaths = HDFSClientUtil.listFiles(getFileSystem(),
        new Path(BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            Counter.BATCHES_SUCCESSFUL.getPath()), true);
    Collections.sort(stagedPaths);
    Path stagedPathToDelete = stagedPaths.get(1).getParent();
    getFileSystem().delete(stagedPathToDelete, true);
    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        processDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDED,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED }));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS_MALFORMED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_DUPLICATE)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_CLEANSED));

    processDriver.reset();

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        cleanDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDED,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED }));
    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        processDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDED,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGED,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONED,
            BaseTestCase.PATH_HDFS_DIR_PROCESSED }));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.FILES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.FILES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(StageDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(StageDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS).longValue(),
        processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
            Counter.RECORDS_MALFORMED)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_DUPLICATE)
            + processDriver.getCounter(CleanseDriver.class.getCanonicalName(),
                Counter.RECORDS_CLEANSED));

  }

}

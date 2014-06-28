package com.cloudera.cycelhire.main.process;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.cycelhire.main.common.Counter;
import com.cloudera.cycelhire.main.common.Driver;
import com.cloudera.cycelhire.main.process.partition.PartitionDriver;
import com.cloudera.cycelhire.main.process.stage.StageDriver;
import com.cloudera.cyclehire.main.common.hdfs.HDFSClientUtil;
import com.cloudera.cyclehire.main.test.BaseTestCase;

public class PartitionTest extends BaseTest {

  protected Driver stageDriver;
  protected Driver partitionDriver;

  public PartitionTest() throws IOException {
    super();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    stageDriver = new StageDriver(getFileSystem().getConf());
    partitionDriver = new PartitionDriver(getFileSystem().getConf());
    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        stageDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDING,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING }));
  }

  @Test
  public void testPartitionInvalid() {
    Assert.assertEquals(Driver.RETURN_FAILURE_RUNTIME,
        partitionDriver.runner(new String[0]));
    Assert.assertEquals(Driver.RETURN_FAILURE_RUNTIME, partitionDriver
        .runner(new String[] { BaseTestCase.PATH_HDFS_DIR_RAW_STAGING }));
    Assert.assertEquals(
        Driver.RETURN_FAILURE_RUNTIME,
        partitionDriver.runner(new String[] {
            BaseTestCase.PATH_LOCAL_DIR_NON_EXISTANT,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING }));
    Assert.assertEquals(
        Driver.RETURN_FAILURE_RUNTIME,
        partitionDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING }));
  }

  @Test
  public void testPartitionValid() throws FileNotFoundException,
      IllegalArgumentException, IOException {

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        partitionDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING }));
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.BATCHES_SKIPPED)
        .longValue() > 0);
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.BATCHES_FAILED) > 0);
    Assert
        .assertTrue(partitionDriver.getCounter(
            PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SUCCESSFUL) > 0);
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.BATCHES) > 0);
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED)
        .longValue() > 0);
    Assert
        .assertTrue(partitionDriver.getCounter(
            PartitionDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED) > 0);
    Assert
        .assertTrue(partitionDriver.getCounter(
            PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SUCCESSFUL) > 0);
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.PARTITIONS) > 0);

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPartitionValidRinseRepeat() throws FileNotFoundException,
      IllegalArgumentException, IOException, InterruptedException {

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        partitionDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING }));
    long batchesCount = partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.BATCHES).longValue();
    long partitionsCount = partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.PARTITIONS)
        .longValue();
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.BATCHES_SKIPPED)
        .longValue() > 0);
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.BATCHES_FAILED) > 0);
    Assert
        .assertTrue(partitionDriver.getCounter(
            PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SUCCESSFUL) > 0);
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.BATCHES) > 0);
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED)
        .longValue() > 0);
    Assert
        .assertTrue(partitionDriver.getCounter(
            PartitionDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED) > 0);
    Assert
        .assertTrue(partitionDriver.getCounter(
            PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SUCCESSFUL) > 0);
    Assert.assertTrue(partitionDriver.getCounter(
        PartitionDriver.class.getCanonicalName(), Counter.PARTITIONS) > 0);

    partitionDriver.reset();

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        partitionDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING }));
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        batchesCount,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED).longValue());
    Assert.assertEquals(
        0L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_FAILED).longValue());
    Assert.assertEquals(
        0L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SUCCESSFUL).longValue());
    Assert.assertEquals(
        batchesCount,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue());
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        partitionsCount,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED).longValue());
    Assert.assertEquals(
        0L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_FAILED).longValue());
    Assert.assertEquals(
        0L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SUCCESSFUL).longValue());
    Assert.assertEquals(
        partitionsCount,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue());

    partitionDriver.reset();

    List<Path> stagingPaths = HDFSClientUtil.listFiles(getFileSystem(),
        new Path(BaseTestCase.PATH_HDFS_DIR_RAW_STAGING,
            Counter.BATCHES_SUCCESSFUL.getPath()), true);
    Collections.sort(stagingPaths);
    Path stagingPathToDelete = stagingPaths.get(3).getParent();
    getFileSystem().delete(stagingPathToDelete, true);
    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        stageDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDING,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING }));
    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        partitionDriver.runner(new String[] {
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING,
            BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING }));
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.BATCHES_SUCCESSFUL));
    Assert.assertEquals(
        batchesCount - 1,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SKIPPED).longValue());
    Assert.assertEquals(
        0L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_FAILED).longValue());
    Assert.assertEquals(
        1L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES_SUCCESSFUL).longValue());
    Assert.assertEquals(
        batchesCount,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.BATCHES).longValue());
    Assert.assertEquals(
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue(),
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_FAILED)
            + partitionDriver.getCounter(
                PartitionDriver.class.getCanonicalName(),
                Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(
        partitionsCount - 1,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SKIPPED).longValue());
    Assert.assertEquals(
        0L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_FAILED).longValue());
    Assert.assertEquals(
        1L,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS_SUCCESSFUL).longValue());
    Assert.assertEquals(
        partitionsCount,
        partitionDriver.getCounter(PartitionDriver.class.getCanonicalName(),
            Counter.PARTITIONS).longValue());

  }

}

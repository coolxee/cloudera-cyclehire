package com.cloudera.cycelhire.main.process;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.cycelhire.main.common.Counter;
import com.cloudera.cycelhire.main.common.Driver;
import com.cloudera.cycelhire.main.process.stage.StageDriver;
import com.cloudera.cyclehire.main.common.hdfs.HDFSClientUtil;
import com.cloudera.cyclehire.main.test.BaseTestCase;

public class StageTest extends BaseTest {

  protected Driver stageDriver;

  public StageTest() throws IOException {
    super();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    stageDriver = new StageDriver(getFileSystem().getConf());
  }

  @Test
  public void testStageInvalid() {
    Assert.assertEquals(Driver.RETURN_FAILURE_RUNTIME, stageDriver.runner(new String[0]));
    Assert.assertEquals(Driver.RETURN_FAILURE_RUNTIME,
        stageDriver.runner(new String[] { BaseTestCase.PATH_HDFS_DIR_RAW_LANDING }));
    Assert.assertEquals(
        Driver.RETURN_FAILURE_RUNTIME,
        stageDriver.runner(new String[] { BaseTestCase.PATH_HDFS_DIR_NON_EXISTANT,
            BaseTestCase.PATH_HDFS_DIR_RAW_LANDING }));
    Assert.assertEquals(
        Driver.RETURN_FAILURE_RUNTIME,
        stageDriver.runner(new String[] { BaseTestCase.PATH_HDFS_DIR_RAW_LANDING,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING, BaseTestCase.PATH_HDFS_DIR_RAW_PARTITIONING }));
  }

  @Test
  public void testStageValid() throws FileNotFoundException, IllegalArgumentException, IOException {

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        stageDriver.runner(new String[] { BaseTestCase.PATH_HDFS_DIR_RAW_LANDING,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING }));
    long filesCount = stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES).longValue();
    long partitionsCount = stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS).longValue();
    Assert.assertEquals(
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES).longValue(),
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SKIPPED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_FAILED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SUCCESSFUL));
    Assert.assertTrue(stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SKIPPED) > 0);
    Assert.assertTrue(stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_FAILED) > 0);
    Assert.assertTrue(stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SUCCESSFUL) > 0);
    Assert.assertTrue(stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES) > 0);
    Assert.assertEquals(
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS).longValue(),
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(0L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED)
        .longValue());
    Assert.assertTrue(stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED) > 0);
    Assert.assertTrue(stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SUCCESSFUL) > 0);
    Assert.assertTrue(stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS) > 0);

    stageDriver.reset();

    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        stageDriver.runner(new String[] { BaseTestCase.PATH_HDFS_DIR_RAW_LANDING,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING }));
    Assert.assertEquals(
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES).longValue(),
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SKIPPED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_FAILED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SUCCESSFUL));
    Assert.assertEquals(filesCount, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SKIPPED)
        .longValue());
    Assert.assertEquals(0L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_FAILED)
        .longValue());
    Assert.assertEquals(0L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SUCCESSFUL)
        .longValue());
    Assert.assertEquals(filesCount, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES)
        .longValue());
    Assert.assertEquals(
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS).longValue(),
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(partitionsCount,
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED).longValue());
    Assert.assertEquals(0L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED)
        .longValue());
    Assert.assertEquals(0L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SUCCESSFUL)
        .longValue());
    Assert.assertEquals(partitionsCount,
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS).longValue());

    stageDriver.reset();

    getFileSystem().delete(
        HDFSClientUtil.listFiles(getFileSystem(), new Path(BaseTestCase.PATH_HDFS_DIR_RAW_STAGING), true).get(0), true);
    Assert.assertEquals(
        Driver.RETURN_SUCCESS,
        stageDriver.runner(new String[] { BaseTestCase.PATH_HDFS_DIR_RAW_LANDING,
            BaseTestCase.PATH_HDFS_DIR_RAW_STAGING }));
    Assert.assertEquals(
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES).longValue(),
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SKIPPED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_FAILED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SUCCESSFUL));
    Assert.assertEquals(filesCount - 1,
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SKIPPED).longValue());
    Assert.assertEquals(0L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_FAILED)
        .longValue());
    Assert.assertEquals(1L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES_SUCCESSFUL)
        .longValue());
    Assert.assertEquals(filesCount, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.FILES)
        .longValue());
    Assert.assertEquals(
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS).longValue(),
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED)
            + stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SUCCESSFUL));
    Assert.assertEquals(partitionsCount - 1,
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SKIPPED).longValue());
    Assert.assertEquals(0L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_FAILED)
        .longValue());
    Assert.assertEquals(1L, stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS_SUCCESSFUL)
        .longValue());
    Assert.assertEquals(partitionsCount,
        stageDriver.getCounter(StageDriver.class.getCanonicalName(), Counter.PARTITIONS).longValue());

  }

}
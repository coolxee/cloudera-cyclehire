#!/bin/bash

export ROOT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/..

[ -f $ROOT_DIR/../../bin/cyclehire.env ] && source $ROOT_DIR/../../bin/cyclehire.env

set -x

CMD_LINE_ARGUMENTS="$1"
ROOT_DIR_HDFS_RAW_LANDED=${2:-"$ROOT_DIR_HDFS_RAW_LANDED"}
ROOT_DIR_HDFS_RAW_STAGED=${3:-"$ROOT_DIR_HDFS_RAW_STAGED"}
ROOT_DIR_HDFS_RAW_PARTITIONED=${4:-"$ROOT_DIR_HDFS_RAW_PARTITIONED"}
ROOT_DIR_HDFS_PROCESSED=${5:-"$ROOT_DIR_HDFS_PROCESSED"}
LIBJARS="$(echo -n $(ls -m $ROOT_DIR/lib/jar/dep/*.jar)|sed 's/, /,/g')"
export HADOOP_CLASSPATH="$(echo -n $(ls -m $ROOT_DIR/lib/jar/dep/*.jar)|sed 's/, /:/g')"

hadoop \
  jar "$ROOT_DIR"/lib/jar/cloudera-cyclehire-main-process-*.jar \
  com.cloudera.cyclehire.main.process.ProcessDriver \
  -libjars "$LIBJARS" \
  $CMD_LINE_ARGUMENTS \
  "$ROOT_DIR_HDFS_RAW_LANDED" \
  "$ROOT_DIR_HDFS_RAW_STAGED" \
  "$ROOT_DIR_HDFS_RAW_PARTITIONED" \
  "$ROOT_DIR_HDFS_PROCESSED"


#!/bin/bash

export ROOT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/..

[ -f $ROOT_DIR/../../bin/cyclehire.env ] && source $ROOT_DIR/../../bin/cyclehire.env

set -x

CMD_LINE_ARGUMENTS="$1"
ROOT_DIR_HDFS_RAW_STAGED=${2:-"$ROOT_DIR_HDFS_RAW_STAGED"}
ROOT_DIR_HDFS_RAW_PARTITIONED=${3:-"$ROOT_DIR_HDFS_RAW_PARTITIONED"}
ROOT_DIR_HDFS_PROCESSED=${4:-"$ROOT_DIR_HDFS_PROCESSED"}

hadoop \
	jar "$ROOT_DIR/lib/jar/cloudera-cyclehire-main-process-*.jar" \
	-libjars "$(echo -n $(ls -m $ROOT_DIR/lib/jar/dep/*.jar)|sed 's/, /,/g')" \
	com.cloudera.cyclehire.main.process.clean.CleanseDriver \
	$CMD_LINE_ARGUMENTS \
	"$ROOT_DIR_HDFS_RAW_STAGED" \
	"$ROOT_DIR_HDFS_RAW_PARTITIONED" \
	"$ROOT_DIR_HDFS_PROCESSED"

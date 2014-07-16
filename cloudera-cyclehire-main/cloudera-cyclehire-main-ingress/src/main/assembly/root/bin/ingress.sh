#!/bin/bash

export ROOT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/..

[ -f $ROOT_DIR/../../bin/cyclehire.env ] && source $ROOT_DIR/../../bin/cyclehire.env

set -x

CMD_LINE_ARGUMENTS="$1"
ROOT_DIR_LOCAL_TAR=${2:-"$ROOT_DIR_LOCAL_TAR"}
ROOT_DIR_HDFS_RAW_LANDED=${3:-"$ROOT_DIR_HDFS_RAW_LANDED"}

hadoop \
	jar "$ROOT_DIR/lib/jar/cloudera-cyclehire-main-ingress-*.jar" \
	-libjars "$(echo -n $(ls -m $ROOT_DIR/lib/jar/dep/*.jar)|sed 's/, /,/g')" \
	com.cloudera.cyclehire.main.process.clean.CopyDriver \
	-Ddir.include=false \
	-Dblock.single=true \
	-Dtimeout.secs=1000 \
	-Dthread.number=3 \
	-Dthread.queue=file \
	-Dio.file.buffer.size=16384 \
	$CMD_LINE_ARGUMENTS \
	"$ROOT_DIR_LOCAL_TAR" \
	"$ROOT_DIR_HDFS_RAW_LANDED"

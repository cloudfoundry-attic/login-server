#!/bin/bash

cd `dirname $0`

set -e

COBERTURA_CLASSPATH=`mvn -pl login-server dependency:build-classpath -P coverage | grep -v "\[" | tail -n 1`

set -x

java -cp $COBERTURA_CLASSPATH net.sourceforge.cobertura.merge.Main login-server/cobertura.ser

java -cp $COBERTURA_CLASSPATH net.sourceforge.cobertura.reporting.Main --destination login-server/target/site/cobertura --format xml login-server/target/classes

#!/bin/bash

cd `dirname $0`

set -e

mvn dependency:build-classpath -P coverage --quiet

COBERTURA_CLASSPATH=`mvn dependency:build-classpath -P coverage | grep -v "\[" | tail -n 1`

set -x

java -cp $COBERTURA_CLASSPATH net.sourceforge.cobertura.reporting.Main --destination target/site/cobertura --format xml target/classes

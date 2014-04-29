#!/bin/bash

cd `dirname $0`

set -e

COBERTURA_CLASSPATH=`mvn -pl login-server dependency:build-classpath -P coverage | grep -v "\[" | tail -n 1`

set -x

java -cp $COBERTURA_CLASSPATH net.sourceforge.cobertura.instrument.Main login-server/target/classes --ignore CoverageController

#!/bin/bash

cd `dirname $0`

set -e

COBERTURA_CLASSPATH=`mvn dependency:build-classpath -P coverage | grep -v "\[" | tail -n 1`

set -x

java -cp $COBERTURA_CLASSPATH net.sourceforge.cobertura.instrument.Main target/classes

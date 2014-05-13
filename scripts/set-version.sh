#!/bin/bash -ex

cd `dirname $0`/..

mvn -U versions:update-parent -DgenerateBackupPoms=false -DparentVersion=$2
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$1

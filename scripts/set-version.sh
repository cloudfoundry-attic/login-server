#!/bin/bash -ex

cd `dirname $0`/..

mvn versions:update-parent -DgenerateBackupPoms=false -DparentVersion=$1
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$2

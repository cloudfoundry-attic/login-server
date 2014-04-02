#!/bin/bash

set -e

cd `dirname $0`

UAA_BRANCH=master
if [[ $TRAVIS_BRANCH != "master" ]] && [[ $TRAVIS_BRANCH != hotfix/* ]]; then
  UAA_BRANCH=develop
fi

set -x

git clone https://github.com/cloudfoundry/uaa.git --branch $UAA_BRANCH --single-branch uaa
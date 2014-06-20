#!/bin/bash -e

cd `dirname $0`/../..

git config --global user.email "cf-identity-eng@pivotallabs.com"
git config --global user.name "CF Identity Travis bot"

set -x


cd uaa
git checkout `git tag -l 'travis-success-*' | sort -n | tail -n 1`
cd ..
git add uaa
echo `git commit -m "Auto-updating UAA to latest develop version"`

set +x

git push https://cf-identity-eng:$GH_TOKEN@github.com/cloudfoundry/login-server.git develop > /dev/null 2>&1

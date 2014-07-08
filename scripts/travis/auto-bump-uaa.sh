#!/bin/bash -e

cd `dirname $0`/../..

git config --global user.email "cf-identity-eng@pivotallabs.com"
git config --global user.name "CF Login Server Travis bot"

set -x

git checkout develop
cd uaa
git checkout `git tag -l 'travis-success-*' | sort -n | tail -n 1`
cd ..
git add uaa
echo `git commit -m "Auto-updating UAA to latest develop version"`

set +x

git push https://$GH_TOKEN:x-oauth-basic@github.com/cloudfoundry/login-server.git develop 2>&1 | grep -v $GH_TOKEN

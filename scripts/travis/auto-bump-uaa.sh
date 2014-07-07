#!/bin/bash -e

cd `dirname $0`/../..

git config --global user.email "cf-identity-eng@pivotallabs.com"
git config --global user.name "CF Login Server Travis bot"
git config --global credential.username "$GH_TOKEN"

set -x

cd uaa
git checkout `git tag -l 'travis-success-*' | sort -n | tail -n 1`
cd ..
git add uaa
echo `git commit -m "Auto-updating UAA to latest develop version"`

set +x

expect -c '
spawn git push https://github.com/cloudfoundry/login-server.git develop
expect {
":" { send "\r" }
}
expect' 2>&1 | grep -v $GH_TOKEN

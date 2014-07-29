#!/bin/bash -e

cd `dirname $0`/../..

git config --global user.email "cf-identity-eng@pivotallabs.com"
git config --global user.name "CF Login Server Travis bot"

set -x

git checkout develop
cd uaa
LAST_TRAVIS_SUCCESS=`git tag -l 'travis-success-*' | sort -n | tail -n 1`
COMMITS_BEHIND=`git log --oneline HEAD..${LAST_TRAVIS_SUCCESS} | grep -e '^' -c`

if [ $COMMITS_BEHIND -gt 0 ]
then
    echo "UAA is ${COMMITS_BEHIND} commits behind the last successful UAA Travis commit."
    echo "Auto-bumping submodule"
    git checkout ${LAST_TRAVIS_SUCCESS}
    cd ..
    git add uaa
    git commit -m "Auto-updating UAA to latest develop version"
    set +x
    git push https://$GH_TOKEN:x-oauth-basic@github.com/cloudfoundry/login-server.git develop 2>&1 | grep -v $GH_TOKEN
else
    echo "UAA is already at or ahead of the last successful travis build."
    echo "Leaving submodule as is."
fi
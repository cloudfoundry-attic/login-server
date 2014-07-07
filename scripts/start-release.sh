#!/bin/bash -e

cd `dirname $0`/..

if [ "$#" -ne 1 ]; then
    echo "Usage: $(basename $0) login_server_release_version"
    exit 1
fi

echo Creating Login Server release $1

set -x

git checkout develop
git checkout -b releases/$1
./scripts/set-version.sh $1
git commit -am "Bump release version to $1"
git push --set-upstream origin releases/$1

set +x

echo Release branch created from develop branch
echo
echo Check the version number changes and amend if necessary
echo
echo Deploy and finish the release with deploy-and-finish-release.sh
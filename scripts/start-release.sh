#!/bin/bash -e

cd `dirname $0`/..

if [ "$#" -ne 2 ]; then
    echo "Usage: $(basename $0) login_server_release_version uaa_tag"
    exit 1
fi

echo Creating Login Server release $1

set -x

if [[ -n $(git status -s --ignored) ]]; then
    echo "ERROR: Release must be performed from a fresh clone of the repository."
    exit 1
fi

git checkout develop
git checkout -b releases/$1
# Update submodule pointers; Clean out any submodule changes
git submodule foreach --recursive 'git submodule sync; git clean -d --force'
# Update submodule content, checkout if necessary
git submodule update --init --recursive --force
cd uaa
git checkout tags/$2
cd ..
./scripts/set-version.sh $1
git commit -am "Bump release version to $1"
git push --set-upstream origin releases/$1

set +x

echo Release branch created from develop branch
echo
echo Check the version number changes and amend if necessary
echo
echo Deploy and finish the release with deploy-and-finish-release.sh
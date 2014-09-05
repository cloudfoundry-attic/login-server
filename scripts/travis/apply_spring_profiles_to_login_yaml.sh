#!/bin/bash

set -e

cd `dirname $0`/../../

set -x

beg_tag='#BEGIN SAML PROVIDERS'
end_tag='#END SAML PROVIDERS'

(
  sed "/^$beg_tag"'$/,$d' src/main/resources/login.yml
  echo "$beg_tag"
  cat src/test/resources/test.saml.login.yml.txt
  echo "$end_tag"
  sed "1,/^$end_tag/d" src/main/resources/login.yml
) > src/main/resources/test.yml

cat src/main/resources/test.yml

cat src/main/resources/test.yml > src/main/resources/login.yml
rm -f src/main/resources/test.yml

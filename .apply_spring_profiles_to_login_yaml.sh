#!/bin/bash

set -e

cd `dirname $0`

set -x

echo "
spring_profiles: ${@}
" >> login-server/src/main/resources/login.yml

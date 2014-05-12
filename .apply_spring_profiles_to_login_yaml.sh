#!/bin/bash

set -e

cd `dirname $0`

set -x

echo "
spring_profiles: ${@}
" >> src/main/resources/login.yml

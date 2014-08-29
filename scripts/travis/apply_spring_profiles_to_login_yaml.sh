#!/bin/bash

set -e

cd `dirname $0`/../../

set -x

echo "
spring_profiles: ${@}
login.idpEntityAlias: 'testalias'
login.idpMetadataURL: 'http://localhost:8081/test'
" >> src/main/resources/login.yml

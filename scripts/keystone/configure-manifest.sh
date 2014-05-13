#!/bin/bash

set -e

cd `dirname $0`/../..

set -x

echo "
keystone:
  authentication:
    url: http://localhost:5000/v3/auth/tokens
">> src/main/resources/login.yml

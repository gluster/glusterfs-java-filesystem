#!/bin/bash

IDEA_HOST="172.31.31.1"

cd /vagrant

mvn -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=n,address=${IDEA_HOST}:5005,suspend=y" clean install

#!/bin/bash
#

mvn install:install-file -Dfile=./target/microservice-common-1.0-SNAPSHOT.jar -DgroupId=ssas-tuberlin -DartifactId=microservice-common -Dversion=1.0-SNAPSHOT -Dpackaging=jar

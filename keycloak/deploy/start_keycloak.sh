#!/bin/bash
#
#This script will start keycloak with the necessary configuration
KEYCLOAK_CONFIG=../conf/keycloak.json
ATTRIBUTES=

echo "starting keycloak container:"
docker run --rm -p 9000:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -e KEYCLOAK_IMPORT=../conf/keycloak.json -v ../conf/keycloak.json:../conf/keycloak.json jboss/keycloak
if [ $? -eq 0 ]; then
  echo "successfully started keycloak on port 9000"
else
  echo "Error: couldnt start keycloak"
  return 1
fi

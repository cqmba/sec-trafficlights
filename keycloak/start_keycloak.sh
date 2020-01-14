#!/bin/bash
#
#TODO pass Keycloak admin config as files
#This script will start keycloak with the necessary configuration
KEYCLOAK_CONFIG=/conf/realm-export.json
#remeber to build the image beforehand
echo "starting keycloak container:"
docker run -d --rm --name keycloak -p 38080:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -e KEYCLOAK_LOGLEVEL=DEBUG -e KEYCLOAK_IMPORT=$KEYCLOAK_CONFIG keycloak/imported
if [ $? -eq 0 ]; then
  echo "successfully started keycloak on port 38080"
else
  echo "Error: couldnt start keycloak"
  return 1
fi

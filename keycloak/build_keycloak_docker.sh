#!/bin/bash
#
#
echo "Building keycloak container"
docker build . -t keycloak/imported
if [ $? -eq 0 ]; then
  echo "successfully built the docker container for keycloak tagged as keycloak/imported"
else
  echo "Error: correct directory?"
  return 1
fi
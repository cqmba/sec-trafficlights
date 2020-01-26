#!/bin/bash
#

echo "sending POST request"
curl -k -i --header "Content-Type: application/json" --request POST --data '{"color":"GREEN","position":"UNSPECIFIED","type":"VEHICLE", "group":2}' https://localhost:8086/lights/12
read -p "Press enter to continue with PUT"
curl -k -i -X PUT -H "Content-Type: application/json" -d '{"color":"RED"}' https://localhost:8086/lights/
echo "testing get"
curl -k -i https://localhost:8086/lights/12
read -p "Press enter to continue with DELETE"
curl -k -i -X DELETE https://localhost:8086/lights/12
read -p "Press enter to test Emergency Vehicle Request"
curl -k -i -X PUT -H "Content-Type: application/json" -d '{"color":"GREEN", "group":1}' https://localhost:8086/lights/3/colors
read -p "Using api Gateway for resolving Green Light request"
curl -k -i -X PUT -H "Content-Type: application/json" -d '{"color":"GREEN", "group":1}' http://localhost:8787/api/lights/3/colors

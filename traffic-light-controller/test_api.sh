#!/bin/bash
#

MANAGER_TOKEN=eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlRWZCejdLOTVBWUpJQXk1dnBIVU9BNDIxMjdneUtMUlE1RXJJR0duclBFIn0.eyJqdGkiOiJmMjRkNmJjZC0xNjA0LTQ2MTEtYWJkOS0yYzQ1N2FjNDVmYTQiLCJleHAiOjE1ODA3NDE5OTUsIm5iZiI6MCwiaWF0IjoxNTgwNzQxNjk1LCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzL2F1dGgvcmVhbG1zL3ZlcnR4IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6Ijg3ZGU5NDcxLWVlZDYtNDQ3ZS1hOTFmLTk2YmQ1ZjRhYWNkNSIsInR5cCI6IkJlYXJlciIsImF6cCI6InZlcnR4LXRsYzIiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiIwNTZiZmE5NC1kMjdjLTRjNTktODE2OS1iNDU4NzNhY2JhZTIiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImV2IiwibWFuYWdlciIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJtYW5hZ2VyMiJ9.VhHDSvtESgl9W0bJNScQ-bwC6Don4NLC2j_9-zB9imXPpb477yoFgEFmVUPFFCPnTzNZbzNoZoK1XrMLAAPcQDLz6hZK71vLT1Rk1ImESLJvixose144uuYuzvAsw6cTxKtgiSeoFN0nYdT85zPgalWB_dsHjclgdAWuPFubneappVMal_OlGlGL8Kn5yFsOWWXBltjhmCYgsgLyF8TMg34dpnOKwokOyQ6YhE19nhjJO5fB7gVqzeUmtlw-qR39G1JQbRelFmgu2w_ZSRUtSF3zxLx059KuC6wGywhrom35QS5QyHPOKCVcvO75pjPqWmL0M6dKTg15IdNnc628iA

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

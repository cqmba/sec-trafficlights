#!/bin/bash
#
SERVER_STOREPASS=UedJ6AtmjcwF7qNQ
CLIENT_STOREPASS=4mB8nqJd5YEHFkw6
#generate keystore and keypair: args -> storepass: password für keystore
#generates PKCS12 keystore
echo "Generating Server Keystore/Keypair"
keytool -genkeypair -alias server -keyalg RSA -keystore server_keystore.jks -keysize 2048 -dname "CN=mba,OU=ssas,O=tub,L=ber,ST=ber,C=de" -storepass $SERVER_STOREPASS -validity 360
#generate self signed cert
#export server certificate into server.cer
echo "Exporting selfsigned Server Certificate"
keytool -export -alias server -storepass $SERVER_STOREPASS -rfc -file server.cer -keystore server_keystore.jks

echo "Generating Client Keystore/Keypair"
keytool -genkeypair -alias client -keyalg RSA -keystore client_keystore.jks -keysize 2048 -dname "CN=mba,OU=ssas,O=tub,L=ber,ST=ber,C=de" -storepass $CLIENT_STOREPASS -validity 360
#generate self signed cert
#export server certificate into server.cer
echo "Exporting selfsigned Client Certificate"
keytool -export -alias client -storepass $CLIENT_STOREPASS -rfc -file client.cer -keystore client_keystore.jks

#import server certificate into client truststore
echo "Importing server certificate into client truststore"
keytool -importcert -v -alias server -file server.cer -keystore client_truststore.jks -storepass $CLIENT_STOREPASS -noprompt

#import server certificate into client truststore
echo "Importing client certificate into server truststore"
keytool -importcert -v -alias client -file client.cer -keystore server_truststore.jks -storepass $SERVER_STOREPASS -noprompt


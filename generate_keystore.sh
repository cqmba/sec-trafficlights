#!/bin/bash
#
#set these passwords before running
FRONTEND_STOREPASS=
EV_STOREPASS=
TLC_STOREPASS=
GATEWAY_STOREPASS=
KEYCLOAK_STOREPASS=
ALIAS_GATEWAY=gateway
ALIAS_TLC=tlc
ALIAS_EV=ev
ALIAS_FRONTEND=frontend
ALIAS_KEYCLOAK=keycloak
NAME_GATEWAY=gateway
NAME_TLC=tlc
NAME_EV=ev
NAME_FRONTEND=frontend
NAME_KEYCLOAK=keycloak
DOMAIN_TLC="CN=localhost,C=DE"
DOMAIN_GATEWAY="CN=localhost,C=DE"
DOMAIN_EV="CN=localhost,C=DE"
DOMAIN_FRONTEND="CN=localhost,C=DE"
DOMAIN_KEYCLOAK="CN=localhost,C=DE"

#generates PKCS12 keystore
echo "Generating TLC Keystore/Keypair"
keytool -genkeypair -alias $ALIAS_TLC -keyalg RSA -keystore ${NAME_TLC}_keystore.jks -keysize 2048 -dname $DOMAIN_TLC -storepass $TLC_STOREPASS -validity 360

echo "Exporting selfsigned TLC Certificate"
keytool -export -alias $ALIAS_TLC -storepass $TLC_STOREPASS -rfc -file ${NAME_TLC}.pem -keystore ${NAME_TLC}_keystore.jks

echo "Generating Gateway Keystore/Keypair"
keytool -genkeypair -alias $ALIAS_GATEWAY -keyalg RSA -keystore ${NAME_GATEWAY}_keystore.jks -keysize 2048 -dname $DOMAIN_GATEWAY -storepass $GATEWAY_STOREPASS -validity 360

echo "Exporting selfsigned Gateway Certificate"
keytool -export -alias $ALIAS_GATEWAY -storepass $GATEWAY_STOREPASS -rfc -file ${NAME_GATEWAY}.pem -keystore ${NAME_GATEWAY}_keystore.jks

echo "Generating EV Keystore/Keypair"
keytool -genkeypair -alias $ALIAS_EV -keyalg RSA -keystore ${NAME_EV}_keystore.jks -keysize 2048 -dname $DOMAIN_EV -storepass $EV_STOREPASS -validity 360

echo "Exporting selfsigned EV Certificate"
keytool -export -alias $ALIAS_EV -storepass $EV_STOREPASS -rfc -file ${NAME_EV}.pem -keystore ${NAME_EV}_keystore.jks

echo "Generating Frontend Keystore/Keypair"
keytool -genkeypair -alias $ALIAS_FRONTEND -keyalg RSA -keystore ${NAME_FRONTEND}_keystore.jks -keysize 2048 -dname $DOMAIN_FRONTEND -storepass $FRONTEND_STOREPASS -validity 360

echo "Exporting selfsigned Frontend Certificate"
keytool -export -alias $ALIAS_FRONTEND -storepass $FRONTEND_STOREPASS -rfc -file ${NAME_FRONTEND}.pem -keystore ${NAME_FRONTEND}_keystore.jks

echo "Generating Keycloak Keystore/Keypair"
keytool -genkeypair -alias $ALIAS_KEYCLOAK -keyalg RSA -keystore ${NAME_KEYCLOAK}.jks -keysize 2048 -dname $DOMAIN_KEYCLOAK -storepass $KEYCLOAK_STOREPASS -validity 360

echo "Exporting selfsigned Keycloak Certificate"
keytool -export -alias $ALIAS_KEYCLOAK -storepass $KEYCLOAK_STOREPASS -rfc -file ${NAME_KEYCLOAK}.pem -keystore ${NAME_KEYCLOAK}.jks


#echo "Importing TLC,EV,FRONTEND certificate into gateway truststore"
keytool -importcert -v -alias $ALIAS_TLC -file ${NAME_TLC}.pem -keystore ${NAME_GATEWAY}_truststore.jks -storepass $GATEWAY_STOREPASS -noprompt
keytool -importcert -v -alias $ALIAS_EV -file ${NAME_EV}.pem -keystore ${NAME_GATEWAY}_truststore.jks -storepass $GATEWAY_STOREPASS -noprompt
keytool -importcert -v -alias $ALIAS_FRONTEND -file ${NAME_FRONTEND}.pem -keystore ${NAME_GATEWAY}_truststore.jks -storepass $GATEWAY_STOREPASS -noprompt

#echo "Importing Gateway certificate into TLC, EV, FRONTEND truststore"
keytool -importcert -v -alias $ALIAS_GATEWAY -file ${NAME_GATEWAY}.pem -keystore ${NAME_TLC}_truststore.jks -storepass $TLC_STOREPASS -noprompt
keytool -importcert -v -alias $ALIAS_GATEWAY -file ${NAME_GATEWAY}.pem -keystore ${NAME_EV}_truststore.jks -storepass $EV_STOREPASS -noprompt
keytool -importcert -v -alias $ALIAS_GATEWAY -file ${NAME_GATEWAY}.pem -keystore ${NAME_FRONTEND}_truststore.jks -storepass $TLC_STOREPASS -noprompt

#remove certificate files
rm *.pem

#list entries in trust/keystore like this
#keytool -list -v -keystore gateway_truststore.jks -storepass $GATEWAY_STOREPASS


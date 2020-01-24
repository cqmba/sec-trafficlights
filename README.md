Descriptions
===========

General advice:
* scripts should usually only be started within their respective directory
* before executing scripts, always double check what they are doing because I wrote some of then on the fly and the might only work under certain circumstances, be free to add to them
* the vertx config retriever should automaticall load the /conf/config.json files, however this didnt work for me, I had to pass them as an argument so check if your IDE/runner settings are correct and remember that we need to copy the conf.json when building the docker container
* service discovery is still WIP (see api-gateway), same goes for SSL and Keycloak

api-gateway module
* Config folder : conf/config.json -> remember to add to verticle launcher and to docker container if using this configfile
* keystore and truststore directory is also set via config, however ssl is not working yet(see section SSL below)
* if you want to use the module without ssl, start the APIGatewayVerticle httpserver without the options
* currently port 8787 is used for the api-gateway, you can change it but remember to apply changes to the service discovery since that is hardcoded for now, same applies for adding new services
* static content can be added in src/main/resources/assets for now

ev-service
* really bare-bones functionality, was mainly for testing, be free to change/start implementation

hello-world
* will be removed soon, just for testing/getting to know vertx

keycloak
* current default config: port 38080
* exported realm is in conf/realm-export.json
* keycloak installation json is in conf/keycloak.json and also in the module api-gateway/conf/config.json
* build the docker container (run build_keycloak_docker.sh) once before starting it (start_keycloak.sh), since it depends on the tag
TODOs:
* check if the config here get actually loaded (no entrypoint for docker specified)

microservice-common
* just a jar with certain common classes
* was taken from the vertx blueprint and I tried to port it to 3.8.4
* we can probably delete a lot of the methods etc. since we dont need them and possibly even work without this jar
* I had problems getting the jar in the other modules with maven, if the same happens to you install the jar with the maven_install.sh (remember to rerun when it changes)

traffic-light-controller
* backend for the lights API
* current default config: port 8086
* currently SSL is enabled, however it doesnt work when not standalone (i.e. with gateway)

SSL (not fully working)
* run generate_keystore.sh to generate both keystore and truststore for 2 services, (called client & server)
* add the server_keystore.jks and server_truststore.jks to /src/main/resources and check that the storepass is correctly set in config.json
* for client, rename to server_keystore.jks server_truststore.jks and do the same with another service
* sadly it doenst work yet, only standalone (not with 2 services) -> **Help with this problem is appreciated!** I might have misunderstood how the config should be.
* My idea was to create keystore & truststore for all services, import the certificates of all other services into the gateway truststore and import only the gateway certificate into the standalone services

Deployment
* I started working on a deploy Skript (deploy.sh) that should in the end automate the whole build process from maven to docker to kubernetes, but its kinda deprecated (last updated in Mid of December) and I will update it eventually, feel free to add your own ideas to it 

TODO liste
* Keycloak/Vertx config
* SSL certificates/Vertx SSL configuration
* Applications need to be able to verify authorization from oAuth and log access
* We should get the MySQL server running so that we can configure log4j to work over jdbc (Encrypted communication?)
* Verify and test /lights backend


RELEASE notes

SSL settings:
Supported version: TLSv1.3 only
Enabled Cipher suites: TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384
Advanced ciphers we wanted to enable as well but they are not supported:TLS_CHACHA20_POLY1305_SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256
Configuration (Advanced = strongest) taken from https://owasp.org/www-project-cheat-sheets/cheatsheets/TLS_Cipher_String_Cheat_Sheet.html
RELEASE notes
=============

Installation Guide
============
* Download
```
git clone https://gitlab.tubit.tu-berlin.de/aot-security-lectures/wise2019-ivssase-g8.git
cd wise2019-ivssase-g8
```
* For Release we cant provide you with our debug keystores and secrets, therefore you have to create your own certificates first
* Create Keystores, Truststores with respective Storepasswords for the modules traffic-light-controller, ev-service and api-gateway
* The API-Gateway Certificate needs to be imported to the EV-Service Truststore and the TLC-Certificate needs to be imported by the API-Gateway Truststore
* You may use the `generate_keystore.sh` skript, just remember to change the storepass-variables
* After completing the previous steps, you should do the following:
in `api-gateway/` subdirectory, you should have a `gateway_keystore.jks` and `gateway_truststore.jks`

in `api-gateway/conf/config.json` update `keystore.password` value to your keystore password and `truststore.password`to your truststore password

if you have other paths/filenames, update those values aswell

in `traffic-light-controller/` subdirectory, you should have a `tlc_keystore.jks`

in `traffic-light-controller/conf/config.json` update `keystore.password` to your keystore password

if you have other paths/filenames, update those values aswell

in `ev-service/` subdirectory, you should have a `ev_keystore.jks` and `ev_truststore.jks`

in `ev-service/conf/config.json` update `keystore.password` value to your keystore password and `truststore.password`to your truststore password

if you have other paths/filenames, update those values aswell

in `keycloak/` subdirectory, you should have a `keycloak.jks`

in `keycloak/Dockerfile`, you have to update your keycloak path and keycloak keystore password in line 10

in `operationview/docker-conf/` supply a `ca.cert` and a `ca.key`file

* Deploy Keycloak, Database, Frontend (nginx)

Currently, it is only possible to start a valid Keycloak configuration for the application if you fill the MySQL Database with an initial SQL-Dump file.
Please follow this Setup And Deployment Guide in the wiki Part of this Gitlab Repo
https://gitlab.tubit.tu-berlin.de/aot-security-lectures/wise2019-ivssase-g8/wikis/setup-&-deployment-guide

`docker-compose up` should now deploy Keycloak, the MySQL Database and the Frontend running on nginx correctly (Building the frontend takes a while)

* Start Keycloak and log into keycloak to retrieve the client secrets

Go to `https://localhost:8443/` and log into the master console by using your preconfigured keycloak admin credentials (see `docker-compose.yml`) to log in 

go to Clients -> "vertx-tlc2" -> Credentials tab and copy the client secret to `api-gateway/conf/config.json` value credentials : secret : ""

go to Clients -> "vertx-test2" -> Credentials tab and copy the client secret to `ev-service/conf/config.json` value credentials : secret : ""

* Build with maven (you need JDK Version >= 11)
```
mvn clean package
```
* Optionally confirm no bugs are spotted
```
mvn spotbugs:spotbugs
mvn spotbugs:check
```
* Run the Vertx Apps locally(JRE >= 11):
```
cd traffic-light-controller
java -jar ./target/traffic-light-controller-fat.jar -conf conf/config.json
```
2nd Terminal or run everything in the background
```
cd api-gateway
java -jar ./target/api-gateway-fat.jar -conf conf/config.json
```
3rd Terminal or run everything in the background
```
cd ev-service
java -jar ./target/ev-service-fat.jar -conf conf/config.json
```

* Alternatively, deploy vertx apps with docker 
Somehow this is currently bugged: SSL Error PR_END_OF_FILE_ERROR

TLS is working fine when deployed as fat jar though, so far we have not found a solution. For building a vertx docker from a fat jar, we kept to the official vertx Examples on vertx.io

Deploying with docker (manually)
```
cd api-gateway
docker build . -t vertx/api-gateway
docker run -d -p 8787:8787 vertx/api-gateway
cd ..
cd ev-service
docker build . -t vertx/ev-service
docker run -d -p 8087:8087 vertx/ev-service
cd ..
cd traffic-light-controller
docker build . -t vertx/tlc
docker run -d -p 8086:8086 vertx/tlc
```

* If everything is running and the jars are started with the correct config file, you should be able to access the frontend at `https://localhost` or the api-gateway directly at `https://localhost:8787/api/lights`. Make sure to login with a valid user and an authorized Role Mapping, i.e. `manager`or `observer`. The `ev-service` can be triggered manually by calling `https://localhost:8087/sensors/id:` with an id value between 0-3 

* Deploy with Kubernetes/Minikube
This is still work in progress, since its not required for the task. 
We already have a working `docker-compose.yml`, which can be converted to kubernetes with
`kompose up` or `kompose convert`
However, in our current setup we have provided static localhost hostnames to our endpoints, therefore our 
service discovery would have to be ported to kubernetes first, likewise the certificates. We want to revisit this for the next presentation.

Additional Information about the Project
=============================

Wiki Page for changes during Implementation phase regarding initial planning/requirements
https://gitlab.tubit.tu-berlin.de/aot-security-lectures/wise2019-ivssase-g8/wikis/changes-during-implementation

Security Features
=================

**very strong TLS settings, therefore ensuring Confidentiality, Integrity and Authenticity**

Supported version: TLSv1.3 only

Enabled Cipher suites: TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384

Advanced ciphers we wanted to enable as well but they are not supported by vertx:TLS_CHACHA20_POLY1305_SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256

Configuration (Advanced = strongest) taken from https://owasp.org/www-project-cheat-sheets/cheatsheets/TLS_Cipher_String_Cheat_Sheet.html

Vertx does not natively support client Verification for SSL connections, atleast not on the HttpServer Class, only in lower level Classes therefore implementation was not feasible.

**Circuit Breaker for API Gateway**

**extensive, secure Logging (e.g. time, username, roles, source IP & port, accessed resource ...)**

**Service Discovery**

**Role and User based Access Control**

**Token based Authentication and Authorization when accessing Resources**

**Fail-safe Mechanisms and focus on Safety for the Traffic Light Controlling Logic/Scheduling**

**Health Check Service & Lookup**

**User Input Sanitization**

Additional Features
==================

**shipped with a MySQL Database and a Frontend**

**basically production ready Backend Implementation**

**freely configurable Roles & Users through the shipped Keycloak Application and simple login flow**

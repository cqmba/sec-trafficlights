Descriptions
===========
TODO list
* Keycloak/Vertx config
* Create working Docker containers for all services
* Remove the startup Exceptions if possible
* Frontend integration
* Test state machine with frontend for intended behaviour
* Applications need to be able to verify authorization from oAuth
* If possible: Create dataprotection service and IDS logic
* If possible: Refactor for Testing, start writing tests
* If possible: Encrypt DB communication
* If possible: Enable Kubernetes Discovery and deploy with Kubernetes
* **BEFORE RELEASE: Remove all TODOs; handle all Exceptions & Warnings; remove random commented out stuff or development notes; check no passwords get leaked;**
* **BEFORE RELEASE: Check good code quality, readable, short methods, decoupled, seperation of concerns etc.**

RELEASE notes
=============

Installation
============
* Download
```
git clone https://gitlab.tubit.tu-berlin.de/aot-security-lectures/wise2019-ivssase-g8.git
cd wise2019-ivssase-g8
```
* Optionally change the debug passwords
Production passwords should never be published in version control, therefore the application is only bundled with debug passwords to make testing easier
If you want to generate new passwords for the Application, you need to change the variables in the `generate_keystore.sh` Skript and run it afterwards. 
Remember to use different Passwords for Keystores and Truststores
You have to pass those passwords to vertx as a json, this can be done by editing the respective `conf/config.json` file within the submodules and running the jar with the added argument `-conf conf/config.json` 
You might need to update the passwords for the docker containers aswell

* Build with maven (you need JDK Version >= 11)
```
mvn clean package
```
* Deploy locally OR build and run as Docker Container
Deploying locally (repeat in different terminals for ev-service, traffic-light-controller)

```
cd api-gateway
java -jar ./target/api-gateway-fat.jar -conf conf/config.json
```

Deploying with docker (manually)
```
cd api-gateway
docker build . -t vertx/api-gateway
docker run -d -i -t -p 38080:38080 vertx/api-gateway
cd ..
cd ev-service
docker build . -t vertx/ev-service
docker run -d -i -t -p 8087:8087 vertx/ev-service
cd ..
cd traffic-light-controller
docker build . -t vertx/tlc
docker run -d -i -t -p 8086:8086 vertx/tlc
```

OR `docker-compose up`
* Deploy Keycloak, Database, Frontend (nginx)

TODO alles einfach per docker-compose

* Optionally: Deploy with Kubernetes

TODO hier brauchen wir dann die docker commandos von oben vermutlich
`kompose up` bzw `kompose convert`

Additional Information about the Project
=============================

Wiki Page for changes during Implementation phase regarding initial planning/requirements
https://gitlab.tubit.tu-berlin.de/aot-security-lectures/wise2019-ivssase-g8/wikis/changes-during-implementation

The project was checked with the spotbugs-maven-plugin to contain 0 bugs. 
run `mvn spotbugs:check` if you want to verify this

*SSL settings*

Supported version: TLSv1.3 only

Enabled Cipher suites: TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384

Advanced ciphers we wanted to enable as well but they are not supported by vertx:TLS_CHACHA20_POLY1305_SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256

Configuration (Advanced = strongest) taken from https://owasp.org/www-project-cheat-sheets/cheatsheets/TLS_Cipher_String_Cheat_Sheet.html

Vertx does not natively support client Verification for SSL connections, atleast not on the HttpServer Class, only in lower level Classes therefore implementation was not feasible.

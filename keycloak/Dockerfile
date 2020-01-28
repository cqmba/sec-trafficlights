# Extend base image
FROM jboss/keycloak

ENV JBOSS_HOME /opt/jboss/keycloak

# setup SSL
USER root
ADD keycloak_keystore.jks $JBOSS_HOME/standalone/configuration/
RUN chown jboss:jboss $JBOSS_HOME/standalone/configuration/keycloak_keystore.jks
USER jboss
RUN sed -i -e 's/<security-realms>/&\n            <security-realm name="UndertowRealm">\n                <server-identities>\n                    <ssl>\n                        <keystore path="keycloak_keystore.jks" relative-to="jboss.server.config.dir" keystore-password="Amy92TuN6uGFDyEQ" \/>\n                    <\/ssl>\n                <\/server-identities>\n            <\/security-realm>/' $JBOSS_HOME/standalone/configuration/standalone.xml
RUN sed -i -e 's/<server name="default-server">/&\n                <https-listener name="https" socket-binding="https" security-realm="UndertowRealm"\/>/' $JBOSS_HOME/standalone/configuration/standalone.xml

#ENV KEYCLOAK_IMPORT_FILE ./conf/realm-export.json
#ENV KEYCLOAK_TARGET_HOME conf

#EXPOSE 8080

#COPY $KEYCLOAK_IMPORT_FILE  $KEYCLOAK_TARGET_HOME/
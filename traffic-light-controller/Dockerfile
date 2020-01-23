FROM openjdk:latest

ENV VERTICLE_FILE traffic-light-controller-fat.jar
ENV CERTIFICATE_FILE gateway.pem
ENV KEYSTORE_FILE tlc_keystore.jks
ENV CONF_FILE config.json

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8086


# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME/
COPY $KEYSTORE_FILE $VERTICLE_HOME/
COPY conf/$CONF_FILE $VERTICLE_HOME/conf/

USER root
COPY $CERTIFICATE_FILE $JAVA_HOME/lib/security
RUN \
    cd $JAVA_HOME/lib/security \
    && keytool -cacerts -storepass changeit -noprompt -importcert -alias gateway -file $CERTIFICATE_FILE

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE -conf conf/config.json"]
# -Djavax.net.debug=ssl:handshake:verbose
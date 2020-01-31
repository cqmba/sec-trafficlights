FROM openjdk:latest

ENV VERTICLE_FILE api-gateway-fat.jar
ENV TRUSTSTORE_FILE gateway_truststore.jks
ENV KEYSTORE_FILE gateway_keystore.jks
ENV CONF_FILE config.json

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8787


COPY target/$VERTICLE_FILE $VERTICLE_HOME/
COPY $KEYSTORE_FILE $VERTICLE_HOME/
COPY $TRUSTSTORE_FILE $VERTICLE_HOME/
COPY conf/$CONF_FILE $VERTICLE_HOME/conf/

#USER root
#RUN cd $VERTICLE_HOME && chmod +x $VERTICLE_FILE

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE -conf conf/config.json"]
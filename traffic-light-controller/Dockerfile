FROM openjdk:latest

ENV VERTICLE_FILE traffic-light-controller-fat.jar
ENV KEYSTORE_FILE tlc_keystore.jks
ENV TRUSTSTORE_FILE tlc_truststore.jks
ENV CONF_FILE config.json

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8086


COPY target/$VERTICLE_FILE $VERTICLE_HOME/
COPY $KEYSTORE_FILE $VERTICLE_HOME/
COPY $TRUSTSTORE_FILE $VERTICLE_HOME/

#USER root
#RUN cd $VERTICLE_HOME && chmod +x $VERTICLE_FILE

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE"]
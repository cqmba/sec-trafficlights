FROM vertx/vertx3

#ENV VERTICLE_FILE traffic-light-controller-fat.jar
ENV KEYSTORE_FILE tlc_keystore.jks
#ENV TRUSTSTORE_FILE tlc_truststore.jks
ENV CONF_FILE config.json
ENV VERTICLE_NAME de.tub.trafficlight.controller.TLControllerVerticle
ENV VERTICLE_FILE target/traffic-light-controller-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8086


#COPY target/$VERTICLE_FILE $VERTICLE_HOME/
COPY $KEYSTORE_FILE $VERTICLE_HOME/
#COPY $TRUSTSTORE_FILE $VERTICLE_HOME/
COPY conf/$CONF_FILE $VERTICLE_HOME/conf/
COPY $VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -conf conf/config.json"]
# -cp $VERTICLE_HOME/*
#CMD ["exec java -jar $VERTICLE_FILE -conf conf/config.json"]
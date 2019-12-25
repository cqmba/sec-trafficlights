# Extend vert.x base image
FROM vertx/vertx3

ENV VERTICLE_NAME helloworld.HelloVerticle
ENV VERTICLE_FILE target/hello-world-1.0-SNAPSHOT.jar

ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

# Copy the jar file into a container folder
COPY $VERTICLE_FILE $VERTICLE_HOME/

#Launch
WORKDIR $VERTICLE_HOME

ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]
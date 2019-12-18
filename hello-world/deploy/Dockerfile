# Extend vert.x base image
FROM vertx/vertx3

ENV VERTICLE_JAR hello-world-fat.jar
ENV VERTICLE_HOME verticles

EXPOSE 8080

# Copy the jar file into a container folder
COPY $VERTICLE_JAR $VERTICLE_HOME/

WORKDIR .

# Launch the verticle using 'java -jar verticles/hello-world-1.0-SNAPSHOT' command
ENTRYPOINT ["sh", "-c","java -jar $VERTICLE_HOME/$VERTICLE_JAR"]
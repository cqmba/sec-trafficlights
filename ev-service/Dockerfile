# Extend vert.x base image
FROM vertx/vertx3

ENV VERTICLE_JAR ./target/ev-service-fat.jar
ENV VERTICLE_HOME verticles

EXPOSE 8087

# Copy the jar file into a container folder
COPY $VERTICLE_JAR $VERTICLE_HOME/

WORKDIR .

# Launch the verticle using 'java -jar verticles/... command
ENTRYPOINT ["sh", "-c","java -jar $VERTICLE_HOME/$VERTICLE_JAR"]
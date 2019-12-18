#!/bin/sh
#
# Default values of arguments
MAVEN_BUILD=0
DISPLAY_HELP=0
DOCKER_IMAGE=0
DEPLOY_SERVICES=0
#CACHE_DIRECTORY="/etc/cache"
#ROOT_DIRECTORY="/etc/projects"
OTHER_ARGUMENTS=()

#TODO define services as variables

# Loop through arguments and process them
for arg in "$@"
do
    case $arg in
        -b|--build)
        MAVEN_BUILD=1
        shift # Remove from processing
        ;;
        -i|--image)
        DOCKER_IMAGE=1
        shift
        ;;
        -d|--deploy)
        DEPLOY_SERVICES=1
        shift
        ;;
        -h|--help)
        DISPLAY_HELP=1
        shift
        ;;
        *)
        OTHER_ARGUMENTS+=("$1")
        shift # Remove generic argument from processing
        ;;
    esac
done

echo "# Maven build: $MAVEN_BUILD"
echo "# Display help: $DISPLAY_HELP"
echo "# Build docker images: $DOCKER_IMAGE"
echo "# Deploying services: $DEPLOY_SERVICES"
echo "# Other arguments: ${OTHER_ARGUMENTS[*]}"

if [ "$DISPLAY_HELP" -eq "1" ]; then
    echo "use this script like this:";
    echo ". ./deploy.sh -b -i -d";
    echo "-b is optional and does a mvn clean package for every submodule";
    echo "-i is optional and does a docker build for every submodule";
    echo "-d is optional and does a docker run for every submodule";
    echo "WARNING: image creation depends on successfull Maven build and deployment on successfull image creation";
    echo "WARNING: This script should only be used when you are sure that each step will succeed and just want to automate full deployment, if you use it for development you have to make sure to change the script for your local changes!!"
    return 0;
fi

if [ "$MAVEN_BUILD" -eq "1" ]; then
    echo "Running Maven Build:";
    cd hello-world;
    mvn clean package;
    if [ $? -eq 0 ]; then
        echo "hello world package successfully built";
        cd ..;
    else
        echo "Maven Build for package: hello-word contains errors";
        cd ..;
        return 1;
    fi
fi

if [ "$DOCKER_IMAGE" -eq "1" ]; then
    echo "Running Docker Image builds:";
    cp hello-world/target/hello-world-1.0-SNAPSHOT-fat.jar hello-world/deploy/hello-world-fat.jar
    if [ $? -eq 0 ]; then
        echo "successfully copied jar to deploy directory";
    else
        echo "couldnt copy jar to deploy directory, are you sure the jar has been built and is named correctly?";
        return 1;
    fi
    docker build ./hello-world/deploy -t hello/vertx;
    if [ $? -eq 0 ]; then
        echo "hello world docker image successfully built";
    else
        echo "Docker image failed for package: hello-word contains errors";
        return 1;
    fi
fi

if [ "$DEPLOY_SERVICES" -eq "1" ]; then
    echo "Deploying container with service hello-world at external port 8080 in background:";
    docker run -t -i -d -p 8080:8080 hello/vertx;
    if [ $? -eq 0 ]; then
        echo "Success: Service is running on port 8080.";
        echo "To stop the running docker container:";
        echo "Stop single: $ docker stop <dockerhash>";
        echo "Stop all: $ docker stop \`docker ps -q\`";
    else
        echo "Errors when trying to deploy docker image hello-world on port 8080";
        return 1;
    fi
fi
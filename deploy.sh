#!/bin/sh
#
# Default values of arguments
MAVEN_BUILD=0
DISPLAY_HELP=0
DOCKER_IMAGE=0
DEPLOY_SERVICES=0
SETUP_KUBERNETES=0
KUBERNETES_RUN=0
OTHER_ARGUMENTS=()

#TODO define services as variables
declare -a services=("hello-world"
)

# Loop through arguments and process them
for arg in "$@"; do
  case $arg in
  -b | --build)
    MAVEN_BUILD=1
    shift # Remove from processing
    ;;
  -i | --image)
    DOCKER_IMAGE=1
    shift
    ;;
  -d | --deploy)
    DEPLOY_SERVICES=1
    shift
    ;;
  -k | --kubernetes)
    SETUP_KUBERNETES=1
    shift
    ;;
  -r | --run)
    KUBERNETES_RUN=1
    shift
    ;;
  -h | --help)
    DISPLAY_HELP=1
    shift
    ;;
  *)
    OTHER_ARGUMENTS+=("$1")
    shift # Remove generic argument from processing
    ;;
  esac
done

echo "Your Settings:"
echo "# Maven build: $MAVEN_BUILD"
echo "# Display help: $DISPLAY_HELP"
echo "# Build docker images: $DOCKER_IMAGE"
echo "# Deploying services: $DEPLOY_SERVICES"
echo "# Setup kubernetes: $SETUP_KUBERNETES"
echo "# Run kubernetes: $KUBERNETES_RUN"
echo "# Other arguments: ${OTHER_ARGUMENTS[*]}"

if [ "$DISPLAY_HELP" -eq "1" ]; then
  echo "use this script like this:"
  echo ". ./deploy.sh -b -i -d -k -r"
  echo "-b is optional and does a mvn clean package for every submodule"
  echo "-i is optional and does a docker build for every submodule"
  echo "-d is optional and does a docker run for every submodule"
  echo "-k is optional and startes minikube vm with some preconfigured settings"
  echo "-r is optional and pulls all the latest images into kubernetes"
  echo "WARNING: image creation depends on successfull Maven build and deployment on successfull image creation"
  echo "WARNING: This script should only be used when you are sure that each step will succeed and just want to automate full deployment, if you use it for development you have to make sure to change the script for your local changes!!"
  return 0
fi

if [ "$MAVEN_BUILD" -eq "1" ]; then
  for i in "${services[@]}"; do
    echo "Starting Maven Build for service $i"
    cd $i
    mvn clean package
    if [ $? -eq 0 ]; then
      echo "$i package successfully built"
      cd ..
    else
      echo "Maven Build for package: $i contains errors"
      cd ..
      return 1
    fi
  done
fi

if [ "$DOCKER_IMAGE" -eq "1" ]; then
  echo "Running Docker Image builds:"
  cp hello-world/target/hello-world-1.0-SNAPSHOT-fat.jar hello-world/deploy/hello-world-fat.jar
  if [ $? -eq 0 ]; then
    echo "successfully copied jar to deploy directory"
  else
    echo "couldnt copy jar to deploy directory, are you sure the jar has been built and is named correctly?"
    return 1
  fi
  docker build ./hello-world/deploy -t hello/vertx
  if [ $? -eq 0 ]; then
    echo "hello world docker image successfully built"
  else
    echo "Docker image failed for package: hello-word contains errors"
    return 1
  fi
fi

if [ "$DEPLOY_SERVICES" -eq "1" ]; then
  echo "Deploying container with service hello-world at external port 8080 in background:"
  docker run -t -i -d -p 8080:8080 hello/vertx
  if [ $? -eq 0 ]; then
    echo "Success: Service is running on port 8080."
    echo "To stop the running docker container:"
    echo "Stop single: $ docker stop <dockerhash>"
    echo "Stop all: $ docker stop \`docker ps -q\`"
  else
    echo "Errors when trying to deploy docker image hello-world on port 8080"
    return 1
  fi
fi

if [ "$SETUP_KUBERNETES" -eq "1" ]; then
  echo "This will setup your Dev & Registry and start minikube"
  echo "Warning: this part of the script is still buggy."
  while true; do
    read -p "Do you wish to continue anyway? [Y/n]" yn
    case $yn in
    [Yy]*)
      break
      ;;
    [Nn]*) return 0 ;;
    *) echo "Please answer yes or no." ;;
    esac
  done
  echo "Starting local registry:"
  docker-machine start registry
  if [ $? -eq 0 ]; then
    echo "Success: Local registry started"
  else
    echo "Error when starting local registry"
    return 1
  fi
  #echo "starting dev vm"
  #docker-machine start dev
  #if [ $? -eq 0 ]; then
  #  echo "Success: Local dev machine started"
  #else
  #  echo "Error when starting local registry"
  #  return 1
  #fi
  REG_IP=$(docker-machine ip registry)
  #point docker cli to dev vm
  #eval $(docker-machine env dev)
  minikube start --vm-driver="virtualbox" --insecure-registry="$REG_IP:80"
  if [ $? -eq 0 ]; then
    echo "Success: Minikube Running with local registry"
  else
    echo "Error when trying to start minikube vm"
    return 1
  fi
  echo "you may start using minikube with: $ minikube ssh"
  #maybe redirect docker-daemon to work with minikube
  #eval $(minikube docker-env)
fi

if [ "$KUBERNETES_RUN" -eq "1" ]; then
  echo "This will run the latest docker images as a single service in minikube/kubernetes"
  echo "Warning: this part of the script is still buggy."
  while true; do
    read -p "Do you wish to continue anyway? [Y/n]" yn
    case $yn in
    [Yy]*)
      break
      ;;
    [Nn]*) return 0 ;;
    *) echo "Please answer yes or no." ;;
    esac
  done
  #TODO
  #minikube ssh
  #docker pull $REG_IP:80/hello/vertx
  #kubectl run hello-vertx --image=$REG_IP:80/hello/vertx
  #kubectl expose deployment hello-vertx --port=8080 --type=NodePort
  #minikube service hello-vertx
fi
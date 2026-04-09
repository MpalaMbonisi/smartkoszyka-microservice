#!/bin/bash
SERVICE_NAME=$1
mkdir -p services/$SERVICE_NAME/src/main/java/com/github/mpalambonisi/$SERVICE_NAME
mkdir -p services/$SERVICE_NAME/src/main/resources
mkdir -p services/$SERVICE_NAME/src/test/java/com/github/mpalambonisi/$SERVICE_NAME
cp templates/service-pom.xml services/$SERVICE_NAME/pom.xml
cp templates/Dockerfile services/$SERVICE_NAME/Dockerfile
cp templates/application.properties services/$SERVICE_NAME/src/main/resources/
sed -i "s/SERVICE_NAME/$SERVICE_NAME/g" services/$SERVICE_NAME/pom.xml
echo "🟢 Service $SERVICE_NAME created"


# To run the script, use: ./new-service.sh <service-name>

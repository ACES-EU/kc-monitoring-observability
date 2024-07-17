# Use the official Maven image to build the app
# https://hub.docker.com/_/maven
# FROM maven:3.8.4-openjdk-18 AS build
FROM maven:3.9.8-eclipse-temurin-11 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml file and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the application code and build the application
COPY src ./src
RUN mvn -e clean package -DskipTests

# Use the official OpenJDK image to run the app
# https://hub.docker.com/_/openjdk
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the jar file from the build image
COPY --from=build /app/target/aggregation-demo-1.0-SNAPSHOT.jar ./myapp.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "myapp.jar"]
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# copy pom.xml file to the root folder which is /app
COPY pom.xml .

# mvn command is used to install all packages from the maven image
# go-offline refers to caching dependencies for quick boot up
RUN mvn dependency:go-offline -B

COPY src ./src

# maven creates a new package in jar format
RUN mvn clean package

# maven is only used to build the project
# jdk is where it actually runs
FROM openjdk:21-jdk AS runner

WORKDIR /app

# Copy snapshot jar file form builder step to ./app.jar
COPY --from=builder ./app/target/auth-service-0.0.1-SNAPSHOT.jar ./app.jar

# port where our app runs
EXPOSE 4005

# all of our patient-service code is in app.jar file
# java cli is used to run this code
ENTRYPOINT ["java", "-jar", "app.jar"]
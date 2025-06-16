# Use a JDK 11 base image
FROM eclipse-temurin:11-jdk-alpine

# Set a working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY build/libs/file-importer-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your app runs on (usually 8080 for Spring Boot)
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]

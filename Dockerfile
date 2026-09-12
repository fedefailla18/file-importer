# ---- Build stage: compile the jar from source, no host-side gradlew needed ----
# Alpine JDK images are amd64-only for several Temurin versions — use the
# Ubuntu-based tag so this also builds natively on arm64 (Apple Silicon).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Dependency layer first so it's cached across builds when only src/ changes.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar -x test -x integrationTest --no-daemon

# ---- Runtime stage ----
# JRE is enough to run a packaged jar (no compiler needed); 21-jre-alpine
# publishes an arm64 build, unlike 21-jdk-alpine.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR file into the container
COPY --from=build /app/build/libs/investracker-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your app runs on (9080 per application.yml)
EXPOSE 9080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]

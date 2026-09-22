# ---- Build stage: compile + run the full test suite on the canonical Maven/JDK-17 image ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests are part of the build: a failing invariant breaks the image build.
RUN mvn -B package

# ---- Runtime stage: JDK-17 baseline, nothing else ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/camera-geometry-service-1.0.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

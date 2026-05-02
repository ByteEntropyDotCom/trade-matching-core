# --- Stage 1: Build ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for security
RUN addgroup -S trading && adduser -S engine -G trading
USER engine

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the API port and WebSocket port
EXPOSE 8080

# Run with Virtual Threads optimized settings
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "app.jar"]
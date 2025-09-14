# Multi-stage build for Numaflow enrichment application
FROM maven:3.9-amazoncorretto-21 AS build

# Set working directory
WORKDIR /app

# Copy POM file first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM amazoncorretto:21-alpine

# Install required packages
RUN apk add --no-cache \
    curl \
    && rm -rf /var/cache/apk/*

# Create app user for security
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directories for models and logs
RUN mkdir -p /app/models /app/logs && \
    chown -R appuser:appgroup /app

# Download OpenNLP models (optional - can be mounted as volumes)
COPY scripts/download-models.sh /app/scripts/
RUN chmod +x /app/scripts/download-models.sh

# Switch to non-root user
USER appuser

# Expose port for health checks
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

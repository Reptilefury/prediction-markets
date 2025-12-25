# Build stage
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/src/main/resources/secure-connect-cassandra.zip /app/secure-connect/secure-connect-cassandra.zip

# Create startup script with embedded Cassandra bundle
RUN echo '#!/bin/sh\n\
set -e\n\
\n\
# Use embedded secure connect bundle\n\
EMBEDDED_BUNDLE="/app/secure-connect/secure-connect-cassandra.zip"\n\
if [ -f "$EMBEDDED_BUNDLE" ]; then\n\
  echo "Using embedded Cassandra secure connect bundle: $EMBEDDED_BUNDLE"\n\
  export ASTRA_SECURE_CONNECT_BUNDLE="$EMBEDDED_BUNDLE"\n\
else\n\
  echo "ERROR: Embedded bundle not found at: $EMBEDDED_BUNDLE"\n\
  exit 1\n\
fi\n\
\n\
# Start the application\n\
echo "Starting application on port ${PORT:-$SERVER_PORT}"\n\
exec java -jar /app.jar --server.port=${PORT:-$SERVER_PORT}' > /start.sh && \
    chmod +x /start.sh

# Create app user (non-root) for security best practices
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set proper permissions for the app user
RUN chown appuser:appuser /app.jar /start.sh /app/secure-connect/secure-connect-cassandra.zip

# Switch to non-root user
USER appuser

EXPOSE 8080
ENV SERVER_PORT=8080

# Health check to monitor container health
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["/start.sh"]

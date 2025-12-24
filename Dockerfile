# Build stage
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

# Install curl, python3 and gcloud SDK for downloading secure connect bundle
RUN apt-get update && \
    apt-get install --no-install-recommends -y curl ca-certificates python3 python3-pip && \
    curl -sSL https://sdk.cloud.google.com | bash && \
    mv /root/google-cloud-sdk /opt/google-cloud-sdk && \
    /opt/google-cloud-sdk/bin/gcloud components update --quiet && \
    chmod -R 755 /opt/google-cloud-sdk && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /opt/google-cloud-sdk/platform/gsutil/third_party/urllib3/dummyserver/certs && \
    rm -rf /opt/google-cloud-sdk/platform/bq/third_party && \
    rm -rf /opt/google-cloud-sdk/.install && \
    rm -rf /opt/google-cloud-sdk/.git

ENV PATH=$PATH:/opt/google-cloud-sdk/bin

COPY --from=builder /app/target/*.jar app.jar

# Create Cassandra bundle directory with proper permissions
RUN mkdir -p /tmp/cassandra && chmod 777 /tmp/cassandra

# Create startup script to download Cassandra secure connect bundle
RUN echo '#!/bin/sh\n\
set -e\n\
\n\
# Ensure gcloud can write to the directory\n\
mkdir -p /tmp/cassandra\n\
chmod 777 /tmp/cassandra\n\
\n\
# Initialize gcloud with Application Default Credentials\n\
# Cloud Run automatically provides credentials via GOOGLE_APPLICATION_CREDENTIALS\n\
if [ -z "$GOOGLE_APPLICATION_CREDENTIALS" ] && [ -f "$HOME/.config/gcloud/application_default_credentials.json" ]; then\n\
  export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.config/gcloud/application_default_credentials.json"\n\
fi\n\
\n\
# Download bundle if GCS path is provided\n\
if [ -n "$ASTRA_SECURE_CONNECT_BUNDLE" ]; then\n\
  if echo "$ASTRA_SECURE_CONNECT_BUNDLE" | grep -q "^gs://"; then\n\
    echo "Downloading Cassandra secure connect bundle from GCS: $ASTRA_SECURE_CONNECT_BUNDLE"\n\
    LOCAL_BUNDLE="/tmp/cassandra/secure-connect-cassandra.zip"\n\
    \n\
    # Attempt download with retries\n\
    RETRY_COUNT=0\n\
    MAX_RETRIES=3\n\
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do\n\
      if gsutil -m cp "$ASTRA_SECURE_CONNECT_BUNDLE" "$LOCAL_BUNDLE"; then\n\
        export ASTRA_SECURE_CONNECT_BUNDLE="$LOCAL_BUNDLE"\n\
        echo "✓ Successfully downloaded bundle to: $LOCAL_BUNDLE"\n\
        break\n\
      else\n\
        RETRY_COUNT=$((RETRY_COUNT + 1))\n\
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then\n\
          echo "Retry attempt $RETRY_COUNT/$MAX_RETRIES... sleeping 5 seconds"\n\
          sleep 5\n\
        fi\n\
      fi\n\
    done\n\
    \n\
    if [ ! -f "$LOCAL_BUNDLE" ]; then\n\
      echo "✗ ERROR: Failed to download bundle after $MAX_RETRIES attempts"\n\
      echo "  Ensure the GCS bucket is accessible and credentials are configured"\n\
      echo "  Set ASTRA_SECURE_CONNECT_BUNDLE=gs://bucket/path/to/secure-connect-cassandra.zip"\n\
      exit 1\n\
    fi\n\
  fi\n\
fi\n\
\n\
# Start the application with all environment variables\n\
echo "Starting application on port ${PORT:-$SERVER_PORT}"\n\
exec java -jar /app.jar --server.port=${PORT:-$SERVER_PORT}' > /start.sh && \
    chmod +x /start.sh

# Create app user (non-root) for security best practices
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set proper permissions for the app user
RUN chown appuser:appuser /app.jar /start.sh /tmp/cassandra

# Switch to non-root user
USER appuser

EXPOSE 8080
ENV SERVER_PORT=8080

# Health check to monitor container health
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["/start.sh"]

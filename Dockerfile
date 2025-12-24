# Build stage
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

# Install curl, python3 and gcloud SDK for downloading secure connect bundle
RUN apt-get update && \
    apt-get install -y curl ca-certificates python3 python3-pip && \
    curl -sSL https://sdk.cloud.google.com | bash && \
    /root/google-cloud-sdk/bin/gcloud components update --quiet && \
    rm -rf /var/lib/apt/lists/*

ENV PATH=$PATH:/root/google-cloud-sdk/bin

COPY --from=builder /app/target/*.jar app.jar

# Create startup script to download Cassandra secure connect bundle
RUN echo '#!/bin/sh\n\
if [ -n "$ASTRA_SECURE_CONNECT_BUNDLE" ]; then\n\
  if echo "$ASTRA_SECURE_CONNECT_BUNDLE" | grep -q "^gs://"; then\n\
    echo "Downloading Cassandra secure connect bundle from GCS..."\n\
    mkdir -p /tmp/cassandra\n\
    gsutil cp "$ASTRA_SECURE_CONNECT_BUNDLE" /tmp/cassandra/secure-connect-cassandra.zip\n\
    export ASTRA_SECURE_CONNECT_BUNDLE=/tmp/cassandra/secure-connect-cassandra.zip\n\
  fi\n\
fi\n\
java -jar /app.jar --server.port=${PORT:-$SERVER_PORT}' > /start.sh && \
    chmod +x /start.sh

EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["/start.sh"]

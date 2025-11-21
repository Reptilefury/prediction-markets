#!/bin/bash

# Load environment variables
export DATABASE_URL="r2dbc:postgresql://postgres:postgres@localhost:5434/postgres"
export JDBC_DATABASE_URL="jdbc:postgresql://localhost:5434/postgres"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="postgres"
export REDIS_HOST="localhost"
export REDIS_PORT="6380"
export REDIS_PASSWORD="redis123"
export BLNK_API_URL="http://localhost:5001"
export PORT="8080"
export BASE_URL="http://localhost:8080"
export MAGIC_API_KEY="mock_magic_key"
export MAGIC_API_URL="https://api.magic.link"
export ENCLAVE_API_KEY="mock_enclave_key"
export ENCLAVE_API_URL="https://api.enclave.io"
export WALLETCONNECT_PROJECT_ID="mock_walletconnect_id"
export DYNAMIC_ENVIRONMENT_ID="mock_dynamic_id"
export GCP_PROJECT_ID="heroic-equinox-474616-i5"
export PUBSUB_TOPIC_WALLET="mock_topic"
export PUBSUB_SUBSCRIPTION_WALLET="mock_subscription"

echo "🚀 Starting prediction-markets application with local environment..."
echo "📊 Database: $DATABASE_URL"
echo "🔴 Redis: $REDIS_HOST:$REDIS_PORT"
echo "💰 BLNK: $BLNK_API_URL"
echo ""

# Run the application
./mvnw spring-boot:run

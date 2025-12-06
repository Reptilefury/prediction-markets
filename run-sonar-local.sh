#!/bin/bash

# Simple script to run SonarQube analysis locally
# This bypasses SonarCloud and runs analysis without uploading results

echo "Running local SonarQube analysis..."

# Check if SonarQube server is running locally
if curl -s http://localhost:9000 > /dev/null 2>&1; then
    echo "Local SonarQube server detected, running analysis..."
    mvn clean compile test jacoco:report sonar:sonar \
        -Dsonar.host.url=http://localhost:9000 \
        -Dsonar.login=admin \
        -Dsonar.password=admin
else
    echo "No local SonarQube server found. Skipping analysis."
    echo "To run SonarQube locally:"
    echo "1. Download SonarQube Community Edition"
    echo "2. Start with: ./bin/linux-x86-64/sonar.sh start"
    echo "3. Access at http://localhost:9000"
    echo "4. Run this script again"
fi

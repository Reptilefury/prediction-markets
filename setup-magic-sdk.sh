#!/bin/bash
# Setup script to install Magic Node SDK for GraalVM integration

echo "Setting up Magic Node SDK..."

cd src/main/resources/magic-node

if [ ! -d "node_modules" ]; then
    echo "Installing npm dependencies..."
    npm install
    if [ $? -eq 0 ]; then
        echo "✓ Magic Node SDK installed successfully"
    else
        echo "✗ Failed to install Magic Node SDK"
        exit 1
    fi
else
    echo "✓ node_modules already exists, skipping npm install"
fi

cd ../../..

echo "✓ Setup complete! You can now start the application."

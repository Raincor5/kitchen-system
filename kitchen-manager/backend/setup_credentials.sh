#!/bin/bash

# Script to securely set up Google Cloud credentials

# Create credentials directory if it doesn't exist
mkdir -p /home/mckrotsky/kitchen-manager/backend/credentials

# Check if credentials file is provided as an argument
if [ -z "$1" ]; then
    echo "Usage: $0 <path-to-credentials-file>"
    echo "Example: $0 ~/Downloads/google-credentials.json"
    exit 1
fi

# Check if the source file exists
if [ ! -f "$1" ]; then
    echo "Error: Credentials file not found at $1"
    exit 1
fi

# Copy the credentials file to the credentials directory
echo "Copying credentials file to secure location..."
cp "$1" /home/mckrotsky/kitchen-manager/backend/credentials/google-credentials.json

# Set restrictive permissions
echo "Setting secure permissions..."
chmod 600 /home/mckrotsky/kitchen-manager/backend/credentials/google-credentials.json

# Verify the file was copied and permissions were set
if [ -f "/home/mckrotsky/kitchen-manager/backend/credentials/google-credentials.json" ]; then
    echo "Credentials file successfully set up at:"
    echo "/home/mckrotsky/kitchen-manager/backend/credentials/google-credentials.json"
    echo "Permissions: $(ls -la /home/mckrotsky/kitchen-manager/backend/credentials/google-credentials.json | awk '{print $1}')"
else
    echo "Error: Failed to set up credentials file"
    exit 1
fi

echo "Done! The Google Cloud Vision API should now work with your credentials." 
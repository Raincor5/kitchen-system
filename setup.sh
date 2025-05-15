#!/bin/bash

# Kitchen System Setup Script
# This script helps set up the development environment for the Kitchen System

echo "Setting up Kitchen Management System..."

# Function to check if a command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Check for required tools
echo "Checking for required tools..."

# Check for Python
if command_exists python3; then
  PYTHON="python3"
elif command_exists python; then
  PYTHON="python"
else
  echo "Error: Python is not installed. Please install Python 3.8 or higher."
  exit 1
fi

# Check Python version
PYTHON_VERSION=$($PYTHON -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')
echo "Python version: $PYTHON_VERSION"

# Check for pip
if ! command_exists pip; then
  echo "Error: pip is not installed. Please install pip."
  exit 1
fi

# Check for virtualenv
if ! command_exists virtualenv; then
  echo "virtualenv not found. Installing virtualenv..."
  pip install virtualenv
fi

# Check for Git
if ! command_exists git; then
  echo "Error: Git is not installed. Please install Git."
  exit 1
fi

# Set up kitchen-manager
echo "Setting up kitchen-manager..."
cd kitchen-manager || exit 1

# Create and activate virtual environment
virtualenv venv
source venv/bin/activate

# Install requirements
pip install -r backend/requirements.txt

# Set up environment variables if they don't exist
if [ ! -f .env ]; then
  echo "Creating .env file from example..."
  if [ -f .env.example ]; then
    cp .env.example .env
    echo "Please edit kitchen-manager/.env with your configuration."
  else
    echo "Warning: .env.example not found. Creating empty .env file."
    touch .env
  fi
fi

# Deactivate virtualenv
deactivate

# Return to root directory
cd ..

# Set up FFT frontend
echo "Setting up FFT Frontend..."

cd ftt-frontend/fft-android || exit 1

# Check for Android dependencies
if command_exists ./gradlew; then
  echo "Gradle wrapper found."
else
  echo "Warning: Gradle wrapper not found. Make sure Android Studio is properly set up."
fi

# Set up environment variables if they don't exist
if [ ! -f .env ]; then
  echo "Creating .env file from example..."
  if [ -f .env.example ]; then
    cp .env.example .env
    echo "Please edit ftt-frontend/fft-android/.env with your configuration."
  else
    echo "Warning: .env.example not found. Creating empty .env file."
    touch .env
  fi
fi

# Return to root directory
cd ../..

echo "Setup complete! Please check the README.md files for further instructions."
echo "Next steps:"
echo "1. Configure your .env files with proper settings"
echo "2. Run 'cd kitchen-manager && source venv/bin/activate && cd backend && python app/main.py' to start the backend"
echo "3. Run 'cd ftt-frontend/fft-android && ./gradlew installDebug' to build and install the Android app" 
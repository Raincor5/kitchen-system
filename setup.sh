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

# Check for Node.js
if ! command_exists node; then
  echo "Error: Node.js is not installed. Please install Node.js 16 or higher."
  exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d 'v' -f 2 | cut -d '.' -f 1)
echo "Node.js version: $NODE_VERSION"

if [ "$NODE_VERSION" -lt 16 ]; then
  echo "Warning: Node.js version is less than 16. Some components may not work correctly."
fi

# Check for npm
if ! command_exists npm; then
  echo "Error: npm is not installed. Please install npm."
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

# Set up prep-tracker
echo "Setting up Prep Tracker..."

cd prep-tracker || exit 1

# Install npm dependencies
if [ -f package.json ]; then
  echo "Installing Prep Tracker dependencies..."
  npm install --quiet
else
  echo "Warning: package.json not found for Prep Tracker."
fi

# Set up environment variables if they don't exist
if [ ! -f .env ]; then
  echo "Creating .env file from example..."
  if [ -f .env.example ]; then
    cp .env.example .env
    echo "Please edit prep-tracker/.env with your configuration."
  else
    echo "Warning: .env.example not found. Creating empty .env file."
    touch .env
  fi
fi

# Return to root directory
cd ..

# Set up recipe-upscaler backend
echo "Setting up Recipe Upscaler Backend..."

cd recipe-upscaler/backend || exit 1

# Install npm dependencies
if [ -f package.json ]; then
  echo "Installing Recipe Upscaler Backend dependencies..."
  npm install --quiet
else
  echo "Warning: package.json not found for Recipe Upscaler Backend."
fi

# Set up environment variables if they don't exist
if [ ! -f .env ]; then
  echo "Creating .env file from example..."
  if [ -f .env.example ]; then
    cp .env.example .env
    echo "Please edit recipe-upscaler/backend/.env with your configuration."
  else
    echo "Warning: .env.example not found. Creating empty .env file."
    touch .env
  fi
fi

# Return to root directory
cd ../..

# Set up recipe-upscaler calculator (web)
echo "Setting up Recipe Upscaler Web App..."

cd recipe-upscaler/calculator || exit 1

# Install npm dependencies
if [ -f package.json ]; then
  echo "Installing Recipe Upscaler Web App dependencies..."
  npm install --quiet
else
  echo "Warning: package.json not found for Recipe Upscaler Web App."
fi

# Return to root directory
cd ../..

# Set up recipe-upscaler calculator-cross (mobile)
echo "Setting up Recipe Upscaler Mobile App..."

cd recipe-upscaler/calculator-cross || exit 1

# Install npm dependencies
if [ -f package.json ]; then
  echo "Installing Recipe Upscaler Mobile App dependencies..."
  npm install --quiet
else
  echo "Warning: package.json not found for Recipe Upscaler Mobile App."
fi

# Set up environment variables if they don't exist
if [ ! -f .env ]; then
  echo "Creating .env file from example..."
  if [ -f .env.example ]; then
    cp .env.example .env
    echo "Please edit recipe-upscaler/calculator-cross/.env with your configuration."
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
echo "2. Start the backend services:"
echo "   - Kitchen Manager: cd kitchen-manager && source venv/bin/activate && cd backend && python app/main.py"
echo "   - Recipe Upscaler: cd recipe-upscaler/backend && node server.mjs"
echo "3. Start the frontend applications:"
echo "   - FFT Android: cd ftt-frontend/fft-android && ./gradlew installDebug"
echo "   - Prep Tracker: cd prep-tracker && npx expo start"
echo "   - Recipe Calculator Web: cd recipe-upscaler/calculator && npm start"
echo "   - Recipe Calculator Mobile: cd recipe-upscaler/calculator-cross && npx expo start" 
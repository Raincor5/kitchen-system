# Recipe Upscaler

A comprehensive recipe management and scaling system with multiple components.

## Components

### Backend

Node.js backend service for recipe management, storage, and API endpoints. Features include:

- Recipe storage and retrieval
- Ingredient management
- Scaling calculations
- API endpoints for mobile and web clients
- Ngrok integration for remote access

### Calculator (Web)

React web application for scaling recipes. Features include:

- Recipe creation and editing
- Automatic scaling calculations
- Unit conversion
- Recipe preview
- Export to various formats

### Calculator Cross-Platform

Expo/React Native mobile application for scaling recipes on the go. Features include:

- Cross-platform support (iOS, Android, Web)
- Recipe scaling
- Offline support
- Integration with backend services

## Setup

Each component has its own setup instructions. Please refer to the individual README files in each directory:

- [Backend Setup](./backend/README.md)
- [Web Calculator Setup](./calculator/README.md)
- [Mobile Calculator Setup](./calculator-cross/README.md)

## Development Workflow

1. Start the backend server first:
```bash
cd backend
npm install
npm start
```

2. Start the web or mobile client:
```bash
# For web
cd calculator
npm install
npm start

# For mobile
cd calculator-cross
npm install
npx expo start
```

## Integration with Other Components

The Recipe Upscaler integrates with:
- Kitchen Manager for recipe data in label printing
- Prep Tracker for recipe reference during prep planning

## Architecture

The system follows a client-server architecture:

- **Backend**: Node.js server with RESTful API
- **Web Client**: React SPA for desktop use
- **Mobile Client**: React Native app for mobile users

Data flows between components via RESTful API calls, with the backend serving as the central data repository. 
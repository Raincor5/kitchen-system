# Recipe Upscaler Mobile App

A cross-platform mobile application for recipe scaling and management built with Expo and React Native.

## Features

- Cross-platform support (iOS, Android, Web)
- Recipe viewing and scaling
- Offline recipe storage
- Synchronization with backend
- Intuitive mobile-friendly interface
- Unit conversion on-the-go

## Setup

### Requirements

- Node.js 16+
- npm or yarn
- Expo CLI
- Android Studio or Xcode (for mobile development)
- Recipe Upscaler Backend (for full functionality)

### Installation

1. Install dependencies:
```bash
npm install
```

2. Configure environment variables:
```bash
cp .env.example .env
# Edit .env with your settings
```

3. Run the application in development mode:
```bash
npx expo start
```

This will start the Expo development server, allowing you to run the app on:
- iOS Simulator
- Android Emulator
- Physical devices via Expo Go app
- Web browser

## Development

### Project Structure

- `/app` - Main application screens and navigation
- `/components` - Reusable UI components
- `/context` - React Context API providers
- `/constants` - Global constants and configuration
- `/types` - TypeScript type definitions
- `/assets` - Images, fonts and other assets

### Navigation

The app uses Expo Router for navigation with the following structure:
- `app/(tabs)` - Tab-based navigation screens
- `app/recipe-detail.tsx` - Recipe detail screen
- `app/modal.tsx` - Modal screens

## Building for Production

### EAS Build

This project uses EAS (Expo Application Services) for building:

```bash
# Configure EAS
npx eas-cli configure

# Build for Android
npx eas build --platform android

# Build for iOS
npx eas build --platform ios
```

### Local Builds

For development builds:

```bash
npx expo run:android
npx expo run:ios
```

## Integration

The mobile app integrates with:
- Recipe Upscaler Backend via RESTful API
- Kitchen Manager for label data access
- Other kitchen management tools in the ecosystem 
# Food Traceability Tracker Frontend

Android application for food traceability and label management.

## Features

- Label printing
- QR code scanning
- Label layout design
- Defrosting and preparation tracking
- Integration with backend services

## Setup

### Requirements

- Android Studio Giraffe or later
- Android SDK level 24+
- Kotlin 1.7+

### Installation

1. Open the project in Android Studio
2. Copy `.env.example` to `.env` and configure the variables
3. Run Gradle sync to download dependencies
4. Build and install the app using Android Studio or Gradle

```bash
cd fft-android
./gradlew clean build
./gradlew installDebug
```

## Configuration

The application can be configured through environment variables in the `.env` file:

- `API_BASE_URL`: URL of the backend server
- `DEFAULT_PRINTER_IP`: Default printer IP address
- `DEFAULT_PRINTER_PORT`: Default printer port

## Architecture

The app follows MVVM architecture with the following components:

- **Views**: Activities and custom views for the UI
- **ViewModels**: Handle UI-related data and business logic
- **Models/Data**: Data classes and data access objects
- **Services**: Background services for printing and communication

## Testing

Run unit tests with:

```bash
./gradlew test
```

Run instrumented tests with:

```bash
./gradlew connectedAndroidTest
``` 
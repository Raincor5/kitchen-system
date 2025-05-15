# Prep Tracker

Mobile application for tracking food preparation and managing gastronorm trays.

## Features

- Prep tracking for kitchen items
- Gastronorm tray organization
- Digital recipe management
- Real-time updates and synchronization
- Advanced mode for detailed preparation planning

## Setup

### Requirements

- Node.js 16+
- Expo CLI
- Android Studio or Xcode (for mobile development)

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

3. Run the application:
```bash
npx expo start
```

## Usage

The app provides several screens:

- **Home**: Overview of all prep items
- **Advanced Mode**: Detailed prep planning with drag-and-drop functionality
- **GN Organizer**: Organize items in gastronorm trays
- **Overall Summary**: Summary view of all kitchen prep status

## Configuration

The app can be configured through:

- `.env` file - API endpoints and environment variables
- `constants/apiConfig.ts` - API configuration
- `app/config.ts` - App-wide configuration

## Architecture

The app follows a context-based React architecture:

- **Context Providers**: Manage global state (DishesContext, RecipeContext, etc.)
- **Components**: Reusable UI elements
- **Services**: API communication and data handling
- **Types**: TypeScript type definitions

## Development

To build for production:

```bash
npx expo build:android  # For Android
npx expo build:ios      # For iOS
```

## Integration with Other Components

This app integrates with:
- Kitchen Manager backend for label printing
- Recipe Upscaler for recipe management 
# Recipe Upscale Calculator

A React web application for creating, editing, and scaling recipes.

## Features

- Intuitive recipe creation and editing
- Automatic scaling calculations
- Unit conversion
- Recipe preview with formatted output
- Export to various formats
- Integration with backend API for storage
- AI-assisted recipe management

## Setup

### Requirements

- Node.js 16+
- npm or yarn
- Recipe Upscaler Backend (for full functionality)

### Installation

1. Install dependencies:
```bash
npm install
```

2. Configure environment variables (if needed):
```bash
cp .env.example .env
# Edit .env with your settings
```

3. Start the development server:
```bash
npm start
```

## Usage

The application consists of several main components:

1. **Recipe Builder**: Create and edit recipes with ingredients and instructions
2. **Recipe Upscaler**: Scale recipes by a factor or to a specific yield
3. **Recipe Preview**: View the formatted recipe with scaled ingredients
4. **AI Integration**: Get assistance with recipe conversion and normalization

## Building for Production

To build the application for production:

```bash
npm run build
```

This will create a `build` directory with optimized production assets.

## Technologies Used

- React
- JavaScript/ES6
- CSS
- Fetch API for backend communication

## Architecture

The application follows a component-based architecture:

- **App.js**: Main application container
- **RecipeBuilder.js**: Component for creating/editing recipes
- **RecipeUpscaler.js**: Component for scaling recipes
- **RecipePreview.js**: Component for previewing formatted recipes
- **AIIntegration.js**: Component for AI-assisted features

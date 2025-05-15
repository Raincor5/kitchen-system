# Recipe Upscaler Backend

Node.js backend service for recipe management, storage, and scaling calculations.

## Features

- REST API for recipe management
- Recipe storage and retrieval
- Ingredient database with normalization
- Scaling calculations with unit conversion
- Authentication and user management
- File upload handling for recipe imports
- Ngrok integration for remote access

## Setup

### Requirements

- Node.js 16+
- npm or yarn
- MongoDB (optional, configurable via .env)

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

3. Run the server:
```bash
node server.mjs
```

## API Endpoints

### Recipes

- `GET /api/recipes` - Get all recipes
- `GET /api/recipes/:id` - Get recipe by ID
- `POST /api/recipes` - Create a new recipe
- `PUT /api/recipes/:id` - Update a recipe
- `DELETE /api/recipes/:id` - Delete a recipe

### Ingredients

- `GET /api/ingredients` - Get all ingredients
- `GET /api/ingredients/:id` - Get ingredient by ID
- `POST /api/ingredients` - Create a new ingredient

### Scaling

- `POST /api/scale` - Scale a recipe
- `POST /api/convert` - Convert units

## Configuration

The server can be configured through:

- `.env` file - Database URLs, ports, and API keys
- `ngrok.yml` - Ngrok configuration for remote access

## Database Normalization

The `database normalization` directory contains scripts for normalizing ingredient data to ensure consistency across recipes. Run the normalization process with:

```bash
node database-normalization/export_with_embeddings.py
```

## Usage with Ngrok

To enable remote access via Ngrok:

1. Configure `ngrok.yml` with your authtoken
2. Run the update script:
```bash
./update_ngrok_urls.sh
```

## Development

For development and testing:

```bash
node test.mjs
``` 
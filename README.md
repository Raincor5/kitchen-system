# Kitchen Management System

A comprehensive system for kitchen management, food traceability, and label printing.

## Project Structure

This repository contains multiple related projects that work together to form a complete kitchen management solution:

- **kitchen-manager**: Backend services for kitchen management including label processing and printing
- **ftt-frontend**: Food Traceability Tracker Android application
- **prep-tracker**: Mobile application for food preparation tracking and planning
- **recipe-upscaler**: Recipe management and scaling tools, including backend and frontend applications

## Components

### Kitchen Manager

The kitchen manager is responsible for:
- Label generation and printing
- Preparation tracking
- Recipe processing
- Integration with kitchen equipment

### Food Traceability Tracker (FTT)

The FTT system includes:
- Android app for label printing and scanning
- Label layout design functionality
- Integration with kitchen processes

### Prep Tracker

Tracks food preparation processes:
- Mobile app for real-time prep tracking
- Gastronorm tray organization
- Advanced planning features
- Integration with recipe system

### Recipe Upscaler

Manages recipes and portion scaling:
- Backend API for recipe data management
- Web app for desktop recipe scaling
- Mobile app for on-the-go recipe access
- Ingredient normalization features

## Setup and Installation

### Automated Setup

For a quick start, run the setup script:

```bash
chmod +x setup.sh
./setup.sh
```

This will:
1. Check for required dependencies
2. Set up Python virtual environments
3. Install required packages
4. Create configuration files from templates

### Manual Setup

Each component has its own setup instructions. Please refer to the individual README files in each directory:

- [Kitchen Manager Setup](./kitchen-manager/README.md)
- [FTT Frontend Setup](./ftt-frontend/README.md)
- [Prep Tracker Setup](./prep-tracker/README.md)
- [Recipe Upscaler Setup](./recipe-upscaler/README.md)

## Development

### Requirements

- Python 3.8+ for backend services
- Node.js 16+ for JavaScript applications
- Android Studio for mobile app development
- Expo CLI for React Native apps
- Docker for containerized deployment

### Environment Setup

1. Clone this repository
2. Set up environment variables (see `.env.example` files in each directory)
3. Follow the setup instructions for each component

## System Architecture

The system is designed as a set of microservices that communicate with each other:

- **Kitchen Manager Backend**: Central service for label generation and printing
- **FTT Android App**: Mobile interface for label printing and scanning
- **Prep Tracker App**: Mobile interface for prep tracking and planning
- **Recipe Upscaler Backend**: API for recipe management and scaling
- **Recipe Upscaler Web/Mobile**: User interfaces for recipe management

## License

Proprietary - All rights reserved. 
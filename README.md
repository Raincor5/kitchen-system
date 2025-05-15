# Kitchen Management System

A comprehensive system for kitchen management, food traceability, and label printing.

## Project Structure

This repository contains multiple related projects that work together to form a complete kitchen management solution:

- **kitchen-manager**: Backend services for kitchen management including label processing and printing
- **ftt-frontend**: Food Traceability Tracker Android application
- **ftt-backend**: Food Traceability Tracker backend services

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

## Development

### Requirements

- Python 3.8+ for backend services
- Android Studio for mobile app development
- Docker for containerized deployment

### Environment Setup

1. Clone this repository
2. Set up environment variables (see `.env.example` files in each directory)
3. Follow the setup instructions for each component

## License

Proprietary - All rights reserved. 
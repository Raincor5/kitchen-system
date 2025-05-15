# Kitchen Management System

![Kitchen Management System](https://img.shields.io/badge/Kitchen-Management-blue)
![Version](https://img.shields.io/badge/version-1.0.0-green)
![License](https://img.shields.io/badge/license-Proprietary-red)

A comprehensive system for professional kitchen management, food traceability, and intelligent label printing.

<p align="center">
  <img src="docs/assets/kitchen-system-logo.png" alt="Kitchen Management System Logo" width="300" />
</p>

## 🍳 Overview

The Kitchen Management System is an integrated solution designed to streamline and optimize kitchen operations in professional settings. It combines several specialized components that work together to provide a complete kitchen management experience:

* **Label Generation & Printing**: Create and print labels for food items with QR codes
* **Food Traceability**: Track food items throughout their lifecycle
* **Recipe Management**: Manage and scale recipes for various portion sizes
* **Prep Tracking**: Monitor food preparation processes in real-time
* **Gastronorm Organization**: Optimize kitchen storage with digital tray management

## 🧩 Project Structure

This repository contains multiple related projects that work together to form a complete kitchen management solution:

```
kitchen-system/
├── kitchen-manager/     # Backend services for label processing and printing
├── ftt-frontend/        # Food Traceability Tracker Android application
├── prep-tracker/        # Mobile application for preparation tracking and planning
└── recipe-upscaler/     # Recipe management and scaling tools
    ├── backend/         # Recipe API and data management
    ├── calculator/      # Web-based recipe scaling application
    └── calculator-cross/ # Cross-platform mobile recipe app
```

## 📱 Components

### Kitchen Manager

<p align="center">
  <img src="docs/assets/kitchen-manager.png" alt="Kitchen Manager Screenshot" width="400" />
</p>

The kitchen manager is responsible for:
- Label generation and printing
- Preparation tracking
- Recipe processing
- Integration with kitchen equipment

**Key Features:**
- QR code generation for digital tracking
- Custom label design and printing
- Integration with label printers

### Food Traceability Tracker (FTT)

<p align="center">
  <img src="docs/assets/ftt-app.png" alt="FTT App Screenshot" width="400" />
</p>

The FTT system includes:
- Android app for label printing and scanning
- Label layout design functionality
- Integration with kitchen processes

**Key Features:**
- Scan labels to track food items
- Print labels directly from mobile devices
- Real-time updates on food status

### Prep Tracker

<p align="center">
  <img src="docs/assets/prep-tracker.png" alt="Prep Tracker Screenshot" width="400" />
</p>

Tracks food preparation processes:
- Mobile app for real-time prep tracking
- Gastronorm tray organization
- Advanced planning features
- Integration with recipe system

**Key Features:**
- Interactive drag-and-drop interface
- Real-time preparation status
- Digital inventory management

### Recipe Upscaler

<p align="center">
  <img src="docs/assets/recipe-upscaler.png" alt="Recipe Upscaler Screenshot" width="400" />
</p>

Manages recipes and portion scaling:
- Backend API for recipe data management
- Web app for desktop recipe scaling
- Mobile app for on-the-go recipe access
- Ingredient normalization features

**Key Features:**
- Automatic unit conversion
- Portion scaling with ingredient adjustments
- Recipe sharing and export

## 🚀 Setup and Installation

### Quick Start

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

## 💻 Development

### Requirements

- Python 3.8+ for backend services
- Node.js 16+ for JavaScript applications
- Android Studio for mobile app development
- Expo CLI for React Native apps
- Docker for containerized deployment

### Environment Setup

1. Clone this repository:
```bash
git clone https://github.com/Raincor5/kitchen-system.git
cd kitchen-system
```

2. Set up environment variables (see `.env.example` files in each directory)
3. Follow the setup instructions for each component

## 🏗️ System Architecture

<p align="center">
  <img src="docs/assets/system-architecture.png" alt="System Architecture Diagram" width="600" />
</p>

The system is designed as a set of microservices that communicate with each other:

- **Kitchen Manager Backend**: Central service for label generation and printing
- **FTT Android App**: Mobile interface for label printing and scanning
- **Prep Tracker App**: Mobile interface for prep tracking and planning
- **Recipe Upscaler Backend**: API for recipe management and scaling
- **Recipe Upscaler Web/Mobile**: User interfaces for recipe management

## 📊 Data Flow

1. **Recipe Creation**: Recipes are created and stored in the Recipe Upscaler
2. **Preparation Planning**: Prep Tracker uses recipe data to plan preparation tasks
3. **Label Generation**: Kitchen Manager generates labels for prepared items
4. **Label Printing**: FTT Android app prints labels to physical printers
5. **Tracking**: QR codes on labels enable ongoing tracking throughout the food lifecycle

## 📄 API Documentation

API documentation is available for each backend service:

- [Kitchen Manager API](https://raincor5.github.io/kitchen-system/api/kitchen-manager)
- [Recipe Upscaler API](https://raincor5.github.io/kitchen-system/api/recipe-upscaler)

## 🛠️ Contributing

We welcome contributions to the Kitchen Management System! Here's how to get started:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📝 License

Proprietary - All rights reserved.

## 📧 Contact

Raincor5 - slava.krot5@gmail.com

Project Link: [https://github.com/Raincor5/kitchen-system](https://github.com/Raincor5/kitchen-system) 
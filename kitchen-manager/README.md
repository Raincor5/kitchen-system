# Kitchen Manager

Backend service for managing kitchen operations, label generation, and printing.

## Features

- Label generation and printing
- QR code generation for linking to digital systems
- Image processing for label recognition
- Integration with prep tracking system
- REST API for mobile and web clients

## Setup

### Requirements

- Python 3.8+
- pip
- ngrok for external access (optional)

### Installation

1. Create a virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

2. Install dependencies:
```bash
pip install -r backend/requirements.txt
```

3. Configure environment variables:
```bash
cp .env.example .env
# Edit .env with your settings
```

4. Run the application:
```bash
cd backend
python app/main.py
```

## API Endpoints

The service provides the following main endpoints:

- `/api/labels`: Label management
- `/api/printer`: Printer operations
- `/api/prep-tracking`: Preparation tracking
- `/api/gastronorm`: Gastronorm tray management

## Scripts

- `generate_qr_code.py`: Generate QR codes for linking physical items to digital records
- `update_ngrok_urls.sh`: Update ngrok URLs for external access
- `backend/verify_gastronorm_trays.py`: Verify gastronorm tray data

## Development

To contribute to the project:

1. Create a feature branch
2. Make your changes
3. Run tests: `pytest backend/tests/`
4. Submit a pull request

## Troubleshooting

For common issues, check:

- Connection issues: Verify printer connectivity and network settings
- Label printing problems: Check printer settings and label format
- API errors: Check application logs for details 
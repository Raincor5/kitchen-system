# Kitchen Manager Label System

A comprehensive label generation and printing system for kitchen management, integrated with other kitchen management applications.

## Features

- Generate labels for trays with detailed information
- Print labels to physical label printers
- Fetch data from other kitchen management applications:
  - Prep Tracker: Dish and prep bag information
  - Recipe Upscaler: Recipe and ingredient information
  - Kitchen Manager: Tray information
- Enhanced label generation with data from multiple sources
- Batch label generation and printing

## Architecture

The system consists of the following components:

1. **Label Generator Service**: Generates formatted labels based on tray data
2. **Label Printer Service**: Handles the physical printing of labels
3. **Data Fetcher Service**: Fetches data from other applications
4. **API Endpoints**: RESTful API for label generation and printing

## Integration with Other Applications

The system integrates with the following applications:

- **Prep Tracker**: Fetches dish and prep bag information
- **Recipe Upscaler**: Fetches recipe and ingredient information
- **Kitchen Manager**: Fetches tray information

## API Endpoints

### Labels

- `POST /api/labels/generate`: Generate labels from label data
- `POST /api/labels/generate-enhanced/{tray_id}`: Generate an enhanced label with data from other applications
- `POST /api/labels/generate-enhanced-batch`: Generate enhanced labels for multiple trays
- `POST /api/labels/print`: Print labels
- `POST /api/labels/print-enhanced/{tray_id}`: Generate and print an enhanced label
- `POST /api/labels/print-enhanced-batch`: Generate and print enhanced labels for multiple trays
- `GET /api/labels/printer/status`: Get printer status

### Trays

- `GET /api/trays`: Get all trays with optional filtering
- `GET /api/trays/{tray_id}`: Get a specific tray by ID
- `POST /api/trays`: Create a new tray
- `PUT /api/trays/{tray_id}`: Update an existing tray
- `DELETE /api/trays/{tray_id}`: Delete a tray
- `POST /api/trays/batch`: Create multiple trays at once

## Installation

1. Clone the repository
2. Install dependencies:
   ```
   pip install -r backend/requirements.txt
   ```
3. Configure the application:
   - Set up the API endpoints for other applications in the `DataFetcher` class
   - Configure the printer settings in the `LabelPrinter` class

## Running the Application

```
cd backend
uvicorn app.main:app --reload
```

## Configuration

The application can be configured by modifying the following files:

- `backend/app/services/data_fetcher.py`: Configure API endpoints for other applications
- `backend/app/services/label_printer.py`: Configure printer settings
- `backend/app/main.py`: Configure CORS settings

## Dependencies

- FastAPI: Web framework
- Uvicorn: ASGI server
- Pydantic: Data validation
- HTTPX: HTTP client for fetching data from other applications
- PyCUPS: CUPS client for printing
- Python-Jose: JWT token handling
- Passlib: Password hashing
- Python-dotenv: Environment variable management 
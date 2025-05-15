from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import requests
import os
import logging
from typing import Dict, Any, Optional

router = APIRouter()
logger = logging.getLogger(__name__)

class PrinterSettings(BaseModel):
    linesPerFeed: int = 3
    fontSize: float = 1.0
    fontStyle: str = "normal"
    alignment: str = "left"
    useQRCode: bool = False
    printLogo: bool = False
    darkMode: bool = False

class PrintTextRequest(BaseModel):
    text: str
    settings: Optional[PrinterSettings] = None

class PrintLabelRequest(BaseModel):
    label_data: Dict[str, Any]
    settings: Optional[PrinterSettings] = None

@router.get("/awaken")
async def awaken():
    """
    Endpoint for the Android app to check if the API is available.
    """
    return {"status": "awake", "message": "API is ready"}

@router.post("/print-text")
async def print_text(request: PrintTextRequest):
    try:
        # Get the Android printer service URL from environment
        android_service_url = os.environ.get('LABEL_DETECTOR_URL', 'http://localhost:8080')
        
        # Send the print request to the Android service
        # The Android app will use the provided settings or fall back to defaults
        logger.info(f"Sending print request to Android service at {android_service_url}")
        
        # Prepare request data
        request_data = {
            "text": request.text,
            "settings": request.settings.dict() if request.settings else None
        }
        
        response = requests.post(
            f"{android_service_url}/print-text",
            json=request_data,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        if response.status_code == 200:
            return {"success": True, "status": "printed"}
        else:
            raise HTTPException(status_code=500, detail="Failed to print text")
            
    except Exception as e:
        logger.error(f"Error sending print request: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/print-label")
async def print_label(request: PrintLabelRequest):
    try:
        # Get the Android printer service URL from environment
        android_service_url = os.environ.get('LABEL_DETECTOR_URL', 'http://localhost:8080')
        
        # Send the print request to the Android service
        logger.info(f"Sending label print request to Android service at {android_service_url}")
        
        # Prepare request data
        request_data = {
            "label_data": request.label_data,
            "settings": request.settings.dict() if request.settings else None
        }
        
        response = requests.post(
            f"{android_service_url}/print-label",
            json=request_data,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        if response.status_code == 200:
            return {"success": True, "status": "printed"}
        else:
            raise HTTPException(status_code=500, detail="Failed to print label")
            
    except Exception as e:
        logger.error(f"Error sending print request: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e)) 
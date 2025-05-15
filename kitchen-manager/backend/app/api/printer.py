from fastapi import APIRouter, HTTPException, Depends
from typing import Dict, List, Optional
from pydantic import BaseModel
from datetime import datetime
import uuid
from ..services.label_generator import LabelGenerator
from ..services.label_printer import LabelPrinterService

router = APIRouter()
label_generator = LabelGenerator()
printer_service = LabelPrinterService()

# Store pending labels in memory (in a real app, this would be in a database)
pending_labels = []

class PrinterLabel(BaseModel):
    id: str
    tray_id: str
    formatted_label: str
    raw_data: Dict
    created_at: str
    printed: bool = False

class PrinterSettings(BaseModel):
    linesPerFeed: int
    fontSize: float
    fontStyle: str
    alignment: str
    useQRCode: bool
    printLogo: bool
    darkMode: bool

# Store settings in memory (in a real app, this would be in a database)
current_settings = PrinterSettings(
    linesPerFeed=3,
    fontSize=1.0,
    fontStyle="normal",
    alignment="left",
    useQRCode=False,
    printLogo=False,
    darkMode=False
)

@router.get("/status", response_model=Dict[str, str])
async def get_printer_status():
    """
    Get the current status of the label printer.
    For now, this is a mock implementation that always returns 'Ready'.
    In a real implementation, this would check the actual printer status.
    """
    try:
        # Mock printer status - in a real implementation, this would check the actual printer
        return {"status": "Ready"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error checking printer status: {str(e)}")

@router.get("/settings", response_model=PrinterSettings)
async def get_printer_settings():
    """Get the current printer settings."""
    try:
        return current_settings
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching printer settings: {str(e)}")

@router.put("/settings", response_model=PrinterSettings)
async def update_printer_settings(settings: PrinterSettings):
    """Update the printer settings."""
    try:
        global current_settings
        current_settings = settings
        # Update the printer service with new settings
        printer_service.update_settings(settings.dict())
        return current_settings
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating printer settings: {str(e)}")

@router.get("/pending", response_model=List[PrinterLabel])
async def get_pending_labels():
    """
    Get all pending labels that haven't been printed yet.
    This endpoint is used by the printer driver to fetch labels to print.
    """
    try:
        # Return only labels that haven't been printed yet
        return [label for label in pending_labels if not label.printed]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching pending labels: {str(e)}")

@router.post("/mark-printed/{label_id}")
async def mark_label_printed(label_id: str):
    """
    Mark a label as printed.
    This endpoint is used by the printer driver to mark a label as printed.
    """
    try:
        # Find the label in the pending labels list
        for label in pending_labels:
            if label.id == label_id:
                label.printed = True
                return {"status": "success", "message": f"Label {label_id} marked as printed"}
        
        # If the label is not found, return a 404 error
        raise HTTPException(status_code=404, detail=f"Label {label_id} not found")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error marking label as printed: {str(e)}")

@router.post("/add-label")
async def add_label(tray_id: str):
    """
    Add a label to the pending labels list.
    This endpoint is used by the kitchen manager to add a label to the pending labels list.
    """
    try:
        # Generate an enhanced label for the tray
        label_data = await label_generator.generate_enhanced_label(tray_id)
        
        # Create a new printer label
        new_label = PrinterLabel(
            id=str(uuid.uuid4()),
            tray_id=tray_id,
            formatted_label=label_data["formatted_label"],
            raw_data=label_data["raw_data"],
            created_at=datetime.now().isoformat(),
            printed=False
        )
        
        # Add the label to the pending labels list
        pending_labels.append(new_label)
        
        return {"status": "success", "message": f"Label for tray {tray_id} added to pending list", "label_id": new_label.id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error adding label: {str(e)}")

@router.get("/labels", response_model=List[PrinterLabel])
async def get_all_labels():
    """
    Get all labels, including those that have been printed.
    This endpoint is used by the printer driver to fetch all labels.
    """
    try:
        return pending_labels
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching labels: {str(e)}")

@router.on_event("shutdown")
async def shutdown_event():
    """
    Clean up resources when the application shuts down.
    """
    try:
        # Close the printer connection
        await printer_service.close()
    except Exception as e:
        print(f"Error closing printer connection: {str(e)}") 
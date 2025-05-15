from fastapi import APIRouter, HTTPException, Depends
from typing import List, Optional, Dict
from pydantic import BaseModel
from ..services.label_generator import LabelGenerator
from ..services.label_printer import LabelPrinterService
from datetime import datetime

# Define models directly in this file
class LabelCreate(BaseModel):
    tray_id: str
    dish_name: str
    prep_date: str
    expiry_date: str
    ingredients: List[str] = []
    allergens: Optional[List[str]] = None
    notes: Optional[str] = None
    
    class Config:
        # Allow extra fields in the model
        extra = "allow"

class Label(LabelCreate):
    id: str
    created_at: str

class LabelBatch(BaseModel):
    labels: List[LabelCreate]
    
    class Config:
        # Allow extra fields in the model
        extra = "allow"

router = APIRouter()
label_generator = LabelGenerator()
printer_service = LabelPrinterService()

@router.post("/generate", response_model=List[Label])
async def generate_labels(label_batch: LabelBatch):
    try:
        # Debug the incoming data
        print(f"Received label batch: {label_batch}")
        
        # Extract the raw data from the Pydantic model
        labels_data = []
        for label in label_batch.labels:
            # Convert Pydantic model to dict
            label_dict = label.dict()
            print(f"Processing label: {label_dict}")
            labels_data.append(label_dict)
        
        # Generate labels
        generated_labels = label_generator.generate_labels(labels_data)
        return generated_labels
    except Exception as e:
        import traceback
        print(f"Error generating labels: {str(e)}")
        print(traceback.format_exc())
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/generate-enhanced/{tray_id}")
async def generate_enhanced_label(tray_id: str):
    """
    Generate an enhanced label with data from other applications.
    """
    try:
        label = await label_generator.generate_enhanced_label(tray_id)
        return {"tray_id": tray_id, "label": label}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/generate-enhanced-batch")
async def generate_enhanced_batch_labels(tray_ids: List[str]):
    """
    Generate enhanced labels for multiple trays.
    """
    try:
        labels = await label_generator.generate_enhanced_batch_labels(tray_ids)
        return {"labels": [{"tray_id": tray_id, "label": label} for tray_id, label in zip(tray_ids, labels)]}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/print")
async def print_labels(label_batch: LabelBatch):
    try:
        # Generate labels
        generated_labels = label_generator.generate_labels(label_batch.labels)
        
        # Print each label
        results = []
        for label in generated_labels:
            # Parse dates
            prep_date = datetime.strptime(label["prep_date"], "%Y-%m-%d")
            expiry_date = datetime.strptime(label["expiry_date"], "%Y-%m-%d")
            
            # Print the label using our new service
            success = printer_service.print_label(
                dish_name=label["dish_name"],
                prep_date=prep_date,
                expiry_date=expiry_date,
                ingredients=label.get("ingredients", []),
                allergens=label.get("allergens", []),
                notes=label.get("notes", ""),
                tray_id=label["tray_id"]
            )
            
            results.append({
                "tray_id": label["tray_id"],
                "success": success,
                "status": "printed" if success else "error"
            })
        
        return {"results": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/print-enhanced/{tray_id}")
async def print_enhanced_label(tray_id: str):
    """
    Generate and print an enhanced label with data from other applications.
    """
    try:
        # Generate enhanced label
        label = await label_generator.generate_enhanced_label(tray_id)
        
        # Parse dates
        prep_date = datetime.strptime(label["prep_date"], "%Y-%m-%d")
        expiry_date = datetime.strptime(label["expiry_date"], "%Y-%m-%d")
        
        # Print the label using our new service
        success = printer_service.print_label(
            dish_name=label["dish_name"],
            prep_date=prep_date,
            expiry_date=expiry_date,
            ingredients=label.get("ingredients", []),
            allergens=label.get("allergens", []),
            notes=label.get("notes", ""),
            tray_id=tray_id
        )
        
        return {
            "tray_id": tray_id,
            "success": success,
            "status": "printed" if success else "error"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/print-enhanced-batch")
async def print_enhanced_batch_labels(tray_ids: List[str]):
    """
    Generate and print enhanced labels for multiple trays.
    """
    try:
        # Generate enhanced labels
        labels = await label_generator.generate_enhanced_batch_labels(tray_ids)
        
        # Print each label
        results = []
        for tray_id, label in zip(tray_ids, labels):
            # Parse dates
            prep_date = datetime.strptime(label["prep_date"], "%Y-%m-%d")
            expiry_date = datetime.strptime(label["expiry_date"], "%Y-%m-%d")
            
            # Print the label using our new service
            success = printer_service.print_label(
                dish_name=label["dish_name"],
                prep_date=prep_date,
                expiry_date=expiry_date,
                ingredients=label.get("ingredients", []),
                allergens=label.get("allergens", []),
                notes=label.get("notes", ""),
                tray_id=tray_id
            )
            
            results.append({
                "tray_id": tray_id,
                "success": success,
                "status": "printed" if success else "error"
            })
        
        return {"results": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/printer/status", response_model=Dict[str, str])
async def get_printer_status():
    """
    Get the current status of the label printer.
    """
    try:
        # In a real implementation, this would check the actual printer status
        # For now, we'll just return a mock status
        return {"status": "Ready"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error checking printer status: {str(e)}")

@router.get("/preview/{tray_id}")
async def preview_label(tray_id: str):
    """
    Preview a label for a specific tray without printing it.
    """
    try:
        # Generate enhanced label
        label = await label_generator.generate_enhanced_label(tray_id)
        
        # Format for display
        formatted_label = label_generator.format_for_printer(label)
        
        return {
            "tray_id": tray_id,
            "label": formatted_label,
            "raw_data": label
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error previewing label: {str(e)}")

@router.get("/{tray_id}/text")
async def get_label_text(tray_id: str):
    try:
        # Get the tray data
        tray = await get_tray(tray_id)
        if not tray:
            raise HTTPException(status_code=404, detail="Tray not found")
            
        # Generate the label text
        label_text = label_generator.generate_label_text(tray)
        
        return {"text": label_text}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.on_event("shutdown")
async def shutdown_event():
    """
    Clean up resources when the application shuts down.
    """
    await label_generator.close() 
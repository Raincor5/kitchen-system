from fastapi import APIRouter, HTTPException, Depends
from typing import Dict, Any
from pydantic import BaseModel
from datetime import datetime
from ..services.prep_tracker_service import PrepTrackerService

router = APIRouter()
prep_tracker_service = PrepTrackerService()

class PrepData(BaseModel):
    dish_name: str
    prep_date: str
    ingredients: list[str] = []
    allergens: list[str] = []
    notes: str = ""
    tray_id: str = None

@router.post("/prep-data")
async def receive_prep_data(data: PrepData):
    """
    Receive prep tracker data and trigger label printing.
    """
    try:
        # Convert the Pydantic model to a dictionary
        data_dict = data.dict()
        
        # Handle the prep data and print label
        success = await prep_tracker_service.handle_prep_data(data_dict)
        
        if success:
            return {"status": "success", "message": "Label printed successfully"}
        else:
            raise HTTPException(status_code=500, detail="Failed to print label")
            
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing prep data: {str(e)}") 
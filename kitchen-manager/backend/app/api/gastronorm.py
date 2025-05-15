from fastapi import APIRouter, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import List, Dict, Any
import json
import os
from pathlib import Path
from .state import SELECTED_TRAYS
import logging

router = APIRouter()

# Path to the GN.json file in the prep tracking app
# Use a more reliable path that doesn't depend on relative paths
GN_JSON_PATH = "/home/mckrotsky/projects/prep-tracker/prep-tracking-app/assets/documents/GN.json"

def load_all_trays():
    """
    Load all trays from the JSON file.
    """
    try:
        if not os.path.exists(GN_JSON_PATH):
            logging.warning(f"Gastronorm trays data not found at {GN_JSON_PATH}")
            # Return empty list instead of raising an error
            return []
            
        with open(GN_JSON_PATH, 'r') as f:
            return json.load(f)
    except Exception as e:
        logging.error(f"Error reading gastronorm trays: {str(e)}")
        # Return empty list instead of raising an error
        return []

class SelectedTraysRequest(BaseModel):
    tray_ids: List[str]

@router.post("/select")
async def select_trays(request: SelectedTraysRequest):
    """
    Set the list of selected gastronorm trays.
    """
    try:
        # Load all trays to validate the IDs
        all_trays = load_all_trays()
        valid_ids = {tray["id"] for tray in all_trays}
        
        # Validate that all requested tray IDs exist
        invalid_ids = set(request.tray_ids) - valid_ids
        if invalid_ids:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid tray IDs: {', '.join(invalid_ids)}"
            )
        
        # Update the selected trays
        global SELECTED_TRAYS
        SELECTED_TRAYS = request.tray_ids
        
        return {"message": "Selected trays updated successfully", "selected_trays": SELECTED_TRAYS}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating selected trays: {str(e)}")

@router.get("/list")
async def list_gastronorm_trays():
    """
    Get all gastronorm trays from the JSON file.
    If there are selected trays, only return those.
    Otherwise, return all trays.
    """
    try:
        # Load all trays
        all_trays = load_all_trays()
        
        # If there are selected trays, filter to only include those
        if SELECTED_TRAYS:
            trays = [tray for tray in all_trays if tray["id"] in SELECTED_TRAYS]
        else:
            trays = all_trays
            
        return JSONResponse(content=trays)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error reading gastronorm trays: {str(e)}")

@router.get("/{tray_id}")
async def get_tray(tray_id: str):
    """
    Get a specific gastronorm tray by ID, but only if it's selected.
    """
    try:
        # Check if the requested tray is selected
        if tray_id not in SELECTED_TRAYS:
            raise HTTPException(status_code=404, detail=f"Tray with ID {tray_id} is not currently selected")
        
        # Load all trays
        all_trays = load_all_trays()
        
        # Find the requested tray
        tray = next((t for t in all_trays if t["id"] == tray_id), None)
        if not tray:
            raise HTTPException(status_code=404, detail=f"Tray with ID {tray_id} not found")
            
        return JSONResponse(content=tray)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error reading gastronorm tray: {str(e)}")

@router.get("/stats/selected")
async def get_selected_tray_stats():
    """
    Get statistics about the currently selected trays.
    """
    try:
        # Load all trays
        all_trays = load_all_trays()
        
        # Filter to only include selected trays
        selected_trays = [tray for tray in all_trays if tray["id"] in SELECTED_TRAYS]
        
        # Calculate statistics
        total_volume = sum(tray["volumeLiters"] for tray in selected_trays)
        materials = set(tray["material"] for tray in selected_trays)
        
        return {
            "total_trays": len(selected_trays),
            "total_volume_liters": total_volume,
            "materials": sorted(materials)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error calculating tray statistics: {str(e)}")

@router.get("/{tray_id}/contents")
async def get_tray_contents(tray_id: str):
    """
    Get the contents of a specific tray, including prep data.
    """
    try:
        # Check if the requested tray is selected
        if tray_id not in SELECTED_TRAYS:
            raise HTTPException(status_code=404, detail=f"Tray with ID {tray_id} is not currently selected")
        
        # Load all trays
        all_trays = load_all_trays()
        
        # Find the requested tray
        tray = next((t for t in all_trays if t["id"] == tray_id), None)
        if not tray:
            raise HTTPException(status_code=404, detail=f"Tray with ID {tray_id} not found")
        
        # Get prep data for this tray
        from .prep_tracking import get_prep_data_for_tray
        prep_data = await get_prep_data_for_tray(tray_id)
        
        # Combine tray data with prep data
        tray_contents = {
            "tray": tray,
            "prep_data": prep_data
        }
            
        return JSONResponse(content=tray_contents)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error getting tray contents: {str(e)}") 
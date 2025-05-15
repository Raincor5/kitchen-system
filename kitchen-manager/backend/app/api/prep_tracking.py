from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import logging
import json
from datetime import datetime
import os
from pathlib import Path
import httpx
from ..core.config import settings
from .state import SELECTED_TRAYS

# Configure logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Create a file handler
handler = logging.FileHandler('prep_tracking.log')
handler.setLevel(logging.INFO)

# Create a formatting for the logs
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
handler.setFormatter(formatter)

# Add the handler to the logger
logger.addHandler(handler)

router = APIRouter()

# In-memory storage for prep data
PREP_DATA = {
    "dishes": [],
    "tray_contents": {}  # Map of tray_id to prep data
}

class Ingredient(BaseModel):
    name: str
    weight: float
    unit: str

class PrepBag(BaseModel):
    id: str
    dishName: str
    ingredients: List[Ingredient]
    addedIngredients: List[Ingredient]
    isComplete: bool

class Dish(BaseModel):
    id: str
    name: str
    prepBags: List[PrepBag]
    completedPrepBags: Optional[List[PrepBag]] = None
    ingredients: List[Ingredient]
    quantity: int
    colour: Optional[str] = None

class PrepTrackingData(BaseModel):
    dishes: List[Dish]
    selectedTrays: List[str]

@router.post("/receive-prep-data")
async def receive_prep_data_post(request: Request):
    """
    Receive prep data from the prep-tracker app via POST request.
    """
    try:
        # Log the raw request body for debugging
        body = await request.body()
        logger.info(f"Received raw request body: {body.decode()}")
        
        # Parse the JSON data
        data = await request.json()
        logger.info(f"Parsed JSON data: {json.dumps(data, indent=2)}")
        
        if not data.get("dishes") or not data.get("selectedTrays"):
            logger.warning("Missing required data in request")
            return {
                "message": "No data received",
                "completed_prep_bags": [],
                "selected_trays": SELECTED_TRAYS
            }

        # Update the selected tray IDs
        SELECTED_TRAYS.clear()
        SELECTED_TRAYS.extend(data["selectedTrays"])
        logger.info(f"Updated selected trays: {SELECTED_TRAYS}")
        
        # Store the prep data
        PREP_DATA["dishes"] = data["dishes"]
        
        # Process completed prep bags
        completed_prep_bags = []
        for dish in data["dishes"]:
            for prep_bag in dish.get("prepBags", []):
                if prep_bag.get("isComplete"):
                    completed_prep_bags.append({
                        "id": prep_bag["id"],
                        "name": f"{dish['name']} - {prep_bag['id']}",
                        "volume": calculate_volume(prep_bag),
                        "dish_name": dish["name"]
                    })
        
        # Process tray contents
        PREP_DATA["tray_contents"] = {}
        for dish in data["dishes"]:
            if "trayId" in dish and dish["trayId"]:
                tray_id = dish["trayId"]
                if tray_id not in PREP_DATA["tray_contents"]:
                    PREP_DATA["tray_contents"][tray_id] = {
                        "dishes": [],
                        "total_bags": 0,
                        "ingredients": {}
                    }
                
                # Add dish to tray
                PREP_DATA["tray_contents"][tray_id]["dishes"].append({
                    "id": dish["id"],
                    "name": dish["name"],
                    "quantity": dish.get("quantity", 1),
                    "prep_bags": [bag for bag in dish.get("prepBags", []) if bag.get("isComplete")]
                })
                
                # Count total bags
                PREP_DATA["tray_contents"][tray_id]["total_bags"] += len([bag for bag in dish.get("prepBags", []) if bag.get("isComplete")])
                
                # Aggregate ingredients
                for prep_bag in dish.get("prepBags", []):
                    if prep_bag.get("isComplete"):
                        for ingredient in prep_bag.get("ingredients", []):
                            name = ingredient.get("name", "Unknown")
                            if name not in PREP_DATA["tray_contents"][tray_id]["ingredients"]:
                                PREP_DATA["tray_contents"][tray_id]["ingredients"][name] = {
                                    "total_weight": 0,
                                    "unit": ingredient.get("unit", "g")
                                }
                            
                            weight = float(ingredient.get("weight", 0))
                            PREP_DATA["tray_contents"][tray_id]["ingredients"][name]["total_weight"] += weight
        
        logger.info(f"Processed tray contents: {json.dumps(PREP_DATA['tray_contents'], indent=2)}")
        
        return {
            "message": "Prep data received and processed successfully",
            "completed_prep_bags": completed_prep_bags,
            "selected_trays": SELECTED_TRAYS
        }
    except Exception as e:
        logger.error(f"Error processing prep data: {str(e)}")
        return {
            "message": f"Error processing prep data: {str(e)}",
            "completed_prep_bags": [],
            "selected_trays": SELECTED_TRAYS
        }

@router.get("/receive-prep-data")
async def receive_prep_data_get():
    """
    Get the current prep data state.
    """
    return {
        "message": "Current prep data state",
        "completed_prep_bags": [],
        "selected_trays": SELECTED_TRAYS
    }

def calculate_volume(prep_bag: Dict[str, Any]) -> float:
    """
    Calculate the approximate volume needed for a prep bag based on its ingredients.
    """
    total_volume = 0.0
    
    # Calculate volume for main ingredients
    for ingredient in prep_bag.get("ingredients", []):
        weight = float(ingredient.get("weight", 0))
        unit = ingredient.get("unit", "").lower()
        
        # Convert to liters based on unit
        if unit == "kg":
            total_volume += weight
        elif unit == "g":
            total_volume += weight / 1000
        elif unit == "l":
            total_volume += weight
        elif unit == "ml":
            total_volume += weight / 1000
    
    # Add some buffer for added ingredients
    for ingredient in prep_bag.get("addedIngredients", []):
        weight = float(ingredient.get("weight", 0))
        unit = ingredient.get("unit", "").lower()
        
        if unit == "kg":
            total_volume += weight
        elif unit == "g":
            total_volume += weight / 1000
        elif unit == "l":
            total_volume += weight
        elif unit == "ml":
            total_volume += weight / 1000
    
    # Add 20% buffer for mixing and handling
    return total_volume * 1.2

@router.get("/selected-trays")
async def get_selected_trays():
    """
    Get the list of currently selected trays.
    """
    return SELECTED_TRAYS 

async def get_prep_data_for_tray(tray_id: str) -> Dict[str, Any]:
    """
    Get prep data for a specific tray.
    """
    if tray_id not in PREP_DATA["tray_contents"]:
        return {
            "dishes": [],
            "total_bags": 0,
            "ingredients": {}
        }
    
    return PREP_DATA["tray_contents"][tray_id] 

@router.get("/recipes")
async def get_recipes():
    """
    Proxy request to get recipes from the recipe-upscaler backend.
    """
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{settings.RECIPE_UPSCALER_URL}/api/recipes")
            if response.status_code == 200:
                return response.json()
            else:
                logger.error(f"Failed to fetch recipes from recipe-upscaler: {response.status_code}")
                raise HTTPException(status_code=response.status_code, detail="Failed to fetch recipes")
    except Exception as e:
        logger.error(f"Error fetching recipes: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e)) 
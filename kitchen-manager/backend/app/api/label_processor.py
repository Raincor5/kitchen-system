from fastapi import APIRouter, UploadFile, File, HTTPException, BackgroundTasks
from typing import Dict, Any, List
import cv2
import numpy as np
import logging
import uuid
from datetime import datetime
from ..services.label_storage import LabelStorage
from ..services.image_processor import ImageProcessor
from ..services.text_parser import parse_label_text, find_closest_match
from ..services.db_client import DatabaseClient
import asyncio

router = APIRouter()
logger = logging.getLogger(__name__)

# Initialize services
label_storage = LabelStorage()
image_processor = ImageProcessor()
db_client = DatabaseClient()

@router.post("/process-image")
async def process_image(file: UploadFile = File(...)) -> List[Dict[str, Any]]:
    """Process an image to detect and extract label information."""
    try:
        # Record file info for debugging
        file_size = 0
        contents = await file.read()
        file_size = len(contents)
        
        logger.info(f"Received file: {file.filename}, size: {file_size} bytes, content type: {file.content_type}")
        
        # Check file size
        if file_size > 10 * 1024 * 1024:  # 10MB limit
            logger.warning(f"File too large: {file_size} bytes")
            raise HTTPException(status_code=400, detail="File too large (max 10MB)")
            
        # Check content type
        if file.content_type and not file.content_type.startswith('image/'):
            logger.warning(f"Invalid content type: {file.content_type}")
            raise HTTPException(status_code=400, detail=f"Invalid content type: {file.content_type}")
            
        # Convert bytes to image
        nparr = np.frombuffer(contents, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if image is None:
            logger.error("Failed to decode image")
            raise HTTPException(status_code=400, detail="Invalid image file")

        # Fetch products and employees for text parsing
        products = await db_client.get_products_from_api()
        employees = await db_client.get_employees_from_api()
        
        product_names = [product["name"] for product in products]
        employee_names = [employee["name"] for employee in employees]
        
        # Process the entire image - now using Roboflow detection
        logger.info(f"Processing image with dimensions: {image.shape}")
        
        # We'll pass a dummy bbox since the image processor will use Roboflow to detect labels
        dummy_bbox = {
            'x': image.shape[1] // 2,
            'y': image.shape[0] // 2,
            'width': image.shape[1],
            'height': image.shape[0]
        }
        
        result = image_processor.process_recipe_image(image, dummy_bbox)
        
        if "error" in result and result["error"]:
            logger.error(f"Error in image processing: {result['error']}")
            raise HTTPException(status_code=500, detail=result["error"])
        
        # Check if any text was extracted
        if not result["text"]:
            logger.warning("No text extracted from image")
            return []
            
        # Create a response for each detected label
        response = []
        
        if "all_results" in result and result["all_results"]:
            # Multiple labels were detected, return all with text
            for label_result in result["all_results"]:
                label_id = label_result["detection_id"]
                raw_text = label_result["text"]
                
                # Parse the extracted text using actual products and employees
                parsed_data = parse_label_text(raw_text, product_names, employee_names)
                
                label_data = {
                    "label_id": label_id,
                    "raw_text": raw_text,
                    "parsed_data": parsed_data,
                    "image_info": {
                        "confidence": label_result["confidence"],
                        "bbox": label_result["bbox"]
                    }
                }
                response.append(label_data)
        else:
            # Single label or fallback
            label_id = str(uuid.uuid4())
            raw_text = result["text"]
            
            # Parse the extracted text using actual products and employees
            parsed_data = parse_label_text(raw_text, product_names, employee_names)
            
            label_data = {
                "label_id": label_id,
                "raw_text": raw_text,
                "parsed_data": parsed_data,
                "image_info": result["processed_image"] 
            }
            response.append(label_data)
        
        extracted_chars = sum(len(label["raw_text"]) for label in response)
        logger.info(f"Successfully processed image, extracted {extracted_chars} characters of text")
        return response

    except HTTPException as e:
        # Re-raise HTTP exceptions
        logger.error(f"HTTP exception: {str(e.detail)}")
        raise
    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/save-label")
async def save_label(label_data: Dict[str, Any]) -> Dict[str, Any]:
    """Save a label to the database."""
    try:
        saved_label = db_client.save_label(label_data)
        return {"status": "success", "label": saved_label}
    except Exception as e:
        logger.error(f"Error saving label: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/get-labels")
async def get_labels() -> List[Dict[str, Any]]:
    """Get all saved labels."""
    try:
        return db_client.get_labels()
    except Exception as e:
        logger.error(f"Error getting labels: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/print-saved-label/{label_id}")
async def print_saved_label(label_id: str):
    """Print a saved label by its ID."""
    try:
        # Get the label from the database
        label = db_client.get_label_by_id(label_id)
        if not label:
            raise HTTPException(status_code=404, detail=f"Label with ID {label_id} not found")
        
        # Initialize printer service
        from ..services.label_printer import LabelPrinterService
        printer_service = LabelPrinterService()
        
        # Format product name and dates for printing
        product_name = label.get("parsed_data", {}).get("product_name", "Unknown Product")
        dates = label.get("parsed_data", {}).get("dates", [])
        employee_name = label.get("parsed_data", {}).get("employee_name", "Unknown Employee")
        
        # Get content for printing
        content = [
            f"Product: {product_name}",
            f"Employee: {employee_name}"
        ]
        
        # Add dates
        if dates:
            content.append("Dates:")
            for date in dates:
                content.append(f"- {date}")
        
        # Print content
        formatted_content = "\n".join(content)
        success = printer_service.print_text(formatted_content)
        
        return {
            "success": success, 
            "status": "printed" if success else "error",
            "label_id": label_id
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error printing saved label: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/delete-label")
async def delete_label(label_id: str) -> Dict[str, Any]:
    """Delete a label by ID."""
    try:
        success = db_client.delete_label(label_id)
        if not success:
            raise HTTPException(status_code=404, detail=f"Label with ID {label_id} not found")
        return {"status": "success"}
    except HTTPException as e:
        raise
    except Exception as e:
        logger.error(f"Error deleting label: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e)) 
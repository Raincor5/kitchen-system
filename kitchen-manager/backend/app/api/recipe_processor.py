from fastapi import APIRouter, UploadFile, File, HTTPException
from typing import Dict, Any
import cv2
import numpy as np
import logging
import os

# Try to import the image processor, but make it optional
try:
    from ..services.image_processor import ImageProcessor
    IMAGE_PROCESSOR_AVAILABLE = True
    image_processor = ImageProcessor()
except Exception as e:
    IMAGE_PROCESSOR_AVAILABLE = False
    logging.warning(f"Image processor not available: {str(e)}")
    logging.warning("Recipe image processing will be limited.")

router = APIRouter()

@router.post("/process-recipe")
async def process_recipe_image(file: UploadFile = File(...)) -> Dict[str, Any]:
    """
    Process a recipe image to extract text and structure it.
    """
    try:
        # Check if image processing is available
        if not IMAGE_PROCESSOR_AVAILABLE:
            return {
                "error": "Image processing is not available. Please install required dependencies.",
                "status": "error"
            }
            
        # Read the image file
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if image is None:
            raise HTTPException(status_code=400, detail="Invalid image file")

        # For now, we'll process the entire image
        # In the future, we could add object detection to find recipe regions
        bbox = {
            'x': 0,
            'y': 0,
            'width': image.shape[1],
            'height': image.shape[0]
        }

        # Process the image
        result = image_processor.process_recipe_image(image, bbox)
        
        return result

    except Exception as e:
        logging.error(f"Error processing recipe image: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
        
@router.get("/status")
async def get_processor_status() -> Dict[str, Any]:
    """
    Get the status of the image processor and available features.
    """
    status = {
        "image_processor_available": IMAGE_PROCESSOR_AVAILABLE,
        "features": {
            "perspective_correction": IMAGE_PROCESSOR_AVAILABLE,
            "text_extraction": IMAGE_PROCESSOR_AVAILABLE,
            "google_cloud_vision": False,
            "tesseract_ocr": False
        }
    }
    
    if IMAGE_PROCESSOR_AVAILABLE:
        # Check if Google Cloud Vision is available
        if hasattr(image_processor, 'vision_client') and image_processor.vision_client is not None:
            status["features"]["google_cloud_vision"] = True
            
        # Check if Tesseract OCR is available
        try:
            import pytesseract
            status["features"]["tesseract_ocr"] = True
        except ImportError:
            pass
            
    return status 
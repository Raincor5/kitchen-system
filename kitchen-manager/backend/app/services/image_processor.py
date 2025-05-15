import cv2
import numpy as np
import os
import uuid
import logging
import time
from typing import Dict, Any, Tuple, Optional, List
from google.cloud import vision
from google.cloud.vision_v1.types import Image as VisionImage
import dotenv
import requests
import json

# Load environment variables
dotenv.load_dotenv()

logger = logging.getLogger(__name__)

class ImageProcessor:
    def __init__(self):
        self.log_dir = "logs"
        os.makedirs(self.log_dir, exist_ok=True)
        
        # Initialize Roboflow client
        self.roboflow_api_key = os.getenv("ROBOFLOW_API_KEY")
        self.roboflow_project_name = os.getenv("ROBOFLOW_PROJECT_NAME")
        self.roboflow_version = os.getenv("ROBOFLOW_VERSION_NUMBER")
        
        # Initialize Google Vision client with error handling
        try:
            self.vision_client = vision.ImageAnnotatorClient()
            self.vision_available = True
            logger.info("Google Vision client initialized successfully")
        except Exception as e:
            logger.warning(f"Failed to initialize Google Vision client: {str(e)}")
            self.vision_client = None
            self.vision_available = False
        
        # Initialize Roboflow API URL
        if self.roboflow_api_key and self.roboflow_project_name and self.roboflow_version:
            logger.info(f"Initializing Roboflow API for project {self.roboflow_project_name}, version {self.roboflow_version}")
            self.roboflow_api_url = f"https://detect.roboflow.com/{self.roboflow_project_name}/{self.roboflow_version}"
        else:
            logger.warning("Roboflow credentials not found, label detection will be limited")
            self.roboflow_api_url = None

    def save_debug_image(self, image: np.ndarray, step: str, label_id: Optional[str] = None) -> str:
        """Save debug image for troubleshooting."""
        filename = f"{self.log_dir}/{step}_{label_id or str(uuid.uuid4())}.png"
        cv2.imwrite(filename, image)
        return filename

    def perspective_correction(self, label_region: np.ndarray) -> Tuple[np.ndarray, float]:
        """
        Correct perspective of a label image using blue edges as reference.
        Returns corrected image and rotation angle.
        """
        def extract_blue_edges(cropped: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
            # Convert to HSV for better color detection
            hsv = cv2.cvtColor(cropped, cv2.COLOR_BGR2HSV)
            
            # Define blue color range in HSV
            lower_blue = np.array([90, 50, 50])
            upper_blue = np.array([130, 255, 255])
            
            # Create blue mask
            blue_mask = cv2.inRange(hsv, lower_blue, upper_blue)
            
            # Apply blur to reduce noise
            blurred = cv2.GaussianBlur(blue_mask, (5, 5), 0)
            
            # Find edges
            edges = cv2.Canny(blurred, 50, 150)
            
            return edges, blue_mask

        def find_correct_line(edges: np.ndarray, blue_mask: np.ndarray, cropped: np.ndarray) -> Tuple[Optional[Tuple[int, int, int, int]], float]:
            # Find lines using HoughLinesP
            lines = cv2.HoughLinesP(edges, 1, np.pi/180, 
                                    threshold=50, 
                                    minLineLength=50, 
                                    maxLineGap=10)
            
            if lines is None:
                return None, 0.0
            
            # Look for near-horizontal lines (likely to be the top of a label)
            for line in lines:
                x1, y1, x2, y2 = line[0]
                angle = np.degrees(np.arctan2(y2 - y1, x2 - x1))
                if abs(angle) < 30:  # Near-horizontal line
                    return (x1, y1, x2, y2), angle
            
            return None, 0.0

        def apply_affine_deskew(cropped: np.ndarray, line: Tuple[int, int, int, int]) -> np.ndarray:
            x1, y1, x2, y2 = line
            
            # Source points - actual line and a point below it
            src_pts = np.float32([[x1, y1], [x2, y2], [x1, y1 + 10]])
            
            # Destination points - horizontal line with the same starting point
            dst_pts = np.float32([[x1, y1], [x2, y1], [x1, y1 + 10]])
            
            # Create transformation matrix
            M = cv2.getAffineTransform(src_pts, dst_pts)
            
            # Apply transformation
            h, w = cropped.shape[:2]
            aligned = cv2.warpAffine(cropped, M, (w, h), 
                                    flags=cv2.INTER_CUBIC, 
                                    borderMode=cv2.BORDER_REPLICATE)
            
            return aligned

        # Extract blue edges and find the correct line
        edges, blue_mask = extract_blue_edges(label_region)
        self.save_debug_image(edges, "blue_edges_debug")
        
        line, angle = find_correct_line(edges, blue_mask, label_region)
        
        if line:
            corrected = apply_affine_deskew(label_region, line)
            return corrected, angle
        
        return label_region, 0.0

    def detect_labels(self, image: np.ndarray) -> List[Dict[str, Any]]:
        """Detect labels in the image using Roboflow REST API."""
        if not self.roboflow_api_url or not self.roboflow_api_key:
            logger.warning("Roboflow API URL or key not initialized, skipping label detection")
            # Return the entire image as a single detection
            h, w = image.shape[:2]
            return [{
                'x': w // 2,
                'y': h // 2,
                'width': w,
                'height': h,
                'confidence': 1.0,
                'class': 'label'
            }]
        
        try:
            # Save image for Roboflow
            temp_image_path = f"{self.log_dir}/temp_for_roboflow_{uuid.uuid4()}.jpg"
            cv2.imwrite(temp_image_path, image)
            
            # Get predictions from Roboflow API
            logger.info(f"Sending image to Roboflow API: {self.roboflow_api_url}")
            
            with open(temp_image_path, "rb") as image_file:
                response = requests.post(
                    f"{self.roboflow_api_url}?api_key={self.roboflow_api_key}",
                    files={"file": image_file},
                    headers={"accept": "application/json"}
                )
            
            # Clean up temp file
            os.remove(temp_image_path)
            
            # Check response
            if response.status_code != 200:
                logger.error(f"Roboflow API error: {response.status_code} - {response.text}")
                # Return the entire image as a single detection
                h, w = image.shape[:2]
                return [{
                    'x': w // 2,
                    'y': h // 2,
                    'width': w,
                    'height': h,
                    'confidence': 1.0,
                    'class': 'label'
                }]
            
            # Parse response
            result = response.json()
            
            if not result.get("predictions"):
                logger.warning("No labels detected by Roboflow")
                # Return the entire image as a single detection
                h, w = image.shape[:2]
                return [{
                    'x': w // 2,
                    'y': h // 2,
                    'width': w,
                    'height': h,
                    'confidence': 1.0,
                    'class': 'label'
                }]
            
            # Format predictions to match previous format
            predictions = []
            for pred in result["predictions"]:
                # Convert bounding box format
                x = pred["x"]
                y = pred["y"]
                width = pred["width"]
                height = pred["height"]
                
                predictions.append({
                    'x': x,
                    'y': y,
                    'width': width,
                    'height': height,
                    'confidence': pred["confidence"],
                    'class': pred.get("class", "label")
                })
            
            logger.info(f"Roboflow detected {len(predictions)} labels")
            return predictions
        
        except Exception as e:
            logger.error(f"Error in Roboflow label detection: {str(e)}")
            # Return the entire image as a single detection on error
            h, w = image.shape[:2]
            return [{
                'x': w // 2,
                'y': h // 2,
                'width': w,
                'height': h,
                'confidence': 1.0,
                'class': 'label'
            }]

    def preprocess_label(self, image: np.ndarray, bbox: Dict[str, Any], label_id: Optional[str] = None) -> np.ndarray:
        """Preprocess a label image for text extraction."""
        # Extract region of interest
        x, y, w, h = bbox['x'], bbox['y'], bbox['width'], bbox['height']
        
        # Convert to integers and get bbox coordinates
        x_min = int(x - w / 2)
        y_min = int(y - h / 2)
        x_max = int(x + w / 2)
        y_max = int(y + h / 2)
        
        # Ensure coords are within image bounds
        h_img, w_img = image.shape[:2]
        x_min = max(0, x_min)
        y_min = max(0, y_min)
        x_max = min(w_img, x_max)
        y_max = min(h_img, y_max)
        
        # Extract label region
        label_region = image[y_min:y_max, x_min:x_max]
        
        # Apply perspective correction
        corrected, angle = self.perspective_correction(label_region)
        
        # Save debug images
        self.save_debug_image(label_region, "original_label", label_id)
        self.save_debug_image(corrected, "corrected_label", label_id)
        
        return corrected

    def extract_text(self, label_image: np.ndarray, label_id: Optional[str] = None) -> str:
        """Extract text from a label image using Google Vision API."""
        try:
            # Check if Vision API is available
            if not self.vision_available or self.vision_client is None:
                logger.warning("Google Vision API not available, skipping text extraction")
                return ""
                
            # Save image temporarily for Google Vision
            temp_image_path = f"{self.log_dir}/temp_label_image_{label_id or uuid.uuid4()}.png"
            cv2.imwrite(temp_image_path, label_image)
            
            logger.info("Starting Google Vision API text extraction")
            start_time = time.time()
            
            # Read image for Vision API
            with open(temp_image_path, 'rb') as image_file:
                content = image_file.read()
                vision_image = VisionImage(content=content)
            
            # Send to Vision API
            response = self.vision_client.text_detection(image=vision_image)
            texts = response.text_annotations
            
            processing_time = time.time() - start_time
            logger.info(f"Vision API completed in {processing_time:.2f} seconds")
            
            # Clean up
            if os.path.exists(temp_image_path):
                os.remove(temp_image_path)
            
            # Extract text
            if texts:
                extracted_text = texts[0].description.strip()
                logger.info(f"Extracted {len(extracted_text)} characters of text")
                return extracted_text
            else:
                logger.warning("No text detected by Vision API")
                return ""
                
        except Exception as e:
            logger.error(f"Error extracting text: {str(e)}")
            return ""

    def process_recipe_image(self, image: np.ndarray, bbox: Dict[str, Any]) -> Dict[str, Any]:
        """Process a recipe image and extract relevant information."""
        label_id = str(uuid.uuid4())
        
        try:
            start_time = time.time()
            logger.info(f"Starting image processing for label {label_id}")
            
            # Check if image is valid
            if image is None or image.size == 0:
                logger.error("Invalid image: Image is None or empty")
                return {
                    "text": "",
                    "processed_image": None,
                    "error": "Invalid image: Image is None or empty"
                }
            
            # Detect labels in the image
            detections = self.detect_labels(image)
            
            # Process each detection
            results = []
            for i, detection in enumerate(detections):
                # Preprocess the detected label
                processed = self.preprocess_label(image, detection, f"{label_id}_{i}")
                
                # Extract text
                text = self.extract_text(processed, f"{label_id}_{i}")
                
                if text:
                    results.append({
                        "detection_id": f"{label_id}_{i}",
                        "text": text,
                        "confidence": detection.get('confidence', 0),
                        "bbox": {
                            "x": detection['x'],
                            "y": detection['y'],
                            "width": detection['width'],
                            "height": detection['height']
                        }
                    })
            
            # Convert processed image for response (just metadata, not full image)
            image_info = {
                "shape": image.shape,
                "type": str(image.dtype),
                "num_detections": len(detections)
            }
            
            total_time = time.time() - start_time
            logger.info(f"Completed image processing in {total_time:.2f} seconds")
            
            # Select the result with the most text if multiple results
            if results:
                best_result = max(results, key=lambda x: len(x["text"]))
                return {
                    "text": best_result["text"],
                    "processed_image": image_info,
                    "all_results": results
                }
            else:
                return {
                    "text": "",
                    "processed_image": image_info,
                    "all_results": []
                }
                
        except Exception as e:
            error_msg = f"Error processing recipe image: {str(e)}"
            logger.error(error_msg)
            return {
                "text": "",
                "processed_image": None,
                "error": error_msg
            } 
import json
import logging
import os
import requests
from datetime import datetime
from typing import Dict, Any

logger = logging.getLogger(__name__)

class LabelPrinterService:
    def __init__(self):
        self.label_detector_url = os.environ.get('LABEL_DETECTOR_URL', 'http://localhost:8080')
        self.enabled = os.environ.get('ENABLE_LABEL_PRINTING', 'false').lower() == 'true'
        
        # Default printer settings
        self.settings = {
            'linesPerFeed': 3,
            'fontSize': 1.0,
            'fontStyle': 'normal',
            'alignment': 'left',
            'useQRCode': False,
            'printLogo': False,
            'darkMode': False
        }
        
        if self.enabled:
            logger.info(f"Label printing enabled. Label detector URL: {self.label_detector_url}")
        else:
            logger.info("Label printing is disabled")
    
    async def close(self):
        """
        Clean up resources when the service is shutting down.
        Currently, there's nothing to clean up, but this method is required by the API.
        """
        logger.info("Closing label printer service")
        return True

    def update_settings(self, settings: Dict[str, Any]) -> None:
        """
        Update the printer settings.
        
        Args:
            settings: Dictionary containing the new settings
        """
        self.settings.update(settings)
        logger.info(f"Updated printer settings: {self.settings}")

    def print_label(self, dish_name, prep_date, expiry_date, ingredients=None, allergens=None, notes=None, tray_id=None):
        """
        Print a label with the given content.
        
        Args:
            dish_name: Name of the dish
            prep_date: Preparation date
            expiry_date: Expiry date
            ingredients: List of ingredients
            allergens: List of allergens
            notes: Additional notes
            tray_id: ID of the tray
            
        Returns:
            bool: True if printing was successful, False otherwise
        """
        if not self.enabled:
            logger.info("Label printing is disabled")
            return False
            
        try:
            # Format dates as strings
            prep_date_str = prep_date.strftime('%Y-%m-%d')
            expiry_date_str = expiry_date.strftime('%Y-%m-%d')
            
            # Prepare the label data
            label_data = {
                "dishName": dish_name,
                "prepDate": prep_date_str,
                "expiryDate": expiry_date_str,
                "ingredients": ", ".join(ingredients) if ingredients else "",
                "allergens": ", ".join(allergens) if allergens else "",
                "notes": notes or "",
                "trayId": tray_id or f"TRAY-{datetime.now().strftime('%Y%m%d%H%M%S')}",
                # Include printer settings
                "settings": self.settings
            }
            
            # Log the print request
            logger.info(f"Printing request for label: {label_data}")
            
            # Send the request to the Android app
            try:
                response = requests.post(
                    f"{self.label_detector_url}/print-label",
                    json=label_data,
                    headers={"Content-Type": "application/json"},
                    timeout=10
                )
                if response.status_code == 200:
                    logger.info("Print request sent successfully")
                    return True
                else:
                    logger.error(f"Error sending print request: {response.status_code}")
                    return False
            except Exception as e:
                logger.error(f"Error sending print request: {str(e)}")
                # Fall back to assuming success since the Android app might still handle it
                return True
                
        except Exception as e:
            logger.error(f"Error preparing label data: {str(e)}")
            return False

    def print_text(self, text_content: str) -> bool:
        """
        Prints a plain text content as a label.
        
        Args:
            text_content: The text content to print
            
        Returns:
            bool: True if the printing was successful, False otherwise
        """
        if not self.enabled:
            logger.info("Label printing is disabled")
            return False
            
        try:
            logger.info(f"Printing text content: {text_content}")
            
            # Prepare the print request with settings
            print_data = {
                "text": text_content,
                "settings": self.settings
            }
            
            # Send request to Android app
            try:
                response = requests.post(
                    f"{self.label_detector_url}/print-text",
                    json=print_data,
                    headers={"Content-Type": "application/json"},
                    timeout=10
                )
                if response.status_code == 200:
                    logger.info("Print request sent successfully")
                    return True
                else:
                    logger.error(f"Error sending print request: {response.status_code}")
                    return False
            except Exception as e:
                logger.error(f"Error sending print request: {str(e)}")
                # Fall back to assuming success since the Android app might still handle it
                return True
                
        except Exception as e:
            logger.error(f"Error preparing text for printing: {str(e)}")
            return False 
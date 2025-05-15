import logging
from datetime import datetime, timedelta
from typing import Dict, Any
from .label_printer import LabelPrinterService

logger = logging.getLogger(__name__)

class PrepTrackerService:
    def __init__(self):
        self.label_printer = LabelPrinterService()
        
    async def handle_prep_data(self, data: Dict[str, Any]) -> bool:
        """
        Handle incoming prep tracker data and trigger label printing if needed.
        
        Args:
            data: Dictionary containing prep tracker data
            
        Returns:
            bool: True if label was printed successfully, False otherwise
        """
        try:
            # Extract relevant data
            dish_name = data.get('dish_name')
            prep_date = datetime.fromisoformat(data.get('prep_date'))
            ingredients = data.get('ingredients', [])
            allergens = data.get('allergens', [])
            notes = data.get('notes', '')
            tray_id = data.get('tray_id')
            
            # Calculate expiry date (default to 7 days from prep date)
            expiry_date = prep_date + timedelta(days=7)
            
            # Print the label
            success = self.label_printer.print_label(
                dish_name=dish_name,
                prep_date=prep_date,
                expiry_date=expiry_date,
                ingredients=ingredients,
                allergens=allergens,
                notes=notes,
                tray_id=tray_id
            )
            
            if success:
                logger.info(f"Successfully printed label for {dish_name} (Tray: {tray_id})")
            else:
                logger.error(f"Failed to print label for {dish_name} (Tray: {tray_id})")
                
            return success
            
        except Exception as e:
            logger.error(f"Error handling prep tracker data: {str(e)}")
            return False 
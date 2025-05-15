import uuid
from datetime import datetime, timedelta
from typing import List, Dict, Any
import httpx
from ..core.config import settings
from ..api.prep_tracking import get_prep_data_for_tray

class LabelGenerator:
    def __init__(self):
        self.client = None

    async def close(self):
        if self.client:
            await self.client.aclose()

    def generate_labels(self, labels: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Generate labels with unique IDs and timestamps.
        """
        try:
            print(f"Generating labels for {len(labels)} items")
            generated_labels = []
            for i, label_data in enumerate(labels):
                print(f"Processing label {i+1}: {label_data}")
                
                # Ensure all required fields are present
                if not isinstance(label_data, dict):
                    print(f"Warning: label_data is not a dict: {type(label_data)}")
                    label_data = dict(label_data)
                
                # Create a new label with required fields
                label = {
                    "tray_id": label_data.get("tray_id", "unknown"),
                    "dish_name": label_data.get("dish_name", "Unknown"),
                    "prep_date": label_data.get("prep_date", datetime.now().strftime("%Y-%m-%d")),
                    "expiry_date": label_data.get("expiry_date", (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d")),
                    "ingredients": label_data.get("ingredients", []),
                    "allergens": label_data.get("allergens", []),
                    "notes": label_data.get("notes", ""),
                    "id": str(uuid.uuid4()),
                    "created_at": datetime.now().isoformat()
                }
                
                # Add any additional fields from the original data
                for key, value in label_data.items():
                    if key not in label:
                        label[key] = value
                
                generated_labels.append(label)
            return generated_labels
        except Exception as e:
            import traceback
            print(f"Error in generate_labels: {str(e)}")
            print(traceback.format_exc())
            raise

    async def generate_enhanced_label(self, tray_id: str) -> Dict[str, Any]:
        """
        Generate an enhanced label with data from other applications.
        """
        # Get tray data from the local gastronorm API
        if not self.client:
            self.client = httpx.AsyncClient()

        try:
            # Get tray data from local API
            response = await self.client.get(f"http://localhost:8080/api/gastronorm/{tray_id}")
            response.raise_for_status()
            tray_data = response.json()
            
            # Get tray contents
            prep_data = await get_prep_data_for_tray(tray_id)
            
            # Format ingredients list
            ingredients_list = []
            for ingredient_name, ingredient_data in prep_data.get("ingredients", {}).items():
                weight = ingredient_data.get("total_weight", 0)
                unit = ingredient_data.get("unit", "g")
                ingredients_list.append(f"{ingredient_name}: {weight} {unit}")
            
            # Format dishes list
            dishes_list = []
            for dish in prep_data.get("dishes", []):
                dish_name = dish.get("name", "Unknown")
                quantity = dish.get("quantity", 1)
                dishes_list.append(f"{dish_name} (x{quantity})")
            
            # Create an enhanced label with tray data
            label = {
                "tray_id": tray_id,
                "dish_name": ", ".join(dishes_list) if dishes_list else "Unknown",
                "prep_date": datetime.now().strftime("%Y-%m-%d"),
                "expiry_date": (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d"),
                "ingredients": ingredients_list,
                "allergens": [],  # This would need to be populated from allergen data
                "notes": f"Total prep bags: {prep_data.get('total_bags', 0)}",
                "tray_details": {
                    "name": tray_data.get("name", "Unknown"),
                    "size": tray_data.get("size", "Unknown"),
                    "material": tray_data.get("material", "Unknown"),
                    "volume": f"{tray_data.get('volumeLiters', 0)} L"
                }
            }

            return label
        except httpx.HTTPError as e:
            raise Exception(f"Error fetching tray data: {str(e)}")

    async def generate_enhanced_batch_labels(self, tray_ids: List[str]) -> List[Dict[str, Any]]:
        """
        Generate enhanced labels for multiple trays.
        """
        labels = []
        for tray_id in tray_ids:
            try:
                label = await self.generate_enhanced_label(tray_id)
                labels.append(label)
            except Exception as e:
                # Log the error but continue with other trays
                print(f"Error generating label for tray {tray_id}: {str(e)}")
        return labels

    def format_for_printer(self, label: Dict[str, Any]) -> Dict[str, Any]:
        """
        Format a label for printing.
        """
        content = [
            f"Tray: {label.get('tray_details', {}).get('name', 'Unknown')}",
            f"Size: {label.get('tray_details', {}).get('size', 'Unknown')}",
            f"Material: {label.get('tray_details', {}).get('material', 'Unknown')}",
            f"Volume: {label.get('tray_details', {}).get('volume', 'Unknown')}",
            "",
            f"Dishes: {label['dish_name']}",
            f"Prep Date: {label['prep_date']}",
            f"Expiry Date: {label['expiry_date']}",
            "",
            "Ingredients:"
        ]
        
        # Add ingredients
        for ingredient in label["ingredients"]:
            content.append(f"- {ingredient}")
        
        # Add allergens if any
        if label.get("allergens"):
            content.append("")
            content.append("Allergens:")
            for allergen in label["allergens"]:
                content.append(f"- {allergen}")
        
        # Add notes
        if label.get("notes"):
            content.append("")
            content.append(f"Notes: {label['notes']}")
        
        return {
            "tray_id": label["tray_id"],
            "content": content
        } 
import asyncio
import aiohttp
import json
from datetime import datetime, timedelta

async def test_label_printing():
    # Test data
    test_label = {
        "dish_name": "Test Dish",
        "prep_date": datetime.now().strftime("%Y-%m-%d"),
        "expiry_date": (datetime.now() + timedelta(days=7)).strftime("%Y-%m-%d"),
        "ingredients": ["Ingredient 1", "Ingredient 2"],
        "allergens": ["Allergen 1"],
        "notes": "Test notes",
        "tray_id": "TEST-001"
    }
    
    # Label detector app URL (update this with your actual URL)
    label_detector_url = "http://localhost:8080/print"
    
    try:
        async with aiohttp.ClientSession() as session:
            # Send print request
            async with session.post(
                label_detector_url,
                json=test_label,
                headers={"Content-Type": "application/json"}
            ) as response:
                if response.status == 200:
                    result = await response.json()
                    print("Print request successful!")
                    print(f"Response: {json.dumps(result, indent=2)}")
                else:
                    print(f"Error: {response.status}")
                    print(await response.text())
    except Exception as e:
        print(f"Error sending print request: {str(e)}")

if __name__ == "__main__":
    asyncio.run(test_label_printing()) 
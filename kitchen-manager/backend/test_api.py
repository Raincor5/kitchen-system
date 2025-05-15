import requests
import json
from app.core.config import settings

def test_gastronorm_api():
    """Test the gastronorm API endpoints."""
    try:
        # Test list endpoint
        print("\nTesting /api/gastronorm/list endpoint:")
        response = requests.get("http://localhost:8000/api/gastronorm/list")
        print(f"Status code: {response.status_code}")
        if response.status_code == 200:
            trays = response.json()
            print(f"Found {len(trays)} trays")
            if trays:
                print("First tray:", json.dumps(trays[0], indent=2))
        else:
            print(f"Error: {response.text}")

        # Test single tray endpoint
        if response.status_code == 200 and trays:
            print("\nTesting /api/gastronorm/{tray_id} endpoint:")
            tray_id = trays[0]["id"]
            response = requests.get(f"http://localhost:8000/api/gastronorm/{tray_id}")
            print(f"Status code: {response.status_code}")
            if response.status_code == 200:
                print("Tray details:", json.dumps(response.json(), indent=2))
            else:
                print(f"Error: {response.text}")

    except Exception as e:
        print(f"Error: {str(e)}")

if __name__ == "__main__":
    test_gastronorm_api() 
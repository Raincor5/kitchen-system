import sys
from pathlib import Path
import json

# Path to the GN.json file in the prep tracking app
GN_JSON_PATH = "/home/mckrotsky/projects/prep-tracker/prep-tracking-app/assets/documents/GN.json"

def verify_gastronorm_trays():
    """
    Verify that the gastronorm trays data is correctly loaded from the JSON file.
    """
    try:
        # Load the JSON file
        with open(GN_JSON_PATH, 'r') as f:
            trays = json.load(f)
        
        print(f"Found {len(trays)} gastronorm trays in the JSON file.")
        
        # Print the first 5 trays
        print("\nFirst 5 gastronorm trays:")
        for i, tray in enumerate(trays[:5]):
            print(f"{i+1}. {tray['name']} (Size: {tray['size']}, Material: {tray['material']})")
            print(f"   Dimensions: {tray['dimensions']}")
            print(f"   Volume: {tray['volume_liters']} liters")
            print(f"   Description: {tray['description']}")
            print()
        
        # Print some statistics
        sizes = set(tray['size'] for tray in trays)
        materials = set(tray['material'] for tray in trays)
        
        print(f"Unique sizes: {sorted(sizes)}")
        print(f"Unique materials: {sorted(materials)}")
        
    except FileNotFoundError:
        print(f"Error: Could not find the gastronorm trays data file at {GN_JSON_PATH}")
    except json.JSONDecodeError:
        print("Error: The gastronorm trays data file is not valid JSON")
    except Exception as e:
        print(f"Error: {str(e)}")

if __name__ == "__main__":
    print("Verifying gastronorm trays data...")
    verify_gastronorm_trays()
    print("Verification complete!") 
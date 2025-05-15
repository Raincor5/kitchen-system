import logging
from typing import Dict, Any, List
import json
import os
from datetime import datetime

logger = logging.getLogger(__name__)

class LabelStorage:
    def __init__(self):
        self.storage_dir = "data/labels"
        self.storage_file = os.path.join(self.storage_dir, "labels.json")
        os.makedirs(self.storage_dir, exist_ok=True)
        self._ensure_storage_file()

    def _ensure_storage_file(self):
        """Ensure the storage file exists and is valid JSON."""
        if not os.path.exists(self.storage_file):
            with open(self.storage_file, 'w') as f:
                json.dump([], f)

    def _load_labels(self) -> List[Dict[str, Any]]:
        """Load all labels from storage."""
        try:
            with open(self.storage_file, 'r') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Error loading labels: {str(e)}")
            return []

    def _save_labels(self, labels: List[Dict[str, Any]]):
        """Save labels to storage."""
        try:
            with open(self.storage_file, 'w') as f:
                json.dump(labels, f, indent=2)
        except Exception as e:
            logger.error(f"Error saving labels: {str(e)}")
            raise

    def save_label(self, label_data: Dict[str, Any]) -> Dict[str, Any]:
        """Save a new label or update an existing one."""
        labels = self._load_labels()
        
        # Add timestamp if not present
        if "timestamp" not in label_data:
            label_data["timestamp"] = datetime.now().isoformat()
        
        # Check if label exists
        existing_index = next(
            (i for i, label in enumerate(labels) if label["label_id"] == label_data["label_id"]),
            -1
        )
        
        if existing_index >= 0:
            # Update existing label
            labels[existing_index] = label_data
        else:
            # Add new label
            labels.append(label_data)
        
        self._save_labels(labels)
        return label_data

    def get_labels(self) -> List[Dict[str, Any]]:
        """Get all saved labels."""
        return self._load_labels()

    def get_label(self, label_id: str) -> Dict[str, Any]:
        """Get a specific label by ID."""
        labels = self._load_labels()
        return next(
            (label for label in labels if label["label_id"] == label_id),
            None
        )

    def delete_label(self, label_id: str):
        """Delete a label by ID."""
        labels = self._load_labels()
        labels = [label for label in labels if label["label_id"] != label_id]
        self._save_labels(labels) 
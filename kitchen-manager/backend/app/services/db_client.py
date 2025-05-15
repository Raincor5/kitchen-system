import os
import logging
from pymongo import MongoClient
import certifi
from typing import Dict, Any, List, Optional
import httpx
from datetime import datetime
from pymongo.errors import ServerSelectionTimeoutError

logger = logging.getLogger(__name__)

class DatabaseClient:
    def __init__(self):
        # Get MongoDB URI from environment variables
        self.mongo_uri = os.getenv("MONGO_URI")
        
        if not self.mongo_uri:
            logger.error("MONGO_URI environment variable not found. MongoDB features will not work.")
            self.client = None
            self.db = None
            self.labels_collection = None
            return
        
        # Initialize client
        try:
            self.client = MongoClient(
                self.mongo_uri,
                tls=True,
                tlsCAFile=certifi.where()
            )
            logger.info("MongoDB client initialized successfully")
            
            # Test connection
            self.client.admin.command('ping')
            logger.info("Successfully connected to MongoDB")
            
            # Set database name - same as FTT backend
            self.db = self.client['ftt_mongo']
            self.labels_collection = self.db['labelData']
            
        except Exception as e:
            logger.error(f"Failed to initialize MongoDB client: {str(e)}")
            self.client = None
            self.db = None
            self.labels_collection = None
    
    async def get_products_from_api(self) -> List[Dict[str, Any]]:
        """Get product list from FTT backend API."""
        try:
            # Fetch from FTT backend or a separate database
            async with httpx.AsyncClient() as client:
                response = await client.get(f"{os.getenv('LABEL_DETECTOR_URL')}/products")
                if response.status_code == 200:
                    return response.json()
                logger.error(f"Failed to get products from API: {response.status_code}")
                return []
        except Exception as e:
            logger.error(f"Error fetching products from API: {str(e)}")
            return []
    
    async def get_employees_from_api(self) -> List[Dict[str, Any]]:
        """Get employee list from FTT backend API."""
        try:
            # Fetch from FTT backend or a separate database
            async with httpx.AsyncClient() as client:
                response = await client.get(f"{os.getenv('LABEL_DETECTOR_URL')}/employees")
                if response.status_code == 200:
                    return response.json()
                logger.error(f"Failed to get employees from API: {response.status_code}")
                return []
        except Exception as e:
            logger.error(f"Error fetching employees from API: {str(e)}")
            return []
    
    def save_label(self, label_data: Dict[str, Any]) -> Dict[str, Any]:
        """Save label to MongoDB."""
        if self.labels_collection is None:
            logger.error("MongoDB client not initialized")
            return label_data
            
        try:
            # Add timestamp if not present
            if "timestamp" not in label_data.get("parsed_data", {}):
                if "parsed_data" not in label_data:
                    label_data["parsed_data"] = {}
                label_data["parsed_data"]["uploadTimestamp"] = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
            
            # Check if label exists
            existing = self.labels_collection.find_one({"label_id": label_data["label_id"]})
            
            if existing:
                # Update existing document
                self.labels_collection.update_one(
                    {"label_id": label_data["label_id"]},
                    {"$set": label_data}
                )
                logger.info(f"Updated label with ID: {label_data['label_id']}")
            else:
                # Insert new document
                result = self.labels_collection.insert_one(label_data)
                logger.info(f"Inserted new label with ID: {label_data['label_id']}")
                
                # Make sure we're not returning any ObjectId values
                if "_id" in label_data:
                    del label_data["_id"]
                
            return label_data
            
        except Exception as e:
            logger.error(f"Error saving label to MongoDB: {str(e)}")
            return label_data
    
    def get_labels(self) -> List[Dict[str, Any]]:
        """Get all labels from MongoDB."""
        if self.labels_collection is None:
            logger.error("MongoDB client not initialized")
            return []
            
        try:
            # Exclude MongoDB _id field
            labels = list(self.labels_collection.find({}, {"_id": 0}))
            logger.info(f"Retrieved {len(labels)} labels from MongoDB")
            return labels
            
        except Exception as e:
            logger.error(f"Error getting labels from MongoDB: {str(e)}")
            return []
    
    def get_label_by_id(self, label_id: str) -> Optional[Dict[str, Any]]:
        """Get a specific label by its ID from MongoDB."""
        if self.labels_collection is None:
            logger.error("MongoDB client not initialized")
            return None
            
        try:
            # Find the label by ID and exclude MongoDB _id field
            label = self.labels_collection.find_one({"label_id": label_id}, {"_id": 0})
            if label:
                logger.info(f"Retrieved label with ID {label_id} from MongoDB")
                return label
            else:
                logger.warning(f"No label found with ID {label_id}")
                return None
                
        except Exception as e:
            logger.error(f"Error getting label from MongoDB: {str(e)}")
            return None
    
    def delete_label(self, label_id: str) -> bool:
        """Delete a label by ID."""
        if self.labels_collection is None:
            logger.error("MongoDB client not initialized")
            return False
            
        try:
            result = self.labels_collection.delete_one({"label_id": label_id})
            success = result.deleted_count > 0
            
            if success:
                logger.info(f"Deleted label with ID: {label_id}")
            else:
                logger.warning(f"No label found with ID: {label_id}")
                
            return success
            
        except Exception as e:
            logger.error(f"Error deleting label from MongoDB: {str(e)}")
            return False 
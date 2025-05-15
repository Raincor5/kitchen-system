from pydantic_settings import BaseSettings
from typing import Optional
import os

class Settings(BaseSettings):
    PROJECT_NAME: str = "Kitchen Manager"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # External Services
    PREP_TRACKER_URL: str = os.getenv("PREP_TRACKER_URL", "http://localhost:8083")
    RECIPE_UPSCALER_URL: str = os.getenv("RECIPE_UPSCALER_URL", "http://localhost:5000")

    class Config:
        case_sensitive = True

settings = Settings() 
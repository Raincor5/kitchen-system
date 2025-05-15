from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging
from dotenv import load_dotenv
import os

# Load environment variables from .env file
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Kitchen Manager Label API",
    description="API for managing kitchen labels and tray tracking",
    version="1.0.0"
)

# Configure CORS to allow connections from other applications
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",  # Frontend development server
        "http://localhost:8080",  # Backend development server
        "https://*.ngrok.app",    # Ngrok URLs
        "http://localhost:8083",  # Android app development
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    return {"message": "Kitchen Manager Label API"}

@app.get("/health")
async def health():
    return {"status": "healthy"}

# Import and include routers
from app.api import labels, gastronorm, printer, prep_tracking, recipe_processor, prep_tracker, label_processor, android
app.include_router(labels.router, prefix="/api/labels", tags=["labels"])
app.include_router(gastronorm.router, prefix="/api/gastronorm", tags=["gastronorm"])
app.include_router(printer.router, prefix="/api/printer", tags=["printer"])
app.include_router(prep_tracking.router, prefix="/api/prep-tracking", tags=["prep-tracking"])
app.include_router(recipe_processor.router, prefix="/api/recipe", tags=["recipe"])
app.include_router(prep_tracker.router, prefix="/api/prep-tracker", tags=["prep-tracker"])
app.include_router(label_processor.router, prefix="/api/label-processor", tags=["label-processor"])
app.include_router(android.router, prefix="/api/android", tags=["android"])

# Startup event
@app.on_event("startup")
async def startup_event():
    logger.info("Starting Kitchen Manager Label API")
    logger.info("Connected to external services: Prep Tracker, Recipe Upscaler, Kitchen Manager")
    logger.info("Label processor service initialized")

# Shutdown event
@app.on_event("shutdown")
async def shutdown_event():
    logger.info("Shutting down Kitchen Manager Label API") 
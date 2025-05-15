// This file contains the API endpoints for the application.
import Constants from 'expo-constants';
import { API_BASE_URL } from '../app/config';

// Get the API URL from environment variables or app.json
/***
const API_BASE_URL_FROM_ENV = process.env.API_URL || Constants.expoConfig?.extra?.API_URL || 'http://localhost:5000/api';
***/
const API_BASE_URL_FROM_ENV = "https://13efbed379db.ngrok.app/api"

const apiEndpoints = {
  recipes: `${API_BASE_URL}/prep-tracking/recipes`,
  gastronorm: `${API_BASE_URL}/gastronorm`,
  prepTracking: `${API_BASE_URL}/prep-tracking`,
  scale: `${API_BASE_URL_FROM_ENV}/scale`,
  processImage: `${API_BASE_URL_FROM_ENV}/process-image`,
};

export default apiEndpoints;

import Constants from 'expo-constants';

// Debug: Log environment variables
console.log('Environment variables:', {
  API_URL: Constants.expoConfig?.extra?.API_URL,
});

// Get the API URL from environment variables or app.json
const getApiUrl = () => {
  // First try to get from .env
  const envApiUrl = process.env.API_URL;
  if (envApiUrl) return envApiUrl;

  // Then try to get from app.json
  const appJsonApiUrl = Constants.expoConfig?.extra?.API_URL;
  if (appJsonApiUrl) return appJsonApiUrl;

  // Fallback to localhost
  return 'http://localhost:8083/api';
};

export const API_BASE_URL = getApiUrl();

// Debug: Log the final API URL
console.log('Using API URL:', API_BASE_URL); 
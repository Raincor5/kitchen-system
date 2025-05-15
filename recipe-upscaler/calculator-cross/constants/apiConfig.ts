import Constants from 'expo-constants';

// Get the API URL from environment variables
const API_BASE_URL = Constants.expoConfig?.extra?.API_URL || 'http://localhost:5000';

const apiEndpoints = {
  recipes: `${API_BASE_URL}/api/recipes`,
  scale: `${API_BASE_URL}/api/scale`,
  processImage: `${API_BASE_URL}/api/process-image`,
};

export default apiEndpoints;

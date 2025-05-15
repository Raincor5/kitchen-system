import { API_BASE_URL } from '../config';
import gnPans from '@/assets/documents/GN.json';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { PrepBag as PrepBagWithComplete } from '@/types/prepBags';
import { Dish } from '../types/dish';
import { Tray } from '../types/tray';

interface Ingredient {
  name: string;
  weight?: number;
  unit?: string;
}

// Helper function to log data
function logExportData(dishes: Dish[], selectedTrays: any) {
  console.log('=== Exporting Data to Kitchen Manager ===');
  console.log(`Timestamp: ${new Date().toISOString()}`);
  console.log(`Number of dishes: ${dishes.length}`);
  
  // Convert selectedTrays to string[] if it's Tray[]
  let trayIds: string[] = [];
  if (Array.isArray(selectedTrays)) {
    if (selectedTrays.length > 0 && typeof selectedTrays[0] === 'string') {
      trayIds = selectedTrays;
    } else {
      trayIds = selectedTrays.map((tray: any) => tray.id);
    }
  }
  
  console.log(`Selected trays: ${trayIds.join(', ')}`);
  
  dishes.forEach(dish => {
    console.log(`\nDish: ${dish.name} (ID: ${dish.id})`);
    console.log(`  Quantity: ${dish.quantity}`);
    console.log(`  Prep bags: ${dish.prepBags.length}`);
    console.log(`  Completed prep bags: ${dish.completedPrepBags?.length || 0}`);
    
    const totalIngredients = dish.prepBags.reduce((sum: number, bag: any) => 
      sum + bag.ingredients.length + bag.addedIngredients.length, 0);
    console.log(`  Total ingredients: ${totalIngredients}`);
  });
}

// Helper function to save export data locally
const saveExportDataLocally = async (exportData: any) => {
  try {
    const backupData = {
      timestamp: new Date().toISOString(),
      data: exportData,
    };
    await AsyncStorage.setItem('prep_data_backup', JSON.stringify(backupData));
    console.log('Data saved locally as backup');
  } catch (error) {
    console.error('Failed to save backup:', error);
    throw error;
  }
};

export const exportToKitchenManager = async (dishes: Dish[], trays: Tray[] | string[]) => {
  try {
    // First, check if any dishes have completed prep bags
    const dishesWithCompletedPrepBags = dishes.filter(dish => 
      dish.completedPrepBags && dish.completedPrepBags.length > 0
    );
    
    // If no dishes have completedPrepBags, check for prep bags marked as complete
    let dishesToExport = dishesWithCompletedPrepBags;
    if (dishesToExport.length === 0) {
      console.log('No dishes with completedPrepBags found, checking for prep bags marked as complete');
      
      // Find dishes with prep bags marked as complete
      dishesToExport = dishes.filter(dish => 
        dish.prepBags.some((bag: any) => (bag as any).isComplete)
      );
      
      if (dishesToExport.length === 0) {
        throw new Error('No dishes with completed prep bags found');
      }
      
      console.log(`Found ${dishesToExport.length} dishes with prep bags marked as complete`);
    }
    
    // Get unique tray IDs from dishes with completed prep bags
    const usedTrayIds = [...new Set(dishesToExport
      .map(dish => dish.trayId)
      .filter(id => id !== null && id !== undefined))];
    
    if (usedTrayIds.length === 0) {
      throw new Error('No trays assigned to dishes with completed prep bags');
    }

    // Determine which tray IDs to use for export
    let usedTrayIdsToExport: string[] = [];
    
    // Check if trays is a string array (tray IDs)
    if (Array.isArray(trays) && trays.length > 0 && typeof trays[0] === 'string') {
      // trays is already a string array, use it directly
      usedTrayIdsToExport = trays as string[];
    } else {
      // Filter trays to only include those that are actually used
      const usedTrays = (trays as Tray[]).filter(tray => usedTrayIds.includes(tray.id));
      usedTrayIdsToExport = usedTrays.map(tray => tray.id);
    }
    
    // Log the export data
    console.log('=== Exporting Data to Kitchen Manager ===');
    console.log(`Timestamp: ${new Date().toISOString()}`);
    console.log(`Number of dishes: ${dishesToExport.length}`);
    console.log(`Selected trays: ${usedTrayIdsToExport.join(', ')}`);
    
    dishesToExport.forEach(dish => {
      console.log(`\nDish: ${dish.name} (ID: ${dish.id})`);
      console.log(`  Quantity: ${dish.quantity}`);
      console.log(`  Prep bags: ${dish.prepBags.length}`);
      console.log(`  Completed prep bags: ${dish.completedPrepBags?.length || 0}`);
      
      // Safely calculate total ingredients
      let totalIngredients = 0;
      dish.prepBags.forEach(bag => {
        if (bag && bag.ingredients && Array.isArray(bag.ingredients)) {
          totalIngredients += bag.ingredients.length;
        }
        if (bag && bag.addedIngredients && Array.isArray(bag.addedIngredients)) {
          totalIngredients += bag.addedIngredients.length;
        }
      });
      console.log(`  Total ingredients: ${totalIngredients}`);
    });

    // Prepare the export data
    const exportData = {
      dishes: dishesToExport.map(dish => ({
        id: dish.id,
        name: dish.name,
        ingredients: dish.ingredients.map(ing => ({
          name: ing.name,
          weight: ing.weight || 0,
          unit: ing.unit || 'g'
        })),
        quantity: dish.quantity || 1,
        trayId: dish.trayId,
        prepBags: dish.prepBags.map(bag => {
          // Skip undefined bags
          if (!bag) {
            console.warn('Skipping undefined prep bag');
            return null;
          }
          
          // Use dish ingredients if bag ingredients are not available
          const ingredients = Array.isArray(bag.ingredients) && bag.ingredients.length > 0 
            ? bag.ingredients as Ingredient[]
            : dish.ingredients as Ingredient[];
          const addedIngredients = Array.isArray(bag.addedIngredients) ? bag.addedIngredients as Ingredient[] : [];
          
          // Check if this bag is in completedPrepBags
          const isCompleted = dish.completedPrepBags?.some(completed => 
            completed && completed.id === bag.id
          ) || false;
          
          return {
            id: bag.id || `bag_${Math.random().toString(36).substr(2, 9)}`,
            dishName: dish.name,
            ingredients: ingredients.map(ing => ({
              name: ing.name,
              weight: ing.weight || 0,
              unit: ing.unit || 'g'
            })),
            addedIngredients: addedIngredients.map(ing => ({
              name: ing.name,
              weight: ing.weight || 0,
              unit: ing.unit || 'g'
            })),
            isComplete: isCompleted
          };
        }).filter(bag => bag !== null), // Remove null entries
        completedPrepBags: (dish.completedPrepBags || []).filter(bag => bag !== null).map(bag => {
          // Use dish ingredients if bag ingredients are not available
          const ingredients = Array.isArray(bag.ingredients) && bag.ingredients.length > 0 
            ? bag.ingredients as Ingredient[]
            : dish.ingredients as Ingredient[];
          const addedIngredients = Array.isArray(bag.addedIngredients) ? bag.addedIngredients as Ingredient[] : [];
          
          return {
            id: bag.id || `completed_${Math.random().toString(36).substr(2, 9)}`,
            dishName: dish.name,
            ingredients: ingredients.map(ing => ({
              name: ing.name,
              weight: ing.weight || 0,
              unit: ing.unit || 'g'
            })),
            addedIngredients: addedIngredients.map(ing => ({
              name: ing.name,
              weight: ing.weight || 0,
              unit: ing.unit || 'g'
            })),
            isComplete: true
          };
        })
      })),
      selectedTrays: usedTrayIdsToExport
    };

    // Send the data to the backend
    const maxRetries = 3;
    let lastError;
    
    for (let i = 0; i < maxRetries; i++) {
      try {
        const response = await fetch(`${API_BASE_URL}/prep-tracking/receive-prep-data`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(exportData),
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        console.log('Export successful:', result);
        return result;
      } catch (error) {
        console.error(`Export attempt ${i + 1} failed:`, error);
        lastError = error;
        if (i < maxRetries - 1) {
          // Wait before retrying, with exponential backoff
          await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
        }
      }
    }

    // If all retries failed, save data locally
    console.warn('All export attempts failed, saving data locally');
    await saveExportDataLocally(exportData);
    throw lastError;
  } catch (error) {
    console.error('Export failed:', error);
    throw error;
  }
};

export const fetchGastronormTrays = async () => {
  try {
    // Use the local JSON file instead of fetching from API
    console.log('Using local gastronorm trays data');
    return gnPans;
  } catch (error) {
    console.error('Error fetching gastronorm trays:', error);
    throw error;
  }
}; 
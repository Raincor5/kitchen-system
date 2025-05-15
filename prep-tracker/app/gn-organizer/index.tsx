// app/gn-organizer/GNOrganizerScreen.tsx
import React, { useMemo, useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Dimensions, TextInput, Alert, ActivityIndicator } from 'react-native';
import { SafeAreaView, SafeAreaProvider } from 'react-native-safe-area-context';
import { useDishesContext } from '../../context/DishesContext';
import { useRouter } from 'expo-router';
import { fetchGastronormTrays } from '../utils/exportData';
import { Tray } from '../types/tray';
import { Dish } from '../types/dish';
import { PrepBag } from '../types/prepBag';

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get("window");
const guidelineBaseWidth = 350;
const guidelineBaseHeight = 680;
const scale = (size: number) => (SCREEN_WIDTH / guidelineBaseWidth) * size;
const verticalScale = (size: number) => (SCREEN_HEIGHT / guidelineBaseHeight) * size;
const moderateScale = (size: number, factor = 0.5) => size + (scale(size) - size) * factor;

interface DishPanSelection {
  panId: string;
  quantity: number;
}

interface DishPanSelections {
  [dishId: string]: DishPanSelection;
}

interface TrayWithCapacity extends Tray {
  estimatedCapacity: number;
  requiredPans: number;
}

interface DishWithOptions {
  dish: Dish;
  totalVolume: number;
  options: TrayWithCapacity[];
}

/**
 * For each dish, compute the total volume and average volume per prep bag.
 * We assume each ingredient's weight in grams approximates its volume in milliliters.
 * (Then we add a 10% margin and convert mL to liters.)
 */
const getDishVolume = (dish: Dish): number => {
  if (!dish.prepBags) return 0;
  
  const numBags = dish.prepBags.length;
  if (numBags === 0) return 0;
  
  let totalVolume = 0;
  dish.prepBags.forEach((bag: PrepBag) => {
    let bagVolume = 0;
    if (bag.ingredients) {
      bag.ingredients.forEach(ingredient => {
        if (ingredient.weight) {
          // Convert weight to volume based on unit
          switch (ingredient.unit) {
            case 'g':
              // For most ingredients, 1g ≈ 1ml
              bagVolume += ingredient.weight;
              break;
            case 'kg':
              // 1kg = 1000g ≈ 1000ml
              bagVolume += ingredient.weight * 1000;
              break;
            case 'ml':
              // Already in ml
              bagVolume += ingredient.weight;
              break;
            case 'l':
              // 1l = 1000ml
              bagVolume += ingredient.weight * 1000;
              break;
            default:
              // For unknown units, assume grams
              bagVolume += ingredient.weight;
          }
        }
      });
    }
    totalVolume += bagVolume;
  });
  
  // Add 10% margin for safety and convert to liters
  return (totalVolume * 1.1) / 1000;
};

/**
 * For each dish, build the list of pan options.
 * For a given dish and a given pan, we compute:
 *  - estimatedCapacity: number of prep bags that can fit in one pan,
 *  - requiredPans: number of pans required to cover all prep bags.
 */
const computeDishPanOptions = (dish: Dish, trays: Tray[]): TrayWithCapacity[] => {
  const numBags = dish.prepBags ? dish.prepBags.length : 0;
  return trays.map((pan: Tray) => {
    let estimatedCapacity = 0;
    let requiredPans = 0;
    if (pan.volumeLiters && getDishVolume(dish) > 0) {
      estimatedCapacity = Math.floor(pan.volumeLiters / getDishVolume(dish));
      if (estimatedCapacity > 0) {
        requiredPans = Math.ceil(numBags / estimatedCapacity);
      }
    }
    return { ...pan, estimatedCapacity, requiredPans };
  });
};

const GNOrganizerScreen = () => {
  const { dishesStack, reorderDishes } = useDishesContext();
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pans, setPans] = useState<Tray[]>([]);

  /**
   * State to store, for each dish (by dish.id), the selected pan and the quantity required.
   * Example: { "dish1": { panId: "gastronorm_2_1_20", quantity: 2 }, ... }
   */
  const [dishPanSelections, setDishPanSelections] = useState<DishPanSelections>({});

  // Load gastronorm trays from the local JSON file
  useEffect(() => {
    const loadTrays = async () => {
      try {
        setLoading(true);
        const trays = await fetchGastronormTrays();
        setPans(trays);
        setError(null);
      } catch (err) {
        console.error('Error loading trays:', err);
        setError('Failed to load gastronorm trays. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadTrays();
  }, []);

  // For each dish, compute its volume data and available pan options.
  const dishData = useMemo<DishWithOptions[]>(() => {
    return dishesStack.map(dish => {
      const totalVolume = getDishVolume(dish);
      const options = computeDishPanOptions(dish, pans);
      return { dish, totalVolume, options };
    });
  }, [dishesStack, pans]);

  // When dishData changes, initialize (or update) default selections.
  useEffect(() => {
    const newSelections: DishPanSelections = {};
    
    // Check if dishes already have tray assignments
    dishData.forEach(({ dish }) => {
      // If the dish has a trayId property, use it
      if (dish.trayId) {
        newSelections[dish.id] = { 
          panId: dish.trayId, 
          quantity: dish.trayQuantity || 1 
        };
      }
    });
    
    // For dishes without tray assignments, use the best option
    dishData.forEach(({ dish, options }) => {
      if (options.length === 0 || newSelections[dish.id]) return;
      
      // For default, choose the option with the highest estimatedCapacity.
      let bestOption = options[0];
      options.forEach(option => {
        if (option.estimatedCapacity > bestOption.estimatedCapacity) {
          bestOption = option;
        }
      });
      newSelections[dish.id] = { panId: bestOption.id, quantity: bestOption.requiredPans };
    });
    
    setDishPanSelections(newSelections);
  }, [dishData]);

  // Handler to update the selection for a given dish.
  const selectPanForDish = (dishId: string, panId: string, quantity: number) => {
    setDishPanSelections(prev => ({ ...prev, [dishId]: { panId, quantity } }));
  };

  // Handler for the "Return" button.
  const handleReturn = () => {
    router.back();
  };

  // Handler for the "Proceed to Summary" button.
  const handleProceed = async () => {
    try {
      setLoading(true);
      
      // Get all selected tray IDs
      const selectedTrayIds = Object.values(dishPanSelections).map(selection => selection.panId);
      
      // Update the dishes in the context with the selected trays
      const updatedDishes = dishesStack.map(dish => {
        const selection = dishPanSelections[dish.id];
        if (selection) {
          return {
            ...dish,
            trayId: selection.panId,
            trayQuantity: selection.quantity
          };
        }
        return dish;
      });
      
      // Update the dishes context
      reorderDishes(updatedDishes);
      
      // Navigate to the overall summary screen
      router.push("/overall-summary");
    } catch (err) {
      console.error('Error updating selected trays:', err);
      Alert.alert(
        'Error',
        'Failed to update selected trays. Please try again.',
        [{ text: 'OK' }]
      );
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <SafeAreaProvider>
        <SafeAreaView style={styles.container}>
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#0000ff" />
            <Text style={styles.loadingText}>Loading gastronorm trays...</Text>
          </View>
        </SafeAreaView>
      </SafeAreaProvider>
    );
  }

  if (error) {
    return (
      <SafeAreaProvider>
        <SafeAreaView style={styles.container}>
          <View style={styles.errorContainer}>
            <Text style={styles.errorText}>{error}</Text>
            <TouchableOpacity style={styles.retryButton} onPress={() => window.location.reload()}>
              <Text style={styles.buttonText}>Retry</Text>
            </TouchableOpacity>
          </View>
        </SafeAreaView>
      </SafeAreaProvider>
    );
  }

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <Text style={styles.title}>Gastronorm Pan Organizer</Text>
        <FlatList
          data={dishData}
          keyExtractor={(item) => item.dish.id}
          renderItem={({ item }) => {
            const { dish, totalVolume, options } = item;
            const selection = dishPanSelections[dish.id];
            return (
              <View style={styles.dishCard}>
                <Text style={styles.dishTitle}>{dish.name}</Text>
                <Text style={styles.dishInfo}>Prep Bags: {dish.prepBags.length}</Text>
                <Text style={styles.dishInfo}>Total Volume: {totalVolume.toFixed(2)} L</Text>
                <Text style={styles.optionTitle}>Select a Gastronorm Pan:</Text>
                {options.map(option => (
                  <TouchableOpacity
                    key={option.id}
                    style={[
                      styles.panOption,
                      selection && selection.panId === option.id && styles.panOptionSelected,
                    ]}
                    onPress={() =>
                      selectPanForDish(dish.id, option.id, option.requiredPans)
                    }
                  >
                    <Text style={styles.panOptionText}>{option.name}</Text>
                    <Text style={styles.panOptionDetails}>
                      Dimensions: {option.dimensions ? `${option.dimensions.length} x ${option.dimensions.width} x ${option.dimensions.depth} mm` : 'N/A'}
                    </Text>
                    <Text style={styles.panOptionDetails}>Capacity: {option.volumeLiters ? `${option.volumeLiters} L` : 'N/A'}</Text>
                    <Text style={styles.panOptionDetails}>
                      Estimated Prep Bags: {option.estimatedCapacity} per pan
                    </Text>
                    <Text style={styles.panOptionDetails}>
                      Pans Needed: {option.requiredPans}
                    </Text>
                  </TouchableOpacity>
                ))}
                {selection && (
                  <View style={styles.selectionSummary}>
                    <Text style={styles.selectionText}>
                      Selected: {selection.quantity} ×{" "}
                      {pans.find(p => p.id === selection.panId)?.name || "N/A"}
                    </Text>
                  </View>
                )}
              </View>
            );
          }}
          contentContainerStyle={styles.listContainer}
        />

        {/* Buttons at the bottom */}
        <View style={styles.buttonRow}>
          <TouchableOpacity style={styles.returnButton} onPress={handleReturn}>
            <Text style={styles.buttonText}>Return</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.proceedButton, loading && styles.buttonDisabled]} 
            onPress={handleProceed}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator size="small" color="#ffffff" />
            ) : (
              <Text style={styles.buttonText}>Proceed to Summary</Text>
            )}
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    </SafeAreaProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  errorText: {
    fontSize: 16,
    color: '#ff0000',
    textAlign: 'center',
    marginBottom: 20,
  },
  retryButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 5,
  },
  title: {
    fontSize: moderateScale(24),
    fontWeight: 'bold',
    textAlign: 'center',
    marginVertical: verticalScale(10),
  },
  listContainer: {
    padding: scale(10),
  },
  dishCard: {
    backgroundColor: 'white',
    borderRadius: scale(10),
    padding: scale(15),
    marginBottom: verticalScale(15),
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  dishTitle: {
    fontSize: moderateScale(18),
    fontWeight: 'bold',
    marginBottom: verticalScale(5),
  },
  dishInfo: {
    fontSize: moderateScale(14),
    color: '#666',
    marginBottom: verticalScale(3),
  },
  optionTitle: {
    fontSize: moderateScale(16),
    fontWeight: '600',
    marginTop: verticalScale(10),
    marginBottom: verticalScale(5),
  },
  panOption: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: scale(5),
    padding: scale(10),
    marginBottom: verticalScale(5),
  },
  panOptionSelected: {
    borderColor: '#007AFF',
    backgroundColor: '#f0f8ff',
  },
  panOptionText: {
    fontSize: moderateScale(14),
    fontWeight: '500',
  },
  panOptionDetails: {
    fontSize: moderateScale(12),
    color: '#666',
    marginTop: verticalScale(2),
  },
  selectionSummary: {
    marginTop: verticalScale(10),
    padding: scale(10),
    backgroundColor: '#f0f8ff',
    borderRadius: scale(5),
  },
  selectionText: {
    fontSize: moderateScale(14),
    color: '#007AFF',
    fontWeight: '500',
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    padding: scale(15),
    backgroundColor: 'white',
    borderTopWidth: 1,
    borderTopColor: '#ddd',
  },
  returnButton: {
    backgroundColor: '#666',
    paddingHorizontal: scale(20),
    paddingVertical: verticalScale(10),
    borderRadius: scale(5),
    minWidth: scale(100),
    alignItems: 'center',
  },
  proceedButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: scale(20),
    paddingVertical: verticalScale(10),
    borderRadius: scale(5),
    minWidth: scale(100),
    alignItems: 'center',
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: 'white',
    fontSize: moderateScale(16),
    fontWeight: '500',
  },
});

export default GNOrganizerScreen;

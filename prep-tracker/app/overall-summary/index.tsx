// app/overall-summary/index.tsx
import React, { useMemo, useState, useCallback } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  FlatList, 
  ScrollView, 
  TouchableOpacity, 
  Dimensions, 
  Alert,
  Modal 
} from 'react-native';
import { SafeAreaView, SafeAreaProvider } from 'react-native-safe-area-context';
import { useDishesContext } from '@/context/DishesContext';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { BlurView } from 'expo-blur';
import { exportToKitchenManager, fetchGastronormTrays } from '../utils/exportData';

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get("window");
const guidelineBaseWidth = 350;
const guidelineBaseHeight = 680;
const scale = (size: number) => (SCREEN_WIDTH / guidelineBaseWidth) * size;
const verticalScale = (size: number) => (SCREEN_HEIGHT / guidelineBaseHeight) * size;
const moderateScale = (size: number, factor = 0.5) => size + (scale(size) - size) * factor;

/**
 * Compute overall ingredients summary.
 * For each dish, we multiply each ingredient's weight by the dish's quantity and then sum them.
 */
const computeOverallIngredients = (dishesStack: any[]) => {
  const summary: Record<string, { totalWeight: number; unit: string }> = {};
  dishesStack.forEach(dish => {
    dish.ingredients.forEach((ing: any) => {
      const weight = ing.weight || 0;
      if (summary[ing.name]) {
        summary[ing.name].totalWeight += weight * dish.quantity;
      } else {
        summary[ing.name] = { totalWeight: weight * dish.quantity, unit: ing.unit || '' };
      }
    });
  });
  return Object.entries(summary);
};

/**
 * Compute overall prep bag volume.
 * For each dish, each prep bag's volume is the sum of its ingredient weights (in grams)
 * multiplied by 1.1 (for a 10% margin) and then converted to liters.
 */
const computeOverallVolumes = (dishesStack: any[]) => {
  const volumes: number[] = [];
  dishesStack.forEach(dish => {
    if (!dish.prepBags || !Array.isArray(dish.prepBags)) {
      console.warn(`Dish ${dish.name} has no prepBags array`);
      return;
    }
    
    dish.prepBags.forEach((bag: any) => {
      // Skip if bag is undefined or ingredients is undefined or not an array
      if (!bag) {
        console.warn(`Undefined prep bag found in dish ${dish.name}`);
        return;
      }
      
      if (!bag.ingredients || !Array.isArray(bag.ingredients)) {
        console.warn(`Prep bag ${bag.id || 'unknown'} has no ingredients array`);
        return;
      }
      
      const totalWeight = bag.ingredients.reduce((sum: number, ing: any) => sum + (ing.weight || 0), 0);
      const volumeLiters = (totalWeight * 1.1) / 1000;
      volumes.push(volumeLiters);
    });
  });
  return volumes;
};

const OverallSummaryScreen = () => {
  const { dishesStack } = useDishesContext();
  const router = useRouter();
  const { panSelections: panSelectionsParam, prepDuration: prepDurationParam } = useLocalSearchParams();
  const [isExporting, setIsExporting] = useState(false);

  // Parse route parameters – panSelections is expected to be a JSON string mapping dish IDs to { panId, quantity }.
  let panSelections: { [dishId: string]: { panId: string; quantity: number } } = {};
  try {
    if (panSelectionsParam) {
      panSelections = JSON.parse(panSelectionsParam as string);
    }
  } catch (error) {
    console.error("Error parsing panSelections:", error);
  }

  // For prepDuration, we expect a number (in minutes) as a string.
  const prepDuration = prepDurationParam ? Number(prepDurationParam) : 0;

  // Compute overall prep bag volumes.
  const allVolumes = useMemo(() => computeOverallVolumes(dishesStack), [dishesStack]);
  const totalVolume = useMemo(() => allVolumes.reduce((sum, v) => sum + v, 0), [allVolumes]);
  const averageVolume = allVolumes.length > 0 ? totalVolume / allVolumes.length : 0;

  // Compute overall ingredients summary.
  const overallIngredients = useMemo(() => computeOverallIngredients(dishesStack), [dishesStack]);

  // Handler for the Return button.
  const handleReturn = () => {
    router.back();
  };

  // Handler for confirming the summary.
  const handleConfirm = () => {
    // Show a success message
    Alert.alert(
      "Prepping Complete", 
      "Your prep session has been completed successfully.",
      [
        {
          text: "OK",
          onPress: () => {
            // Navigate back to the home screen
            router.replace("/");
          }
        }
      ]
    );
  };

  const handleExportPress = useCallback(async () => {
    // Get the dishes that have prep bags
    const dishesWithPrepBags = dishesStack.filter(dish => 
      dish.prepBags && dish.prepBags.length > 0
    );

    if (dishesWithPrepBags.length === 0) {
      Alert.alert('Error', 'No dishes with prep bags to export');
      return;
    }

    // Collect all unique tray IDs from the dishes
    const trayIds = new Set<string>();
    dishesWithPrepBags.forEach(dish => {
      if (dish.trayId) {
        trayIds.add(dish.trayId);
      }
    });

    // Convert to array
    const selectedTrayIds = Array.from(trayIds);

    if (selectedTrayIds.length === 0) {
      Alert.alert('Error', 'No trays selected for any dishes');
      return;
    }

    setIsExporting(true);
    try {
      const result = await exportToKitchenManager(dishesWithPrepBags, selectedTrayIds);
      
      // Check if we're using the fallback mechanism
      if (result.message && result.message.includes('locally')) {
        Alert.alert(
          'Export Successful (Offline Mode)',
          'Your data was saved locally because the Kitchen Manager API is currently unavailable. You can try exporting again later when the connection is restored.',
          [{ text: 'OK' }]
        );
      } else {
        Alert.alert('Success', 'Data exported to kitchen manager successfully');
      }
    } catch (error) {
      Alert.alert(
        'Export Error', 
        error instanceof Error 
          ? error.message 
          : 'Failed to export data to kitchen manager. Your data was saved locally.'
      );
    } finally {
      setIsExporting(false);
    }
  }, [dishesStack]);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Finish Prepping</Text>
        </View>
        
        <View style={styles.summaryContainer}>
          <Text style={styles.subtitle}>
            Prepping Duration: {prepDuration > 0 ? `${prepDuration} minutes` : "N/A"}
          </Text>
          <Text style={styles.subtitle}>
            Average Prep Bag Volume: {averageVolume.toFixed(2)} L (with 10% margin)
          </Text>
        </View>
        
        <TouchableOpacity 
          style={styles.exportButton} 
          onPress={handleExportPress}
          disabled={isExporting}
        >
          <Text style={styles.exportButtonText}>
            {isExporting ? 'Exporting...' : 'Export to Kitchen Manager'}
          </Text>
        </TouchableOpacity>
        
        <ScrollView contentContainerStyle={styles.scrollContent}>
          <Text style={styles.title}>Overall Prepping Summary</Text>
          <Text style={styles.subtitle}>
            Prepping Duration: {prepDuration > 0 ? `${prepDuration} minutes` : "N/A"}
          </Text>
          <Text style={styles.subtitle}>
            Average Prep Bag Volume: {averageVolume.toFixed(2)} L (with 10% margin)
          </Text>
          
          <Text style={styles.sectionTitle}>Dishes Summary</Text>
          <FlatList
            data={dishesStack}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <View style={styles.dishCard}>
                <Text style={styles.dishName}>{item.name}</Text>
                <Text style={styles.dishInfo}>Number of Prep Bags: {item.prepBags.length}</Text>
                {item.trayId && (
                  <Text style={styles.dishInfo}>
                    Selected Tray: {item.trayId} (Quantity: {item.trayQuantity || 1})
                  </Text>
                )}
              </View>
            )}
            scrollEnabled={false}
          />

          <Text style={styles.sectionTitle}>Ingredients Summary</Text>
          <FlatList
            data={overallIngredients}
            keyExtractor={([name]) => name}
            renderItem={({ item: [name, { totalWeight, unit }] }) => (
              <View style={styles.ingredientCard}>
                <Text style={styles.ingredientText}>
                  {name}: {totalWeight.toFixed(2)} {unit}
                </Text>
              </View>
            )}
            scrollEnabled={false}
          />

          <Text style={styles.sectionTitle}>Gastronorm Pan Selections</Text>
          <FlatList
            data={Object.entries(panSelections)}
            keyExtractor={([dishId]) => dishId}
            renderItem={({ item: [dishId, selection] }) => {
              // Try to find the dish name from the dishesStack
              const dish = dishesStack.find((d: any) => d.id === dishId);
              return (
                <View style={styles.panCard}>
                  <Text style={styles.dishName}>
                    {dish ? dish.name : dishId}
                  </Text>
                  <Text style={styles.panDetails}>
                    Selected Pan: {selection.panId}
                  </Text>
                  <Text style={styles.panDetails}>
                    Number of Pans: {selection.quantity}
                  </Text>
                </View>
              );
            }}
            scrollEnabled={false}
          />

          <View style={styles.buttonRow}>
            <TouchableOpacity style={styles.returnButton} onPress={handleReturn}>
              <Text style={styles.buttonText}>Return</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.confirmButton} onPress={handleConfirm}>
              <Text style={styles.buttonText}>Confirm Summary</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </SafeAreaView>
    </SafeAreaProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#121212",
    padding: scale(16),
  },
  header: {
    marginBottom: verticalScale(8),
  },
  summaryContainer: {
    marginBottom: verticalScale(16),
  },
  title: {
    color: "#fff",
    fontSize: moderateScale(24),
    fontWeight: "bold",
  },
  subtitle: {
    color: "#fff",
    fontSize: moderateScale(18),
    marginBottom: verticalScale(8),
  },
  exportButton: {
    backgroundColor: "#4CAF50",
    paddingVertical: verticalScale(12),
    paddingHorizontal: scale(20),
    borderRadius: 8,
    marginBottom: verticalScale(16),
    alignItems: 'center',
  },
  exportButtonText: {
    color: "#fff",
    fontSize: moderateScale(18),
    fontWeight: "bold",
  },
  scrollContent: {
    paddingBottom: verticalScale(20),
  },
  sectionTitle: {
    color: "#fff",
    fontSize: moderateScale(20),
    fontWeight: "bold",
    marginVertical: verticalScale(8),
  },
  dishCard: {
    backgroundColor: "#1e1e1e",
    borderRadius: 8,
    padding: scale(12),
    marginBottom: verticalScale(8),
  },
  dishName: {
    color: "#4CAF50",
    fontSize: moderateScale(18),
    fontWeight: "bold",
  },
  dishInfo: {
    color: "#fff",
    fontSize: moderateScale(16),
  },
  ingredientCard: {
    backgroundColor: "#1e1e1e",
    borderRadius: 8,
    padding: scale(10),
    marginBottom: verticalScale(6),
  },
  ingredientText: {
    color: "#fff",
    fontSize: moderateScale(16),
  },
  panCard: {
    backgroundColor: "#1e1e1e",
    borderRadius: 8,
    padding: scale(12),
    marginBottom: verticalScale(8),
  },
  panDetails: {
    color: "#fff",
    fontSize: moderateScale(16),
  },
  buttonRow: {
    flexDirection: "row",
    justifyContent: "space-around",
    marginTop: verticalScale(20),
  },
  returnButton: {
    backgroundColor: "#FF5722",
    paddingVertical: verticalScale(12),
    paddingHorizontal: scale(20),
    borderRadius: 8,
  },
  confirmButton: {
    backgroundColor: "#4CAF50",
    paddingVertical: verticalScale(12),
    paddingHorizontal: scale(20),
    borderRadius: 8,
  },
  buttonText: {
    color: "#fff",
    fontSize: moderateScale(18),
    fontWeight: "bold",
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.5)",
  },
  modalContent: {
    backgroundColor: "#121212",
    borderRadius: 16,
    padding: scale(20),
    width: "80%",
    alignSelf: "center",
    position: "absolute",
    bottom: 0,
  },
  modalHeaderText: {
    color: "#fff",
    fontSize: moderateScale(20),
    fontWeight: "bold",
    marginBottom: verticalScale(16),
  },
  modalScroll: {
    maxHeight: verticalScale(300),
  },
  trayItem: {
    backgroundColor: "#1e1e1e",
    borderRadius: 8,
    padding: scale(12),
    marginBottom: verticalScale(6),
  },
  trayItemSelected: {
    backgroundColor: "#4CAF50",
  },
  trayItemText: {
    color: "#fff",
    fontSize: moderateScale(16),
  },
  modalButtons: {
    flexDirection: "row",
    justifyContent: "space-around",
    marginTop: verticalScale(20),
  },
  modalButton: {
    backgroundColor: "#4CAF50",
    paddingVertical: verticalScale(12),
    paddingHorizontal: scale(20),
    borderRadius: 8,
  },
  cancelButton: {
    backgroundColor: "#FF5722",
  },
  modalButtonText: {
    color: "#fff",
    fontSize: moderateScale(18),
    fontWeight: "bold",
  },
});

export default OverallSummaryScreen;

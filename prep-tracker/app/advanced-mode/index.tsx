import React, { useEffect, useMemo, useState, useRef, useCallback } from "react";
import {
  View,
  StyleSheet,
  Text,
  ScrollView,
  TouchableOpacity,
  Dimensions,
  Modal,
  Alert,
} from "react-native";
import {
  SafeAreaProvider,
  SafeAreaView,
  useSafeAreaInsets,
} from "react-native-safe-area-context";
import DraggableFlatList, { RenderItemParams } from "react-native-draggable-flatlist";
import DraggableDish from "@/app/advanced-mode/DraggableDish";
import { useDishesContext } from "@/context/DishesContext";
import { useIngredientsContext } from "@/context/IngredientsContext";
import { IngredientChip, getRelevantIngredients } from "./IngredientChip";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { useGestureManager } from "@/context/GestureManagerContext";
import { BlurView } from "expo-blur";
import { exportToKitchenManager, fetchGastronormTrays } from '../utils/exportData';
import { PrepBag } from '../types/prepBag';
import { Ingredient } from '../types/ingredient';
import { Dish } from '../types/dish';

// Scaling utilities based on device dimensions.
const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get("window");
const guidelineBaseWidth = 350;
const guidelineBaseHeight = 680;
const scale = (size: number) => (SCREEN_WIDTH / guidelineBaseWidth) * size;
const verticalScale = (size: number) => (SCREEN_HEIGHT / guidelineBaseHeight) * size;
const moderateScale = (size: number, factor = 0.5) =>
  size + (scale(size) - size) * factor;

// Utility: Create a matrix from an array of prepBags.
const createMatrix = (prepBags: PrepBag[], columnHeight = 8) => {
  if (!prepBags || !Array.isArray(prepBags) || prepBags.length === 0) return [];
  
  // Filter out any invalid prep bags
  const validPrepBags = prepBags.filter((bag): bag is PrepBag => 
    bag !== null && 
    bag !== undefined && 
    Array.isArray(bag.ingredients) && 
    Array.isArray(bag.addedIngredients)
  );
  
  if (validPrepBags.length === 0) return [];
  
  // Calculate the number of columns needed
  const numCols = Math.ceil(validPrepBags.length / columnHeight);
  
  // Create the initial matrix with the correct dimensions
  const matrix = Array(columnHeight).fill(null).map(() => Array(numCols).fill(null));
  
  // Fill the matrix column by column
  validPrepBags.forEach((bag, index) => {
    const col = Math.floor(index / columnHeight);
    const row = index % columnHeight;
    matrix[row][col] = bag;
  });
  
  return matrix;
};

// Helper function to check if an ingredient is exhausted
const isIngredientExhausted = (ingredientName: string, prepBags: PrepBag[]): boolean => {
  // Count how many bags need this ingredient
  const bagsNeedingIngredient = prepBags.filter(bag => 
    bag.ingredients.some(ing => ing.name === ingredientName)
  );
  
  if (bagsNeedingIngredient.length === 0) return false;
  
  // Count how many bags have this ingredient confirmed
  const confirmedBags = bagsNeedingIngredient.filter(bag => 
    bag.addedIngredients.some(added => added.name === ingredientName)
  ).length;
  
  // Debug logging
  console.log(`isIngredientExhausted for ${ingredientName}: ${confirmedBags} confirmed out of ${bagsNeedingIngredient.length} needed`);
  
  // Ingredient is exhausted if all bags that need it have it confirmed
  return confirmedBags >= bagsNeedingIngredient.length;
};

// Define baseCellStyle similar to DraggableDish.tsx
const baseCellStyle = { width: 50, height: 50, margin: 2 };

// Define updateCellLayout function
const updateCellLayout = (bagId: string, layout: { x: number; y: number; width: number; height: number }) => {
  // Implementation similar to DraggableDish.tsx
};

export default function AdvancedModeScreen() {
  const insets = useSafeAreaInsets();
  const { dishesStack, reorderDishes, updatePrepBag } = useDishesContext();
  const { confirmUpdates, getEffectiveCount } = useIngredientsContext();
  const [focusedIndices, setFocusedIndices] = useState<number[]>([]);
  const [selectedIngredients, setSelectedIngredients] = useState<string[]>([]);
  const [showMissing, setShowMissing] = useState(false);
  const [activeBagDetails, setActiveBagDetails] = useState<{ dishId: string; bag: any } | null>(null);
  const ingredientScrollRef = useRef<ScrollView>(null);
  const flatListRef = useRef(null);
  const { isSwipeEnabled, isScrollEnabled, setGestureState } = useGestureManager();
  const [showTraySelection, setShowTraySelection] = useState(false);
  const [availableTrays, setAvailableTrays] = useState<any[]>([]);
  const [selectedTrays, setSelectedTrays] = useState<string[]>([]);
  const [isExporting, setIsExporting] = useState(false);
  const [dishes, setDishes] = useState<Dish[]>([]);
  const [selectedIngredient, setSelectedIngredient] = useState<string | null>(null);
  const [prepBags, setPrepBags] = useState<PrepBag[]>([]);
  const [ingredientsReordered, setIngredientsReordered] = useState(false);

  useEffect(() => {
    setGestureState(selectedIngredients);
  }, [selectedIngredients, setGestureState]);

  // Convert dishes to prep bags
  useEffect(() => {
    const allPrepBags = dishes.flatMap(dish => dish.prepBags);
    setPrepBags(allPrepBags);
  }, [dishes]);

  const isExhausted = useCallback((ingredient: string) => {
    return isIngredientExhausted(ingredient, prepBags);
  }, [prepBags]);

  const focusedDishes = useMemo(() => {
    return focusedIndices.map((i) => dishesStack[i]).filter(Boolean);
  }, [dishesStack, focusedIndices]);

  const effectiveDishes = focusedDishes.length > 0 ? focusedDishes : dishesStack;

  const relevantIngredients = useMemo(() => getRelevantIngredients(effectiveDishes), [effectiveDishes]);

  // Effect to handle ingredient reordering
  useEffect(() => {
    if (ingredientsReordered) {
      // Scroll to the end of the list after reordering
      if (ingredientScrollRef.current) {
        ingredientScrollRef.current.scrollToEnd({ animated: true });
      }
      setIngredientsReordered(false);
    }
  }, [ingredientsReordered]);

  const onBagPress = useCallback(
    (dishId: string, bagId: string) => {
      const dish = dishesStack.find((d) => d.id === dishId);
      if (!dish) return;

      const bag = dish.prepBags.find((b: PrepBag) => b && b.id === bagId);
      if (!bag) return;

      console.log(`onBagPress called for dish ${dishId}, bag ${bagId}`);
      console.log(`Selected ingredients: ${selectedIngredients.join(', ')}`);
      console.log(`Bag ingredients: ${bag.ingredients.map(i => i.name).join(', ')}`);
      console.log(`Bag added ingredients: ${bag.addedIngredients.map(i => i.name).join(', ')}`);

      if (selectedIngredients.length > 0) {
        // Get all ingredients that are allowed for this bag
        const allowed = new Set(bag.ingredients.map((ing: any) => ing.name));

        // Process each selected ingredient
        selectedIngredients.forEach((ing) => {
          if (!allowed.has(ing)) {
            console.log(`Ingredient ${ing} is not allowed for this bag`);
            return;
          }
          
          console.log(`Processing ingredient ${ing} for bag ${bagId}`);
          
          // Check if ingredient was already exhausted before this update
          const wasExhaustedBefore = isIngredientExhausted(ing, dishesStack.flatMap(d => d.prepBags));
          
          // Check if the ingredient is already added to this bag
          const isAlreadyAdded = bag.addedIngredients.some((ai: Ingredient) => ai.name === ing);
          
          console.log(`Ingredient ${ing} is ${isAlreadyAdded ? 'already added' : 'not added'} to this bag`);
          
          if (isAlreadyAdded) {
            // If already added, remove it
            console.log(`Removing ingredient ${ing} from bag ${bagId}`);
            updatePrepBag(bagId, {
              ...bag,
              addedIngredients: bag.addedIngredients.filter((ai: Ingredient) => ai.name !== ing),
            });
          } else {
            // If not already added, add it
            console.log(`Adding ingredient ${ing} to bag ${bagId}`);
            const updatedBagIngredients = [...bag.addedIngredients, { name: ing }];
            updatePrepBag(bagId, {
              ...bag,
              addedIngredients: updatedBagIngredients,
            });
          }
          
          // After adding or removing, check if the ingredient is now exhausted
          // We need to create a temporary updated dish stack to check this
          const tempDishStack = dishesStack.map(d => {
            if (d.id === dishId) {
              return {
                ...d,
                prepBags: d.prepBags.map(b => {
                  if (b.id === bagId) {
                    return {
                      ...b,
                      addedIngredients: isAlreadyAdded 
                        ? b.addedIngredients.filter((ai: Ingredient) => ai.name !== ing)
                        : [...b.addedIngredients, { name: ing }],
                    };
                  }
                  return b;
                })
              };
            }
            return d;
          });
          
          const isExhaustedAfter = isIngredientExhausted(ing, tempDishStack.flatMap(d => d.prepBags));
          
          console.log(`Ingredient ${ing}: was exhausted before: ${wasExhaustedBefore}, is exhausted after: ${isExhaustedAfter}`);
          
          if (!wasExhaustedBefore && isExhaustedAfter) {
            console.log(`Ingredient ${ing} is newly exhausted, confirming updates.`);
            
            // Remove this ingredient from selection immediately
            setSelectedIngredients(prev => prev.filter(name => name !== ing));
            
            // Apply to all bags needing this ingredient
            dishesStack.forEach(d => {
              d.prepBags.forEach(b => {
                if (b.ingredients.some(i => i.name === ing) && 
                    !b.addedIngredients.some(ai => ai.name === ing)) {
                  console.log(`Adding ingredient ${ing} to bag ${b.id} during auto-confirmation`);
                  // Add the ingredient to the bag
                  const updatedBagIngredients = [...b.addedIngredients, { name: ing }];
                  const bagToUpdate = {
                    ...b,
                    addedIngredients: updatedBagIngredients,
                  };
                  
                  updatePrepBag(b.id, bagToUpdate);
                  
                  // Confirm the update
                  confirmUpdates(
                    b.id,
                    updatedBagIngredients.map(ai => ai.name),
                    b.ingredients.map(i => ({
                      name: i.name,
                      weight: i.weight,
                      unit: i.unit
                    })),
                    (confirmedBagId, newAddedIngredients) => {
                      updatePrepBag(confirmedBagId, {
                        ...b,
                        addedIngredients: newAddedIngredients
                      });
                    }
                  );
                }
              });
            });
            
            // Mark that ingredients need to be reordered
            setIngredientsReordered(true);
          }
        });
      } else {
        // Otherwise, show the bag's details in a modal.
        setActiveBagDetails({ dishId, bag });
      }
    },
    [dishesStack, selectedIngredients, updatePrepBag, isIngredientExhausted, confirmUpdates]
  );

  const viewabilityConfig = useMemo(
    () => ({
      viewAreaCoveragePercentThreshold: 50,
      minimumViewTime: 100,
      waitForInteraction: false,
    }),
    []
  );

  const onViewableItemsChanged = useCallback(
    ({ viewableItems }: { viewableItems: any[] }) => {
      const indices = viewableItems
        .map((item) => item.index)
        .filter((i): i is number => i !== null && i !== undefined);
      setFocusedIndices(indices);
    },
    []
  );

  const toggleIngredient = useCallback((name: string) => {
    setSelectedIngredients((prev) =>
      prev.includes(name) ? prev.filter((ing) => ing !== name) : [...prev, name]
    );
  }, []);

  const disableDrag = selectedIngredients.length > 0;

  const confirmAllUpdates = useCallback(() => {
    dishesStack.forEach((dish) => {
      dish.prepBags.forEach((bag: any) => {
        if (!bag) return;
        const effectiveCount = getEffectiveCount(
          bag.id,
          bag.addedIngredients.map((ai: any) => ai.name),
          bag.ingredients.length,
          showMissing
        );
        if (effectiveCount !== bag.addedIngredients.length) {
          confirmUpdates(
            bag.id,
            bag.addedIngredients.map((ai: any) => ai.name),
            bag.ingredients.map((ing: any) => ({
              name: ing.name,
              weight: ing.weight,
              unit: ing.unit
            })),
            (bagId, newAddedIngredients) => {
              updatePrepBag(bagId, {
                ...bag,
                addedIngredients: newAddedIngredients
              });
            }
          );
        }
      });
    });
    
    // Check if any selected ingredients are now exhausted
    const exhaustedIngredients = selectedIngredients.filter(ing => 
      isIngredientExhausted(ing, dishesStack.flatMap(d => d.prepBags))
    );
    
    if (exhaustedIngredients.length > 0) {
      // Remove exhausted ingredients from selection
      setSelectedIngredients(prev => 
        prev.filter(ing => !exhaustedIngredients.includes(ing))
      );
      
      // Mark that ingredients need to be reordered
      setIngredientsReordered(true);
    }
  }, [dishesStack, getEffectiveCount, confirmUpdates, updatePrepBag, showMissing, selectedIngredients, isIngredientExhausted]);

  const handleTraySelection = useCallback((trayId: string) => {
    setSelectedTrays(prev => 
      prev.includes(trayId) 
        ? prev.filter(id => id !== trayId)
        : [...prev, trayId]
    );
  }, []);

  const handleExportConfirm = useCallback(async () => {
    if (selectedTrays.length === 0) {
      Alert.alert('Error', 'Please select at least one tray');
      return;
    }

    setIsExporting(true);
    try {
      // Get the dishes that have prep bags
      const dishesWithPrepBags = dishesStack.filter(dish => 
        dish.prepBags && dish.prepBags.length > 0
      );

      if (dishesWithPrepBags.length === 0) {
        Alert.alert('Error', 'No dishes with prep bags to export');
        return;
      }

      // Get the selected tray objects from availableTrays
      const selectedTrayObjects = availableTrays.filter(tray => 
        selectedTrays.includes(tray.id)
      );

      await exportToKitchenManager(dishesWithPrepBags, selectedTrayObjects);
      Alert.alert('Success', 'Data exported to kitchen manager successfully');
      setShowTraySelection(false);
      setSelectedTrays([]);
    } catch (error) {
      Alert.alert('Error', error instanceof Error ? error.message : 'Failed to export data to kitchen manager');
    } finally {
      setIsExporting(false);
    }
  }, [dishesStack, selectedTrays, availableTrays]);

  const renderItem = useCallback(
    ({ item, drag, isActive }: RenderItemParams<typeof dishesStack[0]>) => {
      const matrix = createMatrix(item.prepBags, 8);
      return (
        <View style={[styles.dishItem, { opacity: isActive ? 0.8 : 1 }]}>
          <DraggableDish
            dishId={item.id}
            label={item.name}
            matrix={matrix}
            colour={item.colour}
            editable={disableDrag}
            onBagPress={(bagId: string) => onBagPress(item.id, bagId)}
            selectedIngredients={selectedIngredients}
            isSwipeEnabled={isSwipeEnabled}
            isExhausted={isExhausted}
          />
          {!disableDrag && (
            <TouchableOpacity style={styles.dragHandle} onLongPress={drag}>
              <Text style={styles.dragHandleText}>≡</Text>
            </TouchableOpacity>
          )}
        </View>
      );
    },
    [disableDrag, onBagPress, selectedIngredients, isSwipeEnabled, dishesStack, isExhausted]
  );

  useEffect(() => {
    ingredientScrollRef.current?.scrollTo({ x: 0, animated: true });
  }, [relevantIngredients]);

  return (
    <GestureHandlerRootView style={styles.root}>
      <SafeAreaProvider>
        <SafeAreaView
          style={[styles.safeArea, { paddingTop: insets.top, paddingBottom: insets.bottom }]}
          edges={["top", "left", "right", "bottom"]}
        >
          <View style={styles.modalContainer}>
            {/* Header */}
            <View style={styles.headerContainer}>
              <Text style={styles.modalTitle}>Advanced Mode</Text>
              <View style={styles.modalDivider} />
            </View>
            {/* Ingredients */}
            <View style={styles.ingredientContainer}>
              <Text style={styles.ingredientTitle}>Select Ingredients</Text>
              <ScrollView
                ref={ingredientScrollRef}
                horizontal
                contentContainerStyle={styles.ingredientBarContent}
                style={styles.ingredientBar}
                showsHorizontalScrollIndicator={false}
              >
                {relevantIngredients.length === 0 ? (
                  <Text style={styles.emptyIngredientsText}>No ingredients</Text>
                ) : (
                  relevantIngredients.map((name) => {
                    const isIngredientExhausted = isExhausted(name);
                    console.log(`Ingredient ${name} is exhausted: ${isIngredientExhausted}`);
                    
                    return (
                      <IngredientChip
                        key={name}
                        name={name}
                        selected={selectedIngredients.includes(name)}
                        onToggle={() => toggleIngredient(name)}
                        exhausted={isIngredientExhausted}
                      />
                    );
                  })
                )}
              </ScrollView>
            </View>
            {/* Draggable Dishes List */}
            <View style={styles.dishesContainer}>
              <DraggableFlatList
                ref={flatListRef}
                data={dishesStack}
                keyExtractor={(dish) => dish.id}
                renderItem={renderItem}
                onDragEnd={({ data }) => reorderDishes(data)}
                onViewableItemsChanged={onViewableItemsChanged}
                viewabilityConfig={viewabilityConfig}
                horizontal
                initialNumToRender={3}
                maxToRenderPerBatch={3}
                windowSize={5}
                contentContainerStyle={styles.listContent}
                scrollEnabled={isScrollEnabled}
                showsHorizontalScrollIndicator={true}
                getItemLayout={(data, index) => ({
                  length: 200, // Approximate width of each dish item
                  offset: 200 * index,
                  index,
                })}
                simultaneousHandlers={[]}
              />
            </View>
          </View>
          {/* Confirmation Button */}
          {disableDrag && (
            <View style={styles.buttonContainer}>
              <TouchableOpacity style={styles.confirmButton} onPress={confirmAllUpdates}>
                <Text style={styles.confirmButtonText}>Confirm Ingredients</Text>
              </TouchableOpacity>
            </View>
          )}
          {/* Ingredient Showcase Modal for Prep Bag Details */}
          {activeBagDetails && (
            <Modal
              visible={true}
              transparent={true}
              animationType="fade"
              onRequestClose={() => setActiveBagDetails(null)}
            >
              <BlurView intensity={50} tint="dark" style={styles.modalOverlay}>
                <View style={styles.modalContent}>
                  <Text style={styles.modalHeaderText}>Prep Bag Details</Text>
                  <ScrollView style={styles.modalScroll}>
                    {activeBagDetails.bag.ingredients.map((ing: any) => {
                      const isAdded = activeBagDetails.bag.addedIngredients.some(
                        (ai: any) => ai.name === ing.name
                      );
                      return (
                        <View key={ing.name} style={styles.ingredientRow}>
                          <Text style={[styles.ingredientName, isAdded && styles.ingredientAdded]}>
                            {ing.name}: {ing.weight} {ing.unit} {isAdded ? "(Added)" : "(Missing)"}
                          </Text>
                        </View>
                      );
                    })}
                  </ScrollView>
                  <TouchableOpacity style={styles.closeButton} onPress={() => setActiveBagDetails(null)}>
                    <Text style={styles.closeButtonText}>Close</Text>
                  </TouchableOpacity>
                </View>
              </BlurView>
            </Modal>
          )}
          {/* Tray Selection Modal */}
          <Modal
            visible={showTraySelection}
            transparent={true}
            animationType="fade"
            onRequestClose={() => setShowTraySelection(false)}
          >
            <BlurView intensity={50} tint="dark" style={styles.modalOverlay}>
              <View style={styles.modalContent}>
                <Text style={styles.modalHeaderText}>Select Trays</Text>
                <ScrollView style={styles.modalScroll}>
                  {availableTrays.map((tray) => (
                    <TouchableOpacity
                      key={tray.id}
                      style={[
                        styles.trayItem,
                        selectedTrays.includes(tray.id) && styles.trayItemSelected
                      ]}
                      onPress={() => handleTraySelection(tray.id)}
                    >
                      <Text style={styles.trayItemText}>
                        {tray.name} ({tray.size})
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
                <View style={styles.modalButtons}>
                  <TouchableOpacity 
                    style={[styles.modalButton, styles.cancelButton]} 
                    onPress={() => {
                      setShowTraySelection(false);
                      setSelectedTrays([]);
                    }}
                  >
                    <Text style={styles.modalButtonText}>Cancel</Text>
                  </TouchableOpacity>
                  <TouchableOpacity 
                    style={[styles.modalButton, styles.confirmButton]}
                    onPress={handleExportConfirm}
                    disabled={isExporting}
                  >
                    <Text style={styles.modalButtonText}>
                      {isExporting ? 'Exporting...' : 'Export'}
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            </BlurView>
          </Modal>
        </SafeAreaView>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: "#121212" },
  safeArea: { flex: 1, backgroundColor: "#121212" },
  modalContainer: { flex: 1, paddingHorizontal: scale(16), paddingBottom: verticalScale(8) },
  headerContainer: { paddingVertical: verticalScale(6), alignItems: "center" },
  modalTitle: { color: "#fff", fontSize: moderateScale(20), fontWeight: "700" },
  modalDivider: { width: "90%", height: verticalScale(1), backgroundColor: "#555", marginTop: verticalScale(4) },
  ingredientContainer: { marginVertical: verticalScale(5) },
  ingredientTitle: { color: "#fff", fontSize: moderateScale(16), fontWeight: "600", marginBottom: verticalScale(4), marginHorizontal: scale(4) },
  ingredientBar: { marginBottom: verticalScale(6) },
  ingredientBarContent: { alignItems: "center", paddingHorizontal: scale(4) },
  emptyIngredientsText: { color: "#fff", marginLeft: scale(8) },
  dishesContainer: {
    flex: 1,
    marginTop: 10,
    minHeight: 200, // Ensure minimum height for the container
  },
  listContent: {
    paddingHorizontal: 10,
    alignItems: 'center', // Center items vertically
  },
  dishItem: {
    marginRight: 15,
    backgroundColor: '#1e1e1e',
    borderRadius: 8,
    padding: 10,
    minHeight: 200, // Match container minHeight
    justifyContent: 'center', // Center content vertically
  },
  dragHandle: {
    position: 'absolute',
    top: 5,
    right: 5,
    padding: 5,
  },
  dragHandleText: {
    color: '#fff',
    fontSize: 20,
  },
  buttonContainer: { paddingHorizontal: scale(16), paddingVertical: verticalScale(8), backgroundColor: "#121212" },
  confirmButton: { backgroundColor: "#4CAF50", paddingVertical: verticalScale(10), paddingHorizontal: scale(20), borderRadius: moderateScale(8), alignSelf: "center" },
  confirmButtonText: { color: "#fff", fontWeight: "bold", fontSize: moderateScale(16) },
  modalOverlay: { flex: 1, justifyContent: "center", alignItems: "center" },
  modalContent: { width: "80%", maxHeight: "70%", backgroundColor: "rgba(0,0,0,0.8)", borderRadius: scale(8), padding: scale(16) },
  modalHeaderText: { fontSize: moderateScale(18), fontWeight: "bold", marginBottom: verticalScale(8), textAlign: "center", color: "#fff" },
  modalScroll: { marginBottom: verticalScale(8) },
  ingredientRow: { paddingVertical: verticalScale(4), borderBottomWidth: 1, borderBottomColor: "#555" },
  ingredientName: { fontSize: moderateScale(16), color: "#fff" },
  ingredientAdded: { color: "green" },
  closeButton: { alignSelf: "center", backgroundColor: "#4CAF50", paddingVertical: verticalScale(8), paddingHorizontal: scale(16), borderRadius: scale(4) },
  closeButtonText: { color: "#fff", fontSize: moderateScale(16), fontWeight: "bold" },
  exportButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
    marginLeft: 8,
  },
  exportButtonText: {
    color: 'white',
    fontSize: 14,
    fontWeight: '600',
  },
  trayItem: {
    padding: scale(12),
    borderBottomWidth: 1,
    borderBottomColor: '#555',
  },
  trayItemSelected: {
    backgroundColor: '#007AFF33',
  },
  trayItemText: {
    color: '#fff',
    fontSize: moderateScale(16),
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: verticalScale(16),
  },
  modalButton: {
    flex: 1,
    paddingVertical: verticalScale(8),
    paddingHorizontal: scale(16),
    borderRadius: scale(4),
    marginHorizontal: scale(4),
  },
  cancelButton: {
    backgroundColor: '#666',
  },
  modalButtonText: {
    color: '#fff',
    fontSize: moderateScale(16),
    fontWeight: 'bold',
    textAlign: 'center',
  },
  cell: {
    backgroundColor: '#1e1e1e',
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#333',
    position: 'relative',
    margin: 2,
    width: 40,
    height: 40,
  },
  completeCell: {
    backgroundColor: '#4CAF50',
    borderColor: '#45a049',
  },
  exhaustedCell: {
    borderColor: '#ff0000',
    borderWidth: 2,
  },
  cellText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '500',
  },
  checkmark: {
    position: 'absolute',
    top: 2,
    right: 4,
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  selectedIngredientCell: {
    borderWidth: 2,
    borderColor: '#ff0000',
  },
  cellTouchable: {
    width: '100%',
    height: '100%',
  },
});

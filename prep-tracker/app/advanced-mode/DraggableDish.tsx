// app/advanced-mode/DraggableDish.tsx
// A component that displays a draggable dish with prep bags.
// Purpose: This component displays a draggable dish with prep bags that can be swiped over to add ingredients.
// The swipe handler is enabled only when the gesture manager indicates that swipe is enabled.
// Additionally, when a bag is swiped over, a small scale animation is triggered to visually indicate the action,
// and if a bag is complete (i.e. full) or an ingredient is exhausted, a red border is applied.
import React, { useRef, useEffect, useCallback } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Animated,
} from "react-native";
import { PanGestureHandler, State as GestureState } from "react-native-gesture-handler";
import { PrepBag } from "@/types/prepBags";
import { useIngredientsContext } from "@/context/IngredientsContext";

export type DraggableDishProps = {
  dishId: string;
  label: string;
  matrix: (PrepBag | null)[][];
  colour?: string;
  editable?: boolean;
  onBagPress?: (bagId: string) => void;
  selectedIngredients?: string[];
  // Prop from parent to control whether swipe gesture is enabled.
  isSwipeEnabled: boolean;
  isExhausted?: (ingredient: string) => boolean;
};

const COLOR_POOL = ["#f94144", "#f3722c", "#f9c74f", "#90be6d", "#43aa8b", "#577590"];
let availableColors = [...COLOR_POOL];
const dishColorMap: Record<string, string> = {};

function getRandomColor(dishName: string) {
  if (dishColorMap[dishName]) return dishColorMap[dishName];
  if (availableColors.length === 0) {
    availableColors = [...COLOR_POOL];
  }
  const index = Math.floor(Math.random() * availableColors.length);
  const chosen = availableColors[index];
  availableColors.splice(index, 1);
  dishColorMap[dishName] = chosen;
  return chosen;
}

const DraggableDish: React.FC<DraggableDishProps> = ({
  dishId,
  label,
  matrix,
  colour,
  editable,
  onBagPress,
  selectedIngredients = [],
  isSwipeEnabled,
  isExhausted,
}) => {
  const { getEffectiveCount } = useIngredientsContext();
  const cellSize = 50;
  const cellMargin = 2;
  const numRows = matrix.length;
  const numCols = numRows > 0 ? matrix[0].length : 0;
  const calculatedWidth = numCols * (cellSize + cellMargin * 2) + 20;
  const calculatedHeight = numRows * (cellSize + cellMargin * 2) + 40;
  const minWidth = 200;
  const minHeight = 150;
  const dynamicWidth = Math.max(calculatedWidth, minWidth);
  const dynamicHeight = Math.max(calculatedHeight, minHeight);
  const dishColor = colour || getRandomColor(label);
  const showMissing = false;

  // Base style for each cell.
  const baseCellStyle = { width: cellSize, height: cellSize, margin: cellMargin };

  // Local state for measuring bag cell absolute layouts.
  const [bagLayouts, setBagLayouts] = React.useState<Record<string, { x: number; y: number; width: number; height: number }>>({});
  // Local ref to track which bag cells have been triggered during a gesture.
  const activatedBagsRef = useRef<Set<string>>(new Set());
  // Ref to hold animation values for each cell.
  const cellAnimations = useRef<Record<string, Animated.Value>>({});

  const updateCellLayout = useCallback(
    (bagId: string, layout: { x: number; y: number; width: number; height: number }) => {
      setBagLayouts((prev) => ({ ...prev, [bagId]: layout }));
    },
    []
  );

  // Translation threshold to avoid accidental triggers.
  const translationThreshold = 5;

  // The pan gesture callback for the dish.
  const onGestureEvent = useCallback(
    (event: any) => {
      if (!isSwipeEnabled || selectedIngredients.length === 0) return;
      const { absoluteX, absoluteY, translationX, translationY } = event.nativeEvent;
      if (Math.abs(translationX) < translationThreshold && Math.abs(translationY) < translationThreshold)
        return;
      Object.entries(bagLayouts).forEach(([bagId, layout]) => {
        const withinX = absoluteX >= layout.x && absoluteX <= layout.x + layout.width;
        const withinY = absoluteY >= layout.y && absoluteY <= layout.y + layout.height;
        if (withinX && withinY && !activatedBagsRef.current.has(bagId)) {
          console.log("Swiped over bag:", bagId);
          activatedBagsRef.current.add(bagId);
          // Trigger cell animation.
          if (cellAnimations.current[bagId]) {
            Animated.sequence([
              Animated.timing(cellAnimations.current[bagId], { toValue: 1.1, duration: 100, useNativeDriver: true }),
              Animated.timing(cellAnimations.current[bagId], { toValue: 1, duration: 100, useNativeDriver: true }),
            ]).start();
          }
          if (onBagPress) onBagPress(bagId);
        }
      });
    },
    [bagLayouts, selectedIngredients, onBagPress, isSwipeEnabled]
  );

  const onHandlerStateChange = useCallback((event: any) => {
    if (
      event.nativeEvent.state === GestureState.BEGAN ||
      event.nativeEvent.state === GestureState.END ||
      event.nativeEvent.state === GestureState.CANCELLED
    ) {
      activatedBagsRef.current.clear();
    }
  }, []);

  const renderCell = useCallback(
    (bag: PrepBag | null, rowIndex: number, colIndex: number) => {
      if (!bag) return null;

      // Use isComplete flag directly instead of comparing counts
      const isComplete = bag.isComplete;
      const hasExhaustedIngredients = bag.ingredients.some(
        (ing) => isExhausted && isExhausted(ing.name)
      );
      
      // Only show red border for bags that need the selected ingredient AND the ingredient hasn't been added yet
      const hasSelectedIngredient = selectedIngredients.some(ing => 
        bag.ingredients.some(ingredient => ingredient.name === ing) && 
        !bag.addedIngredients.some(added => added.name === ing)
      );

      // Debug logging to help troubleshoot
      if (selectedIngredients.length > 0) {
        console.log(`Bag ID: ${bag.id}, Selected Ingredients: ${selectedIngredients.join(', ')}, Has Selected: ${hasSelectedIngredient}`);
        console.log(`Bag ingredients: ${bag.ingredients.map(i => i.name).join(', ')}`);
        console.log(`Bag added ingredients: ${bag.addedIngredients.map(i => i.name).join(', ')}`);
      }

      const cellContent = (
        <View
          key={bag.id}
          style={[
            baseCellStyle,
            styles.cell,
            isComplete && styles.completeCell,
            hasExhaustedIngredients && styles.exhaustedCell,
            hasSelectedIngredient && styles.selectedIngredientCell,
          ]}
          onLayout={(event) => {
            const { x, y, width, height } = event.nativeEvent.layout;
            updateCellLayout(bag.id, { x, y, width, height });
          }}
        >
          <Text style={styles.cellText}>
            {bag.addedIngredients.length}/{bag.ingredients.length}
          </Text>
          {isComplete && <Text style={styles.checkmark}>✓</Text>}
        </View>
      );

      return onBagPress ? (
        <TouchableOpacity
          key={bag.id}
          onPress={() => onBagPress(bag.id)}
          disabled={isComplete}
          style={styles.cellTouchable}
        >
          {cellContent}
        </TouchableOpacity>
      ) : (
        cellContent
      );
    },
    [baseCellStyle, isExhausted, onBagPress, selectedIngredients]
  );

  return (
    <PanGestureHandler
      onGestureEvent={onGestureEvent}
      onHandlerStateChange={onHandlerStateChange}
      enabled={isSwipeEnabled}
    >
      <View style={[styles.matrixWrapper, { width: dynamicWidth, height: dynamicHeight }]}>
        <Text style={styles.dishTitle} numberOfLines={1}>
          {label}
        </Text>
        <View style={styles.matrixContainer}>
          {matrix.map((row, rowIndex) => (
            <View key={rowIndex} style={styles.row}>
              {row.map((bag, colIndex) => renderCell(bag, rowIndex, colIndex))}
            </View>
          ))}
        </View>
      </View>
    </PanGestureHandler>
  );
};

const styles = StyleSheet.create({
  matrixWrapper: {
    backgroundColor: "#1e1e1e",
    padding: 10,
    borderRadius: 8,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 5,
    alignItems: "center",
    minHeight: 150,
  },
  dishTitle: {
    color: "#fff",
    fontWeight: "bold",
    marginBottom: 5,
    textAlign: "center",
    width: "100%",
  },
  matrixContainer: {
    flex: 1,
    justifyContent: "flex-start",
    alignItems: "center",
    width: "100%",
    paddingTop: 5,
  },
  row: {
    flexDirection: "row",
    justifyContent: "center",
    width: "100%",
    marginBottom: 2,
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
  cellTouchable: {
    width: '100%',
    height: '100%',
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
});

export default React.memo(DraggableDish) as React.FC<DraggableDishProps>;

import React from "react";
import { TouchableOpacity, View, Text, StyleSheet } from "react-native";
import { Dish } from "@/types/dish"; // Adjust the import if needed

interface IngredientChipProps {
  name: string;
  selected: boolean;
  onToggle: () => void;
  exhausted: boolean;
}

export const IngredientChip: React.FC<IngredientChipProps> = ({
  name,
  selected,
  onToggle,
  exhausted,
}) => {
  return (
    <TouchableOpacity
      style={[
        styles.chip,
        selected && styles.selectedChip,
        exhausted && styles.exhaustedChip,
      ]}
      onPress={onToggle}
      disabled={exhausted}
    >
      <Text style={[styles.text, exhausted && styles.exhaustedText]}>
        {name}
      </Text>
    </TouchableOpacity>
  );
};

/**
 * Checks if an ingredient is exhausted among the given dishes.
 * For each dish that includes the ingredient, the required count is the number of prep bags.
 * If the confirmed count (from addedIngredients) meets or exceeds the required count, it is exhausted.
 */
export const isIngredientExhausted = (ingredient: string, dishes: any[]): boolean => {
  // Count how many bags need this ingredient
  const bagsNeedingIngredient = dishes.flatMap(dish => 
    dish.prepBags.filter((bag: any) => 
      bag && bag.ingredients.some((ing: any) => ing.name === ingredient)
    )
  );
  
  if (bagsNeedingIngredient.length === 0) return false;
  
  // Count how many bags have this ingredient confirmed
  const confirmedBags = bagsNeedingIngredient.filter((bag: any) => 
    bag.addedIngredients.some((added: any) => added.name === ingredient)
  ).length;
  
  // Debug logging
  console.log(`isIngredientExhausted for ${ingredient}: ${confirmedBags} confirmed out of ${bagsNeedingIngredient.length} needed`);
  
  // Ingredient is exhausted if all bags that need it have it confirmed
  return confirmedBags >= bagsNeedingIngredient.length;
};

/**
 * Computes the relevant ingredients from the given dishes.
 * It builds a frequency map of ingredient occurrences and then sorts:
 * - Ingredients that are not exhausted come first.
 * - Within each group, ingredients are sorted in descending order of frequency.
 */
export const getRelevantIngredients = (dishes: any[]): string[] => {
  const ingredientSet = new Set<string>();
  const frequencyMap: Record<string, number> = {};

  dishes.forEach((dish) => {
    if (dish && dish.ingredients) {
      dish.ingredients.forEach((ing: any) => {
        if (ing && ing.name) {
          ingredientSet.add(ing.name);
          frequencyMap[ing.name] = (frequencyMap[ing.name] || 0) + 1;
        }
      });
    }
  });

  const ingredients = Array.from(ingredientSet);

  // Sort ingredients: non-exhausted first, then by frequency
  ingredients.sort((a, b) => {
    const aEx = isIngredientExhausted(a, dishes) ? 1 : 0;
    const bEx = isIngredientExhausted(b, dishes) ? 1 : 0;
    if (aEx !== bEx) return aEx - bEx;
    return (frequencyMap[b] || 0) - (frequencyMap[a] || 0);
  });

  return ingredients;
};

const styles = StyleSheet.create({
  chip: {
    backgroundColor: '#1e1e1e',
    borderRadius: 20,
    paddingHorizontal: 15,
    paddingVertical: 8,
    marginRight: 10,
    borderWidth: 1,
    borderColor: '#333',
  },
  selectedChip: {
    backgroundColor: '#4CAF50',
    borderColor: '#45a049',
  },
  exhaustedChip: {
    backgroundColor: '#333',
    borderColor: '#ff0000',
    borderWidth: 2,
    opacity: 0.7,
  },
  text: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '500',
  },
  exhaustedText: {
    color: '#ff0000',
  },
});

import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  RefreshControl,
  Dimensions,
  Alert,
  ScrollView,
  TextInput,
  Pressable,
} from 'react-native';
import Swipeable from 'react-native-gesture-handler/Swipeable';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import DraggableFlatList from 'react-native-draggable-flatlist';
import { Snackbar } from 'react-native-paper';
import fuzzysort from 'fuzzysort';
import { TouchableOpacity } from 'react-native-gesture-handler';
import { useNavigation } from '@react-navigation/native';
import { Recipe } from '@/types/recipe';
import { router } from 'expo-router';
import { useRecipeContext } from '@/context/RecipeContext';
import apiEndpoints from "@/constants/apiConfig";

const SCREEN_WIDTH = Dimensions.get('window').width;

export default function RecipePreview() {
  const { recipes, fetchRecipes, setRecipes } = useRecipeContext();
  const [refreshing, setRefreshing] = useState(false);
  const [deletedRecipe, setDeletedRecipe] = useState<Recipe | null>(null);
  const [snackbarVisible, setSnackbarVisible] = useState(false);
  const [filter, setFilter] = useState(''); // For filtering
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc'); // For sorting
  const navigation = useNavigation();

  useEffect(() => {
    fetchRecipes();
  }, []);

  // Apply filtering and sorting
  const filteredRecipes = filter
  ? fuzzysort
      .go(filter, recipes, { keys: ['name'], threshold: -10000 })
      .map((result) => result.obj)
      .sort((a, b) => {
        const comparison = a.name.localeCompare(b.name);
        return sortOrder === 'asc' ? comparison : -comparison;
      })
  : [...recipes].sort((a, b) => {
      const comparison = a.name.localeCompare(b.name);
      return sortOrder === 'asc' ? comparison : -comparison;
    });

    const handleDelete = async (id: string) => {
      try {
        const response = await fetch(`${apiEndpoints.recipes}/${id}`, {
          method: "DELETE",
        });
    
        if (!response.ok) {
          throw new Error("Failed to delete recipe.");
        }
      } catch (error) {
        console.error("Error deleting recipe:", error);
      }
    };

  const handleScaleRecipe = (recipe: Recipe) => {
    router.push({
      pathname: "/scale",
      params: { recipe: JSON.stringify(recipe) },
    });
  };

  const handleUndo = async () => {
    if (!deletedRecipe) return;

    try {
      const response = await fetch(`${apiEndpoints.recipes}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(deletedRecipe),
      });

      if (!response.ok) {
        throw new Error('Failed to restore recipe.');
      }

      setRecipes((prev) => [deletedRecipe, ...prev]);
      setDeletedRecipe(null);
    } catch (error) {
      console.error('Error restoring recipe:', error);
    }
    setSnackbarVisible(false);
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchRecipes();
    setRefreshing(false);
  };

  const handleRecipeClick = (recipe: Recipe | undefined | null) => {
    if (!recipe) {
      Alert.alert('Error', 'Invalid recipe data.');
      return;
    }

    router.push({
      pathname: '/recipe-detail',
      params: { recipe: JSON.stringify(recipe) },
    });
  };

  const renderSwipeRight = () => (
    <View style={[styles.swipeAction, styles.swipeRight]}>
      <Text style={styles.swipeText}>Scale</Text>
    </View>
  );

  const renderSwipeLeft = () => (
    <View style={[styles.swipeAction, styles.swipeLeft]}>
      <Text style={styles.swipeText}>Delete</Text>
    </View>
  );

  const renderRecipe = ({ item, drag }: { item: Recipe; drag: () => void }) => {
    let swipeableRef: Swipeable | null = null;
  
    const handleDeleteWithReset = async () => {
      await handleDelete(item._id); // Perform delete
      swipeableRef?.close(); // Reset swipeable
      setRecipes((prev) => prev.filter((recipe) => recipe._id !== item._id)); // Update list
    };
  
    const handleScaleWithReset = () => {
      handleScaleRecipe(item); // Navigate to Scale tab
      swipeableRef?.close(); // Reset swipeable
    };
  
    return (
      <Swipeable
        ref={(ref) => (swipeableRef = ref)} // Capture swipeable reference
        renderLeftActions={renderSwipeRight} // Right swipe action
        renderRightActions={renderSwipeLeft} // Left swipe action
        onSwipeableRightOpen={handleDeleteWithReset} // Delete recipe
        onSwipeableLeftOpen={handleScaleWithReset} // Open scaling tab
        friction={2}
        overshootLeft={false}
        overshootRight={false}
      >
        <TouchableOpacity
          onPress={() => handleRecipeClick(item)}
          onLongPress={drag}
          style={styles.recipeCard}
        >
          <Text style={styles.recipeName}>{item.name}</Text>
          <Text style={styles.portion}>Portion: {item.originalPortion}</Text>
        </TouchableOpacity>
      </Swipeable>
    );
  };
  

  return (
    <GestureHandlerRootView style={styles.container}>
      {/* Filter and Sort Controls */}
      <View style={styles.filterSortContainer}>
        <TextInput
          style={styles.filterInput}
          placeholder="Search recipes..."
          placeholderTextColor="#777"
          onChangeText={setFilter}
        />
        <Pressable
          style={styles.sortButton}
          onPress={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
        >
          <Text style={styles.sortButtonText}>Sort: {sortOrder === 'asc' ? 'A-Z' : 'Z-A'}</Text>
        </Pressable>
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={{ paddingBottom: 100 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
      >
        {filteredRecipes.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>No recipes match your search.</Text>
          </View>
        ) : (
          <DraggableFlatList
            data={filteredRecipes}
            keyExtractor={(item) => item._id}
            onDragEnd={({ data }) => setRecipes(data)}
            renderItem={renderRecipe}
          />
        )}
      </ScrollView>

      <Snackbar
        visible={snackbarVisible}
        onDismiss={() => setSnackbarVisible(false)}
        action={{
          label: 'Undo',
          onPress: handleUndo,
        }}
      >
        Recipe deleted.
      </Snackbar>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#121212',
    padding: 10,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#ffffff',
    textAlign: 'center',
    marginBottom: 10,
  },
  scrollView: {
    flex: 1,
  },
  recipeCard: {
    backgroundColor: '#1e1e1e', // Matches app's background
    borderRadius: 10,
    padding: 15,
    marginBottom: 15,
    height: 100,
    justifyContent: 'center',
    borderWidth: 1, // Subtle border
    borderColor: '#333', // Darker border color
    shadowColor: '#000', // Shadow for elevation
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 3,
    elevation: 3, // Android-specific shadow
  },
  recipeName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#4CAF50', // Accent color matching buttons
  },
  portion: {
    fontSize: 14,
    color: '#b0b0b0', // Subtle text color
    marginTop: 5,
  },
  swipeAction: {
    justifyContent: 'center',
    alignItems: 'center',
    width: SCREEN_WIDTH * 0.3,
    height: 100, // Matches card height
  },
  swipeRight: {
    backgroundColor: '#28a745',
  },
  swipeLeft: {
    backgroundColor: '#dc3545',
  },
  swipeText: {
    color: '#ffffff',
    fontWeight: 'bold',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 10,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#b0b0b0',
    textAlign: 'center',
  },
  filterSortContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 15,
  },
  filterInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#444',
    backgroundColor: '#1e1e1e',
    color: '#ffffff',
    borderRadius: 5,
    padding: 10,
    marginRight: 10,
  },
  sortButton: {
    backgroundColor: '#4CAF50',
    borderRadius: 5,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 10,
  },
  sortButtonText: {
    color: '#ffffff',
    fontWeight: 'bold',
  },
});

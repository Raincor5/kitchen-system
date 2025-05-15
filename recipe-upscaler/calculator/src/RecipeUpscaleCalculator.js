// Import necessary React libraries and hooks
import React, { useState } from 'react';

const RecipeUpscaleCalculator = () => {
    // State to hold the recipe data
    const [recipe, setRecipe] = useState({
        name: '',
        ingredients: [{ name: '', weight: 0 }],
        originalPortion: 1,
    });
    const [desiredPortion, setDesiredPortion] = useState(1);
    const [scaledIngredients, setScaledIngredients] = useState([]);
    const [savedRecipes, setSavedRecipes] = useState([]); // State to store saved recipes

    // Function to handle recipe input change
    const handleInputChange = (index, field, value) => {
        const updatedIngredients = [...recipe.ingredients];
        updatedIngredients[index][field] = value;
        setRecipe({ ...recipe, ingredients: updatedIngredients });
    };

    // Function to add a new ingredient field
    const addIngredient = () => {
        setRecipe({
            ...recipe,
            ingredients: [...recipe.ingredients, { name: '', weight: 0 }],
        });
    };

    // Function to scale the recipe
    const scaleRecipe = () => {
        const scaleFactor = desiredPortion / recipe.originalPortion;
        const updatedIngredients = recipe.ingredients.map((ingredient) => ({
            name: ingredient.name,
            weight: ingredient.weight * scaleFactor,
        }));
        setScaledIngredients(updatedIngredients);
    };

    // Function to save the upscaled recipe
    const saveRecipe = () => {
        if (recipe.name && scaledIngredients.length > 0) {
            const newRecipe = {
                name: recipe.name,
                ingredients: scaledIngredients,
                portion: desiredPortion,
            };
            setSavedRecipes([...savedRecipes, newRecipe]);
            alert('Recipe saved successfully!');
        } else {
            alert('Please scale the recipe and provide a name before saving.');
        }
    };

    return (
        <div style={{ padding: '20px', maxWidth: '600px', margin: 'auto' }}>
            <h1>Recipe Upscale Calculator</h1>

            <div style={{ marginBottom: '20px' }}>
                <label>
                    Recipe Name:
                    <input
                        type="text"
                        value={recipe.name}
                        onChange={(e) => setRecipe({ ...recipe, name: e.target.value })}
                        style={{ marginLeft: '10px', width: '100%' }}
                    />
                </label>
            </div>

            <div>
                <h3>Ingredients</h3>
                {recipe.ingredients.map((ingredient, index) => (
                    <div key={index} style={{ marginBottom: '10px' }}>
                        <input
                            type="text"
                            placeholder="Ingredient Name"
                            value={ingredient.name}
                            onChange={(e) => handleInputChange(index, 'name', e.target.value)}
                            style={{ marginRight: '10px' }}
                        />
                        <input
                            type="number"
                            placeholder="Weight"
                            value={ingredient.weight}
                            onChange={(e) => handleInputChange(index, 'weight', parseFloat(e.target.value))}
                            style={{ width: '80px' }}
                        />
                    </div>
                ))}
                <button onClick={addIngredient} style={{ marginTop: '10px' }}>
                    Add Ingredient
                </button>
            </div>

            <div style={{ marginTop: '20px' }}>
                <label>
                    Original Portion Count:
                    <input
                        type="number"
                        value={recipe.originalPortion}
                        onChange={(e) => setRecipe({ ...recipe, originalPortion: parseFloat(e.target.value) })}
                        style={{ marginLeft: '10px', width: '80px' }}
                    />
                </label>
            </div>

            <div style={{ marginTop: '20px' }}>
                <label>
                    Desired Portion Count:
                    <input
                        type="number"
                        value={desiredPortion}
                        onChange={(e) => setDesiredPortion(parseFloat(e.target.value))}
                        style={{ marginLeft: '10px', width: '80px' }}
                    />
                </label>
            </div>

            <button
                onClick={scaleRecipe}
                style={{ marginTop: '20px', padding: '10px', backgroundColor: 'blue', color: 'white' }}
            >
                Scale Recipe
            </button>

            <button
                onClick={saveRecipe}
                style={{ marginTop: '20px', padding: '10px', backgroundColor: 'green', color: 'white', marginLeft: '10px' }}
            >
                Save Recipe
            </button>

            {scaledIngredients.length > 0 && (
                <div style={{ marginTop: '30px' }}>
                    <h3>Scaled Ingredients</h3>
                    <ul>
                        {scaledIngredients.map((ingredient, index) => (
                            <li key={index}>
                                {ingredient.name}: {ingredient.weight.toFixed(2)}
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            {savedRecipes.length > 0 && (
                <div style={{ marginTop: '30px' }}>
                    <h3>Saved Recipes</h3>
                    <ul>
                        {savedRecipes.map((savedRecipe, index) => (
                            <li key={index}>
                                <strong>{savedRecipe.name}</strong> (Portion: {savedRecipe.portion})
                                <ul>
                                    {savedRecipe.ingredients.map((ingredient, idx) => (
                                        <li key={idx}>
                                            {ingredient.name}: {ingredient.weight.toFixed(2)}
                                        </li>
                                    ))}
                                </ul>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default RecipeUpscaleCalculator;

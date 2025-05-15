import React, { useState } from 'react';

const RecipeBuilder = () => {
    const [recipe, setRecipe] = useState({
        name: '',
        originalPortion: 1, // Default to 1
        ingredients: [{ name: '', weight: 0, unit: '' }],
    });

    // Update individual ingredient fields
    const handleInputChange = (index, field, value) => {
        const updatedIngredients = [...recipe.ingredients];
        updatedIngredients[index][field] = value;
        setRecipe({ ...recipe, ingredients: updatedIngredients });
    };

    // Add a new empty ingredient
    const addIngredient = () => {
        setRecipe({
            ...recipe,
            ingredients: [...recipe.ingredients, { name: '', weight: 0, unit: '' }],
        });
    };

    // Save the recipe to the backend
    const saveRecipe = () => {
        console.log('Sending recipe to backend:', recipe); // Debugging

        fetch('http://localhost:5000/api/recipes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(recipe),
        })
            .then((response) => {
                if (response.ok) {
                    alert('Recipe saved successfully!');
                    setRecipe({ name: '', originalPortion: 1, ingredients: [{ name: '', weight: 0, unit: '' }] });
                } else {
                    return response.text().then((text) => {
                        throw new Error(text);
                    });
                }
            })
            .catch((error) => {
                console.error('Error saving recipe:', error);
                alert(`Failed to save recipe: ${error.message}`);
            });
    };

    return (
        <div>
            <h2>Recipe Builder</h2>

            {/* Recipe Name */}
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

            {/* Original Portion */}
            <div style={{ marginBottom: '20px' }}>
                <label>
                    Original Portion:
                    <input
                        type="number"
                        value={recipe.originalPortion}
                        onChange={(e) => setRecipe({ ...recipe, originalPortion: parseFloat(e.target.value) })}
                        style={{ marginLeft: '10px', width: '80px' }}
                    />
                </label>
            </div>

            {/* Ingredients */}
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
                        style={{ width: '80px', marginRight: '10px' }}
                    />
                    <select
                        value={ingredient.unit}
                        onChange={(e) => handleInputChange(index, 'unit', e.target.value)}
                        style={{ marginRight: '10px' }}
                    >
                        <option value="">Unit</option>
                        <option value="g">g</option>
                        <option value="kg">kg</option>
                        <option value="ml">ml</option>
                        <option value="l">l</option>
                        <option value="tbsp">tbsp</option>
                        <option value="tsp">tsp</option>
                    </select>
                </div>
            ))}
            <button onClick={addIngredient} style={{ marginTop: '10px' }}>
                Add Ingredient
            </button>

            {/* Save Button */}
            <button
                onClick={saveRecipe}
                style={{
                    marginTop: '20px',
                    padding: '10px',
                    backgroundColor: 'green',
                    color: 'white',
                }}
            >
                Save Recipe
            </button>
        </div>
    );
};

export default RecipeBuilder;

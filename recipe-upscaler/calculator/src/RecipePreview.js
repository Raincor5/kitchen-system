// RecipePreview.js
import React, { useState, useEffect } from 'react';

const RecipePreview = () => {
    const [recipes, setRecipes] = useState([]); // Store fetched recipes
    const [selectedRecipe, setSelectedRecipe] = useState(null);

    // Fetch recipes from the backend on component mount
    useEffect(() => {
        const fetchRecipes = async () => {
            try {
                const response = await fetch('http://localhost:5000/api/recipes'); // Backend API to fetch recipes
                if (!response.ok) {
                    throw new Error('Failed to fetch recipes');
                }
                const data = await response.json();
                setRecipes(data); // Store recipes in the state
            } catch (error) {
                console.error('Error fetching recipes:', error);
                alert('Failed to fetch recipes.');
            }
        };

        fetchRecipes(); // Fetch recipes when the component is mounted
    }, []); // Empty dependency array ensures this runs only once

    // Handle recipe selection for preview
    const handleRecipeSelect = (recipe) => {
        setSelectedRecipe(recipe); // Set selected recipe for preview
    };

    return (
        <div>
            <h2>Recipe Preview</h2>

            {/* Dropdown to select recipe */}
            <div style={{ marginBottom: '20px' }}>
                <label>
                    Select Recipe:
                    <select
                        onChange={(e) => {
                            const selected = JSON.parse(e.target.value);
                            handleRecipeSelect(selected); // Update selected recipe
                        }}
                        style={{ marginLeft: '10px' }}
                    >
                        <option value="">-- Select --</option>
                        {recipes.map((recipe) => (
                            <option key={recipe._id} value={JSON.stringify(recipe)}>
                                {recipe.name}
                            </option>
                        ))}
                    </select>
                </label>
            </div>

            {/* Preview the selected recipe */}
            {selectedRecipe && (
                <div style={{ marginTop: '30px' }}>
                    <h3>{selectedRecipe.name}</h3>
                    <p><strong>Original Portion:</strong> {selectedRecipe.originalPortion}</p>
                    <h4>Ingredients:</h4>
                    <ul>
                        {selectedRecipe.ingredients.map((ingredient, index) => (
                            <li key={index}>
                                {ingredient.name} - {ingredient.weight} {ingredient.unit}
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default RecipePreview;

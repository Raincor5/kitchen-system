import React, { useState, useEffect } from "react";

const RecipeUpscaler = () => {
    const [recipes, setRecipes] = useState([]);
    const [selectedRecipe, setSelectedRecipe] = useState(null);
    const [scalingMode, setScalingMode] = useState("portion");
    const [parameters, setParameters] = useState({});
    const [scaledIngredients, setScaledIngredients] = useState([]);

    useEffect(() => {
        const fetchRecipes = async () => {
            try {
                const response = await fetch("http://localhost:5000/api/recipes");
                if (!response.ok) {
                    throw new Error("Failed to fetch recipes");
                }
                const data = await response.json();
                setRecipes(data);
            } catch (error) {
                console.error("Error fetching recipes:", error);
                alert("Failed to fetch recipes.");
            }
        };

        fetchRecipes();
    }, []);

    const scaleRecipe = async () => {
        if (!selectedRecipe) {
            alert("Please select a recipe first!");
            return;
        }

        try {
            const response = await fetch("http://localhost:5000/api/scale", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    recipe: selectedRecipe,
                    scalingMode,
                    parameter: parameters,
                }),
            });

            if (!response.ok) {
                throw new Error("Failed to scale the recipe");
            }

            const data = await response.json();
            setScaledIngredients(data.scaledIngredients);
        } catch (error) {
            console.error("Error scaling recipe:", error);
            alert("Failed to scale the recipe.");
        }
    };

    return (
        <div>
            <h2>Recipe Upscaler</h2>

            {/* Recipe Selection */}
            <div>
                <label>
                    Select Recipe:
                    <select onChange={(e) => setSelectedRecipe(JSON.parse(e.target.value))}>
                        <option value="">-- Select --</option>
                        {recipes.map((recipe) => (
                            <option key={recipe._id} value={JSON.stringify(recipe)}>
                                {recipe.name}
                            </option>
                        ))}
                    </select>
                </label>
            </div>

            {/* Scaling Mode Selection */}
            <div>
                <label>
                    Scaling Mode:
                    <select onChange={(e) => setScalingMode(e.target.value)}>
                        <option value="portion">Scale by Portion</option>
                        <option value="availability">Scale by Availability</option>
                        <option value="unitConversion">Dynamic Unit Conversion</option>
                    </select>
                </label>
            </div>

            {/* Parameter Inputs */}
            {scalingMode === "portion" && (
                <div>
                    <label>
                        Desired Portion:
                        <input
                            type="number"
                            onChange={(e) =>
                                setParameters({ desiredPortion: parseFloat(e.target.value) })
                            }
                        />
                    </label>
                </div>
            )}
            {scalingMode === "availability" && selectedRecipe && (
                <div>
                    <label>
                        Ingredient Name:
                        <select
                            onChange={(e) =>
                                setParameters((prev) => ({
                                    ...prev,
                                    availableIngredientName: e.target.value,
                                }))
                            }
                        >
                            <option value="">-- Select Ingredient --</option>
                            {selectedRecipe.ingredients.map((ing, index) => (
                                <option key={index} value={ing.name}>
                                    {ing.name}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Available Weight:
                        <input
                            type="number"
                            onChange={(e) =>
                                setParameters((prev) => ({
                                    ...prev,
                                    availableWeight: parseFloat(e.target.value),
                                }))
                            }
                        />
                    </label>
                </div>
            )}
            {scalingMode === "unitConversion" && selectedRecipe && (
                <div>
                    <label>
                        Ingredient Name:
                        <select
                            onChange={(e) =>
                                setParameters((prev) => ({
                                    ...prev,
                                    ingredientName: e.target.value,
                                }))
                            }
                        >
                            <option value="">-- Select Ingredient --</option>
                            {selectedRecipe.ingredients.map((ing, index) => (
                                <option key={index} value={ing.name}>
                                    {ing.name}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Conversion Factor:
                        <input
                            type="number"
                            onChange={(e) =>
                                setParameters((prev) => ({
                                    ...prev,
                                    factor: parseFloat(e.target.value),
                                }))
                            }
                        />
                    </label>
                    <label>
                        To Unit:
                        <input
                            type="text"
                            onChange={(e) =>
                                setParameters((prev) => ({
                                    ...prev,
                                    toUnit: e.target.value,
                                }))
                            }
                        />
                    </label>
                </div>
            )}

            {/* Scale Button */}
            <button onClick={scaleRecipe}>Scale Recipe</button>

            {/* Scaled Ingredients */}
            {scaledIngredients.length > 0 && (
                <div>
                    <h3>Scaled Ingredients</h3>
                    <ul>
                        {scaledIngredients.map((ingredient, index) => (
                            <li key={index}>
                                {ingredient.name}: {ingredient.scaledWeight} {ingredient.unit}
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default RecipeUpscaler;

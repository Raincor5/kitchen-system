// App.js
import React, { useState } from 'react';
import { BrowserRouter as Router, Route, Routes, Link } from 'react-router-dom';
import RecipeBuilder from './RecipeBuilder';
import RecipeUpscaler from './RecipeUpscaler';
import AIIntegration from './AIIntegration';
import RecipePreview from './RecipePreview'; // Import the new Preview component

const App = () => {
    const [recipe, setRecipe] = useState(null); // Hold the current recipe state

    return (
        <Router>
            <div style={{ padding: '20px', maxWidth: '800px', margin: 'auto' }}>
                <h1>Recipe Management System</h1>
                <nav style={{ marginBottom: '20px' }}>
                    <Link to="/builder" style={{ marginRight: '15px' }}>Recipe Builder</Link>
                    <Link to="/upscaler" style={{ marginRight: '15px' }}>Recipe Upscaler</Link>
                    <Link to="/preview" style={{ marginRight: '15px' }}>Recipe Preview</Link> {/* Add Preview Link */}
                    <Link to="/ai">AI Integration</Link>
                </nav>

                <Routes>
                    <Route path="/builder" element={<RecipeBuilder setRecipe={setRecipe} />} />
                    <Route path="/upscaler" element={<RecipeUpscaler setRecipe={setRecipe} />} />
                    <Route path="/ai" element={<AIIntegration />} />
                    <Route path="/preview" element={<RecipePreview recipe={recipe} />} /> {/* Preview Route */}
                </Routes>
            </div>
        </Router>
    );
};

export default App;

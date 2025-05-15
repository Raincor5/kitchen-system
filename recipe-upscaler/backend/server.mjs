// A node server that uses Express to create an API for scaling recipes and processing images to extract recipes.

// Import necessary modules
import express from "express";
import bodyParser from "body-parser";
import cors from "cors";
import mongoose from "mongoose";
import dotenv from "dotenv";
import OpenAI from "openai";

// Load environment variables
dotenv.config();

// Initialize OpenAI Client
const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY,
});

const app = express();
const PORT = 5000;

// Middleware
app.use(cors({
    origin: ['http://localhost:3000', 'http://localhost:8080', 'https://c8a5a64f5ab8.ngrok.app', 'https://*.ngrok.app'],
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true
}));

// Add error handling middleware
app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(500).json({
        error: 'Internal Server Error',
        message: err.message
    });
});

// Add request logging middleware
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
    next();
});

app.use(bodyParser.json());

// Connect to MongoDB
const MONGODB_URI = process.env.MONGODB_URI || "mongodb://localhost:27017/recipes";
mongoose.connect(MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
});

const db = mongoose.connection;
db.on("error", console.error.bind(console, "connection error:"));
db.once("open", () => {
    console.log("Connected to MongoDB");
});

// Define a Recipe schema
const recipeSchema = new mongoose.Schema({
    name: { type: String, required: true },
    originalPortion: { type: Number, required: true },
    ingredients: [
        {
            name: { type: String, required: true },
            weight: { type: Number, required: true },
            unit: { type: String, required: true },
        },
    ],
    steps: { type: [String], required: true },
    // Add optional colour
    colour: { type: String, default: null }
});


const Recipe = mongoose.model("Recipe", recipeSchema);

// Endpoint to scale recipes
app.post("/api/scale", async (req, res) => {
    const { recipe, scalingMode, parameter } = req.body;
  
    if (!recipe || !scalingMode || parameter === undefined) {
      console.error("Invalid request payload:", req.body);
      return res.status(400).send("Invalid request. Please provide all required fields.");
    }
  
    try {
      // Fetch recipe details from the database
      const recipeData = await Recipe.findById(recipe);
      if (!recipeData) {
        return res.status(404).send("Recipe not found.");
      }
  
      let scaledIngredients = [];
  
      if (scalingMode === "availability") {
        const { availableIngredientName, availableWeight, desiredUnit } = parameter;
  
        // Find the scaling ingredient
        const scalingIngredient = recipeData.ingredients.find(
          (ingredient) => ingredient.name === availableIngredientName
        );
  
        if (!scalingIngredient) {
          return res.status(404).send("Scaling ingredient not found.");
        }
  
        let scalingFactor;
  
        // Step 1: Check for matching units
        if (scalingIngredient.unit === desiredUnit) {
          // Units match; calculate scaling factor directly
          scalingFactor = availableWeight / scalingIngredient.weight;
          console.log("Units match. Scaling Factor:", scalingFactor);
        } else {
          // Step 2: Perform unit conversion using ChatGPT
          const conversionPrompt = `
          You are an expert in unit conversion and recipe scaling. Help convert and adjust a recipe based on the availability of one ingredient.
  
          - The recipe requires the following ingredient:
            - Name: "${scalingIngredient.name}"
            - Amount: "${scalingIngredient.weight}" "${scalingIngredient.unit}".
            - Available: "${availableWeight}" "${desiredUnit}".
            - Note: The density or conversion factor may depend on cooking methods or temperature.
  
          Step 1: Identify the relationship (e.g., density) to convert the units.
          Step 2: Convert the ingredient's amount to match the available unit.
          Step 3: Calculate the scaling factor based on the converted amount.
  
          Provide the adjusted scaling factor and density as valid JSON in this format:
          {
            "density": Density (if applicable),
            "scalingFactor": Scaling Factor
          }
  
          Do not include any other text, explanations, or formatting.
          `;
  
          console.log("Conversion Prompt:", conversionPrompt);
  
          const response = await openai.chat.completions.create({
            model: "gpt-4o-mini",
            messages: [
              { role: "system", content: "You are an expert in unit conversion and recipe scaling." },
              { role: "user", content: conversionPrompt },
            ],
          });
  
          const rawContent = response.choices[0]?.message?.content;
  
          if (!rawContent.includes("{")) {
            throw new Error("Invalid response format from ChatGPT.");
          }
  
          const { density, scalingFactor: convertedScalingFactor } = JSON.parse(rawContent);
  
          console.log("Density Calculated:", density);
          console.log("Scaling Factor from Conversion:", convertedScalingFactor);
  
          scalingFactor = convertedScalingFactor;
        }
  
        // Step 3: Scale the entire recipe based on the scaling factor
        scaledIngredients = recipeData.ingredients.map((ingredient) => ({
          name: ingredient.name,
          scaledWeight: ingredient.weight * scalingFactor,
          unit: ingredient.unit,
        }));
  
        console.log("Final Scaled Ingredients:", scaledIngredients);
      } else if (scalingMode === "portion") {
        // Scaling by portion
        const scalingFactor = parameter.desiredPortion / recipeData.originalPortion;
        scaledIngredients = recipeData.ingredients.map((ingredient) => ({
          name: ingredient.name,
          scaledWeight: ingredient.weight * scalingFactor,
          unit: ingredient.unit,
        }));
      } else {
        return res.status(400).send("Invalid scaling mode.");
      }
  
      res.status(200).json({ scaledIngredients });
    } catch (error) {
      console.error("Error scaling recipe:", error.message || error);
      res.status(500).send("Failed to scale recipe.");
    }
  });
  



// Endpoint to add recipes with original portion
app.post("/api/recipes", async (req, res) => {
    const { name, ingredients, originalPortion } = req.body;

    if (!name || !Array.isArray(ingredients) || !originalPortion) {
        return res.status(400).send("Invalid request. Please provide name, ingredients, and original portion.");
    }

    try {
        const recipe = new Recipe({ name, ingredients, originalPortion });
        await recipe.save();

        console.log("Recipe saved to database:", recipe);
        res.status(201).send("Recipe added successfully.");
    } catch (error) {
        console.error("Error adding recipe:", error);
        res.status(500).send("Failed to add recipe. Please try again later.");
    }
});

// Endpoint to fetch all recipes
app.get('/api/recipes', async (req, res) => {
    try {
        const recipes = await Recipe.find();
        console.log('Fetched Recipes:', recipes); // Debugging log
        res.status(200).json(recipes);
    } catch (error) {
        console.error('Error fetching recipes:', error);
        res.status(500).json({ error: 'Failed to fetch recipes.' });
    }
});

// Endpoint to delete a recipe
app.delete('/api/recipes/:id', async (req, res) => {
    try {
        const { id } = req.params;
        await Recipe.findByIdAndDelete(id);
        res.status(200).json({ message: 'Recipe deleted successfully.' });
    } catch (error) {
        console.error('Error deleting recipe:', error);
        res.status(500).json({ error: 'Failed to delete recipe.' });
    }
});


// Endpoint to process image and populate database
import multer from "multer";
import fs from "fs";
import sharp from "sharp";
import vision from "@google-cloud/vision";

// Configure multer for image upload
const upload = multer({ dest: "uploads/" });

// Initialize the Google Vision client
const client = new vision.ImageAnnotatorClient({
    keyFilename: "acc.json", // Replace with your key file path
});

app.post("/api/process-image", upload.single("image"), async (req, res) => {
  const filePath = req.file.path;
  const compressedFilePath = `${filePath}-compressed.jpg`;

  try {
    // Resize & compress
    await sharp(filePath)
      .resize({ width: 800 }) 
      .jpeg({ quality: 80 })
      .toFile(compressedFilePath);

    // OCR
    const [result] = await client.textDetection(compressedFilePath);
    const detectedText = result.fullTextAnnotation?.text;
    if (!detectedText) {
      return res.status(400).send("No text detected in the image.");
    }
    console.log("Detected OCR Text:", detectedText);

    // ChatGPT extraction
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [
        { role: "system", content: "You are an assistant that extracts recipes and formats them as valid JSON." },
        {
          role: "user",
          content: `Extract the text into JSON with these keys:
- 'name': Dish name (string)
- 'originalPortion': Number of portions (number)
- 'ingredients': array of:
  - 'name' (string),
  - 'weight' (number),
  - 'unit' (string)
- 'steps': array of instructions (strings)
- 'colour': a hex color code that semantically fits the dish name (like "#FF0000" if the dish name implies 'red hot')

If data is missing, fill placeholders:
- "Unknown Ingredient", 0, "unitless"
- 'colour': null if you cannot determine

But we will discard the entire recipe if placeholders appear, so do your best.Return only valid JSON.
Do not include any code block markers (e.g., \`\`\`json).`
        },
        { role: "user", content: detectedText },
      ],
    });
    console.log("GPT response:", JSON.stringify(response, null, 2));

    if (!response.choices[0]?.message?.content.includes("{")) {
      throw new Error("Invalid response format from ChatGPT.");
    }
    const extractedData = JSON.parse(response.choices[0].message.content);

    // Validate base fields
    if (!extractedData.name || !extractedData.originalPortion ||
        !extractedData.ingredients || !extractedData.steps) {
      return res.status(400).send("Failed to extract recipe data (missing fields).");
    }

    // *** Check for placeholders/invalid values => discard entire recipe ***
    // e.g., name is "Unknown Dish", any ingredient = "Unknown Ingredient", weight=0, unit="unitless", or steps contain "No step provided."
    if (isRecipeInvalid(extractedData)) {
      return res.status(400).send("Discarding recipe: ChatGPT returned null/placeholder fields.");
    }

    // Everything is valid => Save the recipe
    const recipe = new Recipe(extractedData);
    await recipe.save();

    console.log("Saved recipe:", extractedData);
    return res.status(201).json({
      message: "Recipe processed and saved successfully.",
      recipe: extractedData,
    });
  } catch (error) {
    console.error("Error processing image:", error.message || error);
    res.status(500).send("Failed to process image and extract recipe.");
  } finally {
    // clean up
    fs.unlinkSync(filePath);
    if (fs.existsSync(compressedFilePath)) {
      fs.unlinkSync(compressedFilePath);
    }
  }
});

// Helper to detect placeholders or missing data => discard the entire recipe
function isRecipeInvalid(data) {
  // Name placeholders or null?
  if (!data.name || data.name === "Unknown Dish") return true;
  if (!data.originalPortion || data.originalPortion <= 0) return true;

  // Check ingredients for placeholders
  if (!Array.isArray(data.ingredients) || !data.ingredients.length) return true;
  for (const ing of data.ingredients) {
    if (!ing || !ing.name || !ing.weight || !ing.unit) {
      return true; 
    }
    if (
      ing.name === "Unknown Ingredient" ||
      ing.weight === 0 ||
      ing.unit === "unitless"
    ) {
      return true;
    }
  }

  // Check steps for placeholders
  if (!Array.isArray(data.steps) || !data.steps.length) return true;
  for (const step of data.steps) {
    if (!step || step === "No step provided.") {
      return true;
    }
  }

  // Passed all checks => valid
  return false;
}


app.post("/api/process-image-multi", upload.single("image"), async (req, res) => {
  const filePath = req.file.path;
  const compressedFilePath = `${filePath}-compressed.jpg`;

  try {
      // Resize and compress the image
      await sharp(filePath)
          .resize({ width: 800 }) // Resize to a max width of 800 pixels
          .jpeg({ quality: 80 }) // Compress the image with 80% quality
          .toFile(compressedFilePath);

      // Perform OCR using Google Vision API
      const [result] = await client.textDetection(compressedFilePath);
      const detectedText = result.fullTextAnnotation?.text;

      if (!detectedText) {
          return res.status(400).send("No text detected in the image.");
      }

      console.log("Detected OCR Text:", detectedText);

      // Use ChatGPT API to format the text into structured JSON
      const response = await openai.chat.completions.create({
          model: "gpt-4o",
          messages: [
              { role: "system", content: "You are an assistant that extracts recipes and formats them as valid JSON." },
              {
                  role: "user",
                  content: `Extract and format each dish found in the text into an array of JSON objects. For each dish, use these keys:
- 'name': Dish name (string)
- 'originalPortion': Number of portions (integer)
- 'ingredients': An array of objects with:
  - 'name' (string)
  - 'weight' (number)
  - 'unit' (string)
- 'steps': An array of strings

If any data is missing, use default values (e.g., 'Unknown Ingredient' for name, 0 for weight, 'unitless' for unit, and an empty array for steps). Do not include code block markers. Return only valid JSON.
Do not include any code block markers (e.g., \`\`\`json).
`,
              },
              { role: "user", content: detectedText },
          ],
      });
      console.log("Returned GPT response: ", JSON.stringify(response, null, 2))
      // Parse the response and save the recipe to the database
      if (!response.choices[0]?.message?.content.includes("{")) {
          throw new Error("Invalid response format from ChatGPT.");
      }        
      const extractedData = JSON.parse(response.choices[0]?.message?.content);
      const { name, originalPortion, ingredients, steps } = extractedData;

      if (!name || !originalPortion || !ingredients || !steps) {
          return res.status(400).send("Failed to extract recipe data from the OCR text.");
      }

      const recipe = new Recipe({ name, originalPortion, ingredients, steps });
      await recipe.save();

      console.log("Extracted Recipe:", extractedData);
      res.status(201).json({ message: "Recipe processed and saved successfully.", recipe: extractedData });
  } catch (error) {
      console.error("Error processing image:", error.message || error);
      res.status(500).send("Failed to process image and extract recipe.");
  } finally {
      // Clean up uploaded files
      fs.unlinkSync(filePath);
      if (fs.existsSync(compressedFilePath)) {
          fs.unlinkSync(compressedFilePath);
      }
  }
});

// Add a simple printer status endpoint
app.get("/api/labels/printer/status", (req, res) => {
    console.log("Printer status GET request received");
    res.json({ status: "online", message: "Printer is ready" });
});

app.post("/api/labels/printer/status", (req, res) => {
    console.log("Printer status POST request received");
    res.json({ status: "online", message: "Printer is ready" });
});

// Add a simple prep-tracking endpoint
app.get("/api/prep-tracking/receive-prep-data", (req, res) => {
    console.log("Prep data GET request received");
    res.json({ success: true, message: "Prep data endpoint is available" });
});

app.post("/api/prep-tracking/receive-prep-data", (req, res) => {
    console.log("Prep data POST request received:", req.body);
    res.json({ success: true, message: "Prep data received successfully" });
});

// Add recipes endpoint that properly expands ingredients
app.get("/api/recipes", async (req, res) => {
    try {
        const recipes = await Recipe.find({});
        // Expand the ingredients array for each recipe
        const expandedRecipes = recipes.map(recipe => {
            const recipeObj = recipe.toObject();
            return {
                ...recipeObj,
                ingredients: recipeObj.ingredients.map(ing => ({
                    name: ing.name,
                    weight: ing.weight,
                    unit: ing.unit
                }))
            };
        });
        console.log("Fetched Recipes:", JSON.stringify(expandedRecipes, null, 2));
        res.json(expandedRecipes);
    } catch (error) {
        console.error("Error fetching recipes:", error);
        res.status(500).json({ error: "Failed to fetch recipes" });
    }
});

// Start the server
app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});

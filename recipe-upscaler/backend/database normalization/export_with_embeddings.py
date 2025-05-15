#!/usr/bin/env python3
import os
import json
from pymongo import MongoClient
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"  # Avoid TensorFlow warnings
os.environ["TRANSFORMERS_NO_TF"] = "1"  # Avoid TensorFlow warnings

from sentence_transformers import SentenceTransformer, util
import numpy as np

# 1. Connect to Mongo
MONGO_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017/recipes")
client = MongoClient(MONGO_URI)
db = client["recipes"]
recipes_coll = db["recipes"]

# 2. Fetch all unique ingredient names
all_ingredients = set()
all_recipes = list(recipes_coll.find())
for recipe in all_recipes:
    for ing in recipe.get("ingredients", []):
        name = ing.get("name", "").strip()
        if name:
            all_ingredients.add(name)

all_ingredients = sorted(all_ingredients)
print(f"Found {len(all_ingredients)} unique ingredient names.")

# 3. Load a sentence-transformers model for embeddings
#    You can use any model. "all-MiniLM-L6-v2" is a common default choice.
model_name = "sentence-transformers/all-MiniLM-L6-v2"
model = SentenceTransformer(model_name)

# 4. Create embeddings for each ingredient
ingredient_list = list(all_ingredients)  # to index them easily
embeddings = model.encode(ingredient_list, convert_to_tensor=True)

# 5. Build adjacency or pairwise similarity matrix
similarity_threshold = 0.80  # Adjust based on your data
pairs = []  # We'll store pairs of (nameA, nameB, similarity)

for i in range(len(ingredient_list)):
    for j in range(i+1, len(ingredient_list)):
        sim = float(util.cos_sim(embeddings[i], embeddings[j])[0][0])
        if sim >= similarity_threshold:
            pairs.append({
                "name1": ingredient_list[i],
                "name2": ingredient_list[j],
                "similarity": sim
            })

# 6. We can do naive grouping or more advanced clustering
#    For simplicity, let's just store the pairs for manual inspection
pairs.sort(key=lambda x: x["similarity"], reverse=True)

output = {
    "ingredients": ingredient_list,
    "similarityThreshold": similarity_threshold,
    "similarPairs": pairs
}

# 7. Write JSON for manual review
with open("ingredient_embedding_proposals.json", "w", encoding="utf-8") as f:
    json.dump(output, f, indent=2, ensure_ascii=False)

print("Embedding-based matching complete. See 'ingredient_embedding_proposals.json' for pairs above threshold.")

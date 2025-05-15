export interface PrepBag {
  id: string;
  dishName: string;
  ingredients: {
    name: string;
    weight: number;
    unit: string;
  }[];
  addedIngredients: {
    name: string;
  }[];
  isComplete: boolean;
} 
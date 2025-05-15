import { PrepBag } from './prepBag';

export interface Dish {
  id: string;
  name: string;
  prepBags: PrepBag[];
  completedPrepBags?: PrepBag[];
  ingredients: {
    name: string;
    weight: number;
    unit: string;
  }[];
  quantity: number;
  matrix: (PrepBag | null)[][];
  colour?: string;
  trayId?: string;
  trayQuantity?: number;
} 
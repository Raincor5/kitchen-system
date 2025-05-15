export interface Tray {
  id: string;
  name: string;
  dimensions?: {
    width: number;
    length: number;
    depth: number;
  };
  volumeLiters?: number;
  material?: string;
  description?: string;
} 
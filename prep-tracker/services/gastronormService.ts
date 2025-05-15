import { API_BASE_URL } from '../app/config';

export interface GastronormTray {
  id: string;
  name: string;
  dimensions: {
    length: number;
    width: number;
    depth: number;
  };
  volumeLiters: number;
  material: string;
  description: string;
}

export interface TrayStats {
  total_trays: number;
  total_volume_liters: number;
  materials: string[];
}

export interface TrayContents {
  tray: GastronormTray;
  prep_data: any; // Type this based on your prep data structure
}

class GastronormService {
  private static instance: GastronormService;
  private baseUrl: string;

  private constructor() {
    this.baseUrl = `${API_BASE_URL}/gastronorm`;
  }

  public static getInstance(): GastronormService {
    if (!GastronormService.instance) {
      GastronormService.instance = new GastronormService();
    }
    return GastronormService.instance;
  }

  async getAllTrays(): Promise<GastronormTray[]> {
    try {
      const response = await fetch(`${this.baseUrl}/list`);
      if (!response.ok) {
        throw new Error(`Failed to fetch trays: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching trays:', error);
      throw error;
    }
  }

  async selectTrays(trayIds: string[]): Promise<{ message: string; selected_trays: string[] }> {
    try {
      const response = await fetch(`${this.baseUrl}/select`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ tray_ids: trayIds }),
      });
      if (!response.ok) {
        throw new Error(`Failed to select trays: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error selecting trays:', error);
      throw error;
    }
  }

  async getTrayById(trayId: string): Promise<GastronormTray> {
    try {
      const response = await fetch(`${this.baseUrl}/${trayId}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch tray: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching tray:', error);
      throw error;
    }
  }

  async getSelectedTraysStats(): Promise<TrayStats> {
    try {
      const response = await fetch(`${this.baseUrl}/stats/selected`);
      if (!response.ok) {
        throw new Error(`Failed to fetch tray stats: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching tray stats:', error);
      throw error;
    }
  }

  async getTrayContents(trayId: string): Promise<TrayContents> {
    try {
      const response = await fetch(`${this.baseUrl}/${trayId}/contents`);
      if (!response.ok) {
        throw new Error(`Failed to fetch tray contents: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching tray contents:', error);
      throw error;
    }
  }
}

export default GastronormService.getInstance(); 
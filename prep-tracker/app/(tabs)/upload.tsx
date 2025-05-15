// app/(tabs)/upload.tsx

import React, { useState } from 'react';
import { View, Text, StyleSheet, Alert, Image, ActivityIndicator } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import * as ImageManipulator from 'expo-image-manipulator';
import { useRecipeContext } from '@/context/RecipeContext';
import { Pressable } from 'react-native';
import apiEndpoints from "@/constants/apiConfig";

export default function TabTwoScreen() {
  const { fetchRecipes } = useRecipeContext(); // Access fetchRecipes from the global context
  const [photo, setPhoto] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const handleUploadFile = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      quality: 1,
    });

    if (!result.canceled) {
      console.log('Selected file:', result.assets[0].uri);
      const compressedUri = await compressImage(result.assets[0].uri);
      setPhoto(compressedUri);
      console.log('Ready to upload:', compressedUri);
    }
  };

  const handleTakePicture = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'Camera permission is required to take pictures.');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      quality: 1,
    });

    if (!result.canceled) {
      console.log('Captured image:', result.assets[0].uri);
      const compressedUri = await compressImage(result.assets[0].uri);
      setPhoto(compressedUri);
      console.log('Ready to upload:', compressedUri);
    }
  };

  const compressImage = async (uri: string): Promise<string> => {
    try {
      console.log('Compressing image...');
      const result = await ImageManipulator.manipulateAsync(
        uri,
        [{ resize: { width: 800 } }], // Reduced from 1024px to 800px for faster upload
        { compress: 0.5, format: ImageManipulator.SaveFormat.JPEG } // Reduced from 0.7 to 0.5 for faster upload
      );
      console.log('Compressed image URI:', result.uri);
      return result.uri;
    } catch (error) {
      console.error('Error compressing image:', error);
      Alert.alert('Error', 'Failed to compress the image.');
      throw error;
    }
  };

  const handleUpload = async () => {
    if (!photo) {
      Alert.alert('No Photo', 'Please select or capture a photo first.');
      return;
    }

    setIsUploading(true);
    const formData = new FormData();
    formData.append('image', {
      uri: photo,
      name: 'photo.jpg',
      type: 'image/jpeg',
    } as any);

    try {
      console.log('Uploading photo...');
      
      // Create a controller for the fetch request with a timeout
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 30000); // 30 second timeout
      
      const response = await fetch(apiEndpoints.processImage, {
        method: "POST",
        body: formData,
        signal: controller.signal,
      });
      
      // Clear the timeout
      clearTimeout(timeoutId);

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to upload photo: ${errorText}`);
      }

      const data = await response.json();
      Alert.alert('Success', `Recipe processed: ${data.recipe.name}`);
      console.log('Server response:', data);

      // Fetch the updated list of recipes after successful upload
      await fetchRecipes();
    } catch (error: any) {
      console.error('Error during upload:', error);
      
      if (error.name === 'AbortError') {
        Alert.alert('Upload Timeout', 'The upload is taking too long. Please try again later or check your internet connection.');
      } else {
        Alert.alert('Error', 'Failed to upload the image and process the recipe. Please try again later.');
      }
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Upload Recipes</Text>

      <View style={styles.buttonContainer}>
        <Pressable style={[styles.button, styles.uploadButton]} onPress={handleUploadFile}>
          <Text style={styles.buttonText}>Upload File</Text>
        </Pressable>
        <Pressable style={[styles.button, styles.cameraButton]} onPress={handleTakePicture}>
          <Text style={styles.buttonText}>Take a Picture</Text>
        </Pressable>
      </View>

      {photo && (
        <View style={styles.previewContainer}>
          <Text style={styles.subtitle}>Preview:</Text>
          <Image source={{ uri: photo }} style={styles.preview} />
          <Pressable 
            style={[styles.button, styles.uploadAndProcessButton, isUploading && styles.buttonDisabled]} 
            onPress={handleUpload}
            disabled={isUploading}
          >
            {isUploading ? (
              <ActivityIndicator color="#ffffff" />
            ) : (
              <Text style={styles.buttonText}>Upload and Process</Text>
            )}
          </Pressable>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#121212',
    padding: 20,
  },
  title: {
    fontSize: 26,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 30,
  },
  subtitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 10,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
    width: '100%',
  },
  button: {
    flex: 1,
    paddingVertical: 15,
    alignItems: 'center',
    borderRadius: 8,
    marginHorizontal: 10,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  uploadButton: {
    backgroundColor: '#4CAF50', // Green
  },
  cameraButton: {
    backgroundColor: '#007bff', // Blue
  },
  uploadAndProcessButton: {
    backgroundColor: '#ff5722', // Orange
    marginTop: 20,
    width: 200,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  previewContainer: {
    alignItems: 'center',
    marginTop: 30,
    padding: 20,
    backgroundColor: '#1e1e1e',
    borderRadius: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 5,
    elevation: 5, // Shadow for Android
  },
  preview: {
    width: 200,
    height: 200,
    marginBottom: 15,
    borderRadius: 15,
  },
});

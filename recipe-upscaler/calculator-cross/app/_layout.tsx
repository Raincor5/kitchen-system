import FontAwesome from '@expo/vector-icons/FontAwesome';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import { RootStackParamList } from '@/types/types'; // Adjust the path to match your project structure
import * as SplashScreen from 'expo-splash-screen';
import * as Sentry from 'sentry-expo';
import { useEffect } from 'react';
import 'react-native-reanimated';
import { Alert, StyleSheet } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { RecipeProvider } from '@/context/RecipeContext';
import { useColorScheme } from '@/components/useColorScheme';

export {
  // Catch any errors thrown by the Layout component.
  ErrorBoundary,
} from 'expo-router';

export const unstable_settings = {
  // Ensure that reloading on `/modal` keeps a back button present.
  initialRouteName: '(tabs)',
};

// Initialize Sentry globally
Sentry.init({
  dsn: 'https://9491341ec3c87277c992482a806e696b@o4508668758523904.ingest.de.sentry.io/4508668763308112', // TODO: Move to secrets
  enableInExpoDevelopment: true, // Enable Sentry for development mode
  debug: true, // Enable debug information in the console
});

ErrorUtils.setGlobalHandler((error, isFatal) => {
  console.error('Global Error:', error, isFatal); // Log for debugging

  // Send the error to Sentry
  Sentry.Native.captureException(error);

  if (isFatal) {
    Alert.alert(
      'Critical Error',
      'A critical error occurred. The app needs to restart.',
      [{ text: 'OK' }]
    );
  }
});

// Prevent the splash screen from auto-hiding before asset loading is complete.
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [loaded, error] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
    ...FontAwesome.font,
  });

  // Expo Router uses Error Boundaries to catch errors in the navigation tree.
  useEffect(() => {
    if (error) {
      // Capture font loading errors in Sentry
      Sentry.Native.captureException(error);
      throw error;
    }
  }, [error]);

  useEffect(() => {
    if (loaded) {
      SplashScreen.hideAsync();
    }
  }, [loaded]);

  if (!loaded) {
    return null;
  }

  return (
    <GestureHandlerRootView style={styles.gestureHandlerRoot}>
      <RecipeProvider>
        <RootLayoutNav />
      </RecipeProvider>
    </GestureHandlerRootView>
  );
}

function RootLayoutNav() {
  const colorScheme = useColorScheme();

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Stack>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="modal" options={{ presentation: 'modal', title: 'Help - Recipe Scaler' }} />
        <Stack.Screen
          name="recipe-detail"
          options={{ title: 'Recipe Details', presentation: "modal", headerShown: false }}
        />
      </Stack>
    </ThemeProvider>
  );
}

const styles = StyleSheet.create({
  gestureHandlerRoot: {
    flex: 1,
  },
});

import { StatusBar } from 'expo-status-bar';
import { Platform, StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';

export default function ModalScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Recipe Scaler</Text>
      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

      <Text style={styles.description}>
        The Recipe Scaler helps you adjust your recipes to fit your needs. Here's what you can do:
      </Text>

      <View style={styles.list}>
        <Text style={styles.listItem}>
          • <Text style={styles.bold}>Portion Scaler:</Text> Scale recipes up or down by entering the desired portion size, even with decimals (e.g., 0.5 for half a portion, 3 for triple the portion).
        </Text>
        <Text style={styles.listItem}>
          • <Text style={styles.bold}>Scale by Availability:</Text> Adjust all ingredients based on the available quantity of a key ingredient. Just input the amount you have, and the scaler will calculate the rest.
        </Text>
        <Text style={styles.listItem}>
          • <Text style={styles.bold}>Unit Conversion:</Text> Convert between units (e.g., grams to kilograms, milliliters to liters) dynamically while scaling your recipes.
        </Text>
      </View>

      <Text style={styles.footer}>
        Use these features to make your cooking process more efficient and adaptable!
      </Text>

      {/* Use a light status bar on iOS to account for the black space above the modal */}
      <StatusBar style={Platform.OS === 'ios' ? 'light' : 'auto'} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  separator: {
    marginVertical: 20,
    height: 1,
    width: '80%',
  },
  description: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 20,
  },
  list: {
    marginBottom: 20,
    width: '100%',
  },
  listItem: {
    fontSize: 16,
    marginVertical: 5,
    lineHeight: 24,
    textAlign: 'left',
  },
  bold: {
    fontWeight: 'bold',
  },
  footer: {
    fontSize: 14,
    textAlign: 'center',
    marginTop: 20,
    color: 'gray',
  },
});

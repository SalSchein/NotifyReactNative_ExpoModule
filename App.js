import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Button, View, SafeAreaView, Text, Alert, NativeModules } from 'react-native';
const { NotificationRequestModule } = NativeModules;

export default function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>
        This is a demo of react native module for requesting the notification to local server.
      </Text>
      <View style={styles.fixToText}>
        <Button
          title="Start Service"
          onPress={() => 
            NotificationRequestModule.createNotificationRequestService("http://192.168.0.104:3000/", 15)
            //NotificationRequestModule.createNotificationRequestService("http://192.168.0.153:3000/unresolved-notifications", 15)
          }
        />
        <Button
          title="Stop Service"
          onPress={() => 
            NotificationRequestModule.stopNotificationRequestService()
          }
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    marginHorizontal: 16,
  },
  title: {
    textAlign: 'center',
    marginVertical: 8,
  },
  fixToText: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  }
});

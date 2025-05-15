import { Camera } from "expo-camera";
import React, { useRef, useState } from "react";
import { Button, View, StyleSheet, Image } from "react-native";

const AIIntegrationMobile = () => {
    const [hasPermission, setHasPermission] = useState(null);
    const [cameraRef, setCameraRef] = useState(null);
    const [photo, setPhoto] = useState(null);

    useEffect(() => {
        (async () => {
            const { status } = await Camera.requestPermissionsAsync();
            setHasPermission(status === "granted");
        })();
    }, []);

    const takePicture = async () => {
        if (cameraRef) {
            const data = await cameraRef.takePictureAsync();
            setPhoto(data.uri);
        }
    };

    const handleUpload = async () => {
        const formData = new FormData();
        formData.append("image", {
            uri: photo,
            name: "photo.jpg",
            type: "image/jpeg",
        });

        try {
            const response = await fetch("http://localhost:5000/api/process-image", {
                method: "POST",
                body: formData,
            });

            if (!response.ok) {
                throw new Error("Failed to upload photo.");
            }

            const data = await response.json();
            alert("Recipe processed successfully!");
            console.log(data);
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <View style={styles.container}>
            {hasPermission ? (
                <Camera style={styles.camera} ref={(ref) => setCameraRef(ref)}>
                    <View style={styles.buttonContainer}>
                        <Button title="Take Picture" onPress={takePicture} />
                    </View>
                </Camera>
            ) : (
                <Text>No access to camera</Text>
            )}
            {photo && (
                <View>
                    <Image source={{ uri: photo }} style={styles.preview} />
                    <Button title="Upload and Process" onPress={handleUpload} />
                </View>
            )}
        </View>
    );
};

const styles = StyleSheet.create({
    container: { flex: 1 },
    camera: { flex: 1 },
    buttonContainer: { flex: 0.1, justifyContent: "center" },
    preview: { width: 200, height: 200 },
});

export default AIIntegrationMobile;

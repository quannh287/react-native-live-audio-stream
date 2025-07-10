package com.imxiqi.rnliveaudiostream;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * Centralized event emitter for audio stream events
 * This class handles all event emissions to React Native
 */
public class AudioEventEmitter {
    private static ReactContext reactContext;

    /**
     * Set the React Context for event emission
     * This should be called from the Module when it's initialized
     */
    public static void setReactContext(ReactContext context) {
        reactContext = context;
    }

    /**
     * Send audio data to React Native
     * @param data Base64 encoded audio data
     */
    public static void sendAudioData(String data) {
        android.util.Log.d("AudioEventEmitter", "sendAudioData called. data.length=" + (data != null ? data.length() : "null") + ", reactContext=" + (reactContext != null));
        if (reactContext != null) {
            WritableMap params = Arguments.createMap();
            params.putString("data", data);
            emit("data", params);
        } else {
            android.util.Log.e("AudioEventEmitter", "sendAudioData: reactContext is null!");
        }
    }

    /**
     * Send recording state change event
     * @param isRecording current recording state
     */
    public static void sendRecordingState(boolean isRecording) {
        if (reactContext != null) {
            WritableMap params = Arguments.createMap();
            params.putBoolean("isRecording", isRecording);
            emit("recordingState", params);
        }
    }

    /**
     * Send error event
     * @param error error message
     */
    public static void sendError(String error) {
        if (reactContext != null) {
            WritableMap params = Arguments.createMap();
            params.putString("error", error);
            emit("error", params);
        }
    }

    /**
     * Generic method to emit events (overload: WritableMap or String)
     */
    private static void emit(String eventName, WritableMap params) {
        if (reactContext != null) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    private static void emit(String eventName, String data) {
        if (reactContext != null) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, data);
        }
    }
}

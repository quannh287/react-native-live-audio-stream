package com.imxiqi.rnliveaudiostream;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
public class AudioEventEmitter {
    private static ReactContext reactContext;

    public static void setReactContext(ReactContext context) {
        reactContext = context;
    }
    public static void sendAudioData(String data) {
        android.util.Log.d("AudioEventEmitter", "sendAudioData called. data.length=" + (data != null ? data.length() : "null") + ", reactContext=" + (reactContext != null));
        if (reactContext != null) {
            emit("data", data);
        } else {
            android.util.Log.e("AudioEventEmitter", "sendAudioData: reactContext is null!");
        }
    }
    public static void sendRecordingState(boolean isRecording) {
        if (reactContext != null) {
            WritableMap params = Arguments.createMap();
            params.putBoolean("isRecording", isRecording);
            emit("recordingState", params);
        }
    }
    public static void sendError(String error) {
        if (reactContext != null) {
            WritableMap params = Arguments.createMap();
            params.putString("error", error);
            emit("error", params);
        }
    }

    // Notify JS about service lifecycle/state changes
    // state examples: "stopped", "killed"
    // reason examples: "action_stop", "task_removed", "destroy"
    public static void sendServiceState(String state, String reason) {
        if (reactContext != null) {
            WritableMap params = Arguments.createMap();
            params.putString("state", state);
            if (reason != null) params.putString("reason", reason);
            emit("serviceState", params);
        }
    }
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

package com.imxiqi.rnliveaudiostream;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

public class RNLiveAudioStreamModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    // Audio parameters
    private int sampleRate = 44100;
    private int channels = 1;
    private int bitsPerSample = 16;
    private int audioSource = 6; // MediaRecorder.AudioSource.VOICE_RECOGNITION
    private int bufferSize = 2048;

    // Notification parameters
    private String notificationTitle = "Audio Recording";
    private String notificationContent = "Recording audio in background";

    public RNLiveAudioStreamModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        // Set ReactContext for AudioEventEmitter
        AudioEventEmitter.setReactContext(reactContext);
    }

    @Override
    public String getName() {
        return "RNLiveAudioStream";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("SAMPLE_RATE", sampleRate);
        constants.put("CHANNELS", channels);
        constants.put("BITS_PER_SAMPLE", bitsPerSample);
        constants.put("AUDIO_SOURCE", audioSource);
        constants.put("BUFFER_SIZE", bufferSize);
        return constants;
    }

    @ReactMethod
    public void init(ReadableMap options) {
        if (options.hasKey("sampleRate")) {
            sampleRate = options.getInt("sampleRate");
        }
        if (options.hasKey("channels")) {
            channels = options.getInt("channels");
        }
        if (options.hasKey("bitsPerSample")) {
            bitsPerSample = options.getInt("bitsPerSample");
        }
        if (options.hasKey("audioSource")) {
            audioSource = options.getInt("audioSource");
        }
        if (options.hasKey("bufferSize")) {
            bufferSize = options.getInt("bufferSize");
        }
        if (options.hasKey("notificationTitle")) {
            notificationTitle = options.getString("notificationTitle");
        }
        if (options.hasKey("notificationContent")) {
            notificationContent = options.getString("notificationContent");
        }
    }

    @ReactMethod
    public void start(Promise promise) {
        if (!hasAudioPermission()) {
            String error = "Audio recording permission not granted";
            AudioEventEmitter.sendError(error);
            promise.reject("PERMISSION_ERROR", error);
            return;
        }

        try {
            // Start foreground service
            RNLiveAudioStreamService.startService(
                    reactContext,
                    sampleRate,
                    channels,
                    bitsPerSample,
                    audioSource,
                    bufferSize,
                    notificationTitle,
                    notificationContent
            );

            AudioEventEmitter.sendRecordingState(true);
            promise.resolve("Started");
        } catch (Exception e) {
            AudioEventEmitter.sendError(e.getMessage());
            promise.reject("START_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void stop(Promise promise) {
        try {
            RNLiveAudioStreamService.stopService(reactContext);
            AudioEventEmitter.sendRecordingState(false);
            promise.resolve("Stopped");
        } catch (Exception e) {
            AudioEventEmitter.sendError(e.getMessage());
            promise.reject("STOP_ERROR", e.getMessage());
        }
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
}

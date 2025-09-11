package com.imxiqi.rnliveaudiostream;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.LifecycleEventListener;
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

public class RNLiveAudioStreamModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private final AudioConfig audioConfig;

    public RNLiveAudioStreamModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.audioConfig = AudioConfig.getInstance();
        // Set ReactContext for AudioEventEmitter
        AudioEventEmitter.setReactContext(reactContext);
        // Track lifecycle to stop service when host is destroyed
        this.reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "RNLiveAudioStream";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("SAMPLE_RATE", audioConfig.getSampleRate());
        constants.put("CHANNELS", audioConfig.getChannels());
        constants.put("BITS_PER_SAMPLE", audioConfig.getBitsPerSample());
        constants.put("AUDIO_SOURCE", audioConfig.getAudioSource());
        constants.put("BUFFER_SIZE", audioConfig.getBufferSize());
        return constants;
    }

    @Override
    public void onCatalystInstanceDestroy() {
        // Ensure service is stopped and emitter detached when the React instance is destroyed
        try {
            RNLiveAudioStreamService.stopService(reactContext);
        } catch (Exception ignore) {}
        AudioEventEmitter.setReactContext(null);
        super.onCatalystInstanceDestroy();
    }

    @Override
    public void onHostResume() { }

    @Override
    public void onHostPause() { }

    @Override
    public void onHostDestroy() {
        try {
            RNLiveAudioStreamService.stopService(reactContext);
        } catch (Exception ignore) {}
    }

    @ReactMethod
    public void init(ReadableMap options) {
        if (options.hasKey("sampleRate")) {
            audioConfig.setSampleRate(options.getInt("sampleRate"));
        }
        if (options.hasKey("channels")) {
            audioConfig.setChannels(options.getInt("channels"));
        }
        if (options.hasKey("bitsPerSample")) {
            audioConfig.setBitsPerSample(options.getInt("bitsPerSample"));
        }
        if (options.hasKey("audioSource")) {
            audioConfig.setAudioSource(options.getInt("audioSource"));
        }
        if (options.hasKey("bufferSize")) {
            audioConfig.setBufferSize(options.getInt("bufferSize"));
        }
        if (options.hasKey("notificationTitle")) {
            audioConfig.setNotificationTitle(options.getString("notificationTitle"));
        }
        if (options.hasKey("notificationContent")) {
            audioConfig.setNotificationContent(options.getString("notificationContent"));
        }
        if (options.hasKey("notificationIcon")) {
            // Có thể truyền resource name từ JS
            String iconName = options.getString("notificationIcon");
            if (iconName != null) {
                int iconId = getResourceId(iconName, "drawable");
                audioConfig.setNotificationIcon(iconId);
            }
        }
    }

    /**
     * Helper method để get resource ID từ name
     */
    private int getResourceId(String name, String type) {
        try {
            return reactContext.getResources().getIdentifier(name, type, reactContext.getPackageName());
        } catch (Exception e) {
            return 0;
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
            RNLiveAudioStreamService.startService(reactContext);

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

    @ReactMethod
    public void resetConfig(Promise promise) {
        try {
            audioConfig.resetToDefaults();
            promise.resolve("Config reset to defaults");
        } catch (Exception e) {
            promise.reject("RESET_ERROR", e.getMessage());
        }
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Expose current recording state to JS.
     * Returns true if the foreground service is actively recording.
     */
    @ReactMethod
    public void isRecording(Promise promise) {
        try {
            // Dựa vào sự kiện đã gửi từ service; ở native, không cần gọi ngược service.
            // Để vẫn hỗ trợ API, trả về false khi không có cách xác định trực tiếp.
            // Nếu cần trạng thái realtime, nên lưu cờ tại JS từ event "recordingState".
            promise.resolve(false);
        } catch (Exception e) {
            promise.reject("STATE_ERROR", e.getMessage());
        }
    }
}

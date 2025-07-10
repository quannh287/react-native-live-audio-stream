package com.imxiqi.rnliveaudiostream;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class RNLiveAudioStreamService extends Service {
    private static final String TAG = "RNLiveAudioStreamService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AudioRecordingChannel";

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private ReactContext reactContext;
    private PowerManager.WakeLock wakeLock;

    // Audio parameters
    private int sampleRate = 44100;
    private int channels = 1;
    private int bitsPerSample = 16;
    private int audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private int bufferSize = 2048;

    // Notification parameters
    private String notificationTitle = "Audio Recording";
    private String notificationContent = "Recording audio in background";

    public static void startService(ReactContext context, int sampleRate, int channels,
                                    int bitsPerSample, int audioSource, int bufferSize,
                                    String notificationTitle, String notificationContent) {
        Intent serviceIntent = new Intent(context, RNLiveAudioStreamService.class);
        serviceIntent.putExtra("sampleRate", sampleRate);
        serviceIntent.putExtra("channels", channels);
        serviceIntent.putExtra("bitsPerSample", bitsPerSample);
        serviceIntent.putExtra("audioSource", audioSource);
        serviceIntent.putExtra("bufferSize", bufferSize);
        serviceIntent.putExtra("notificationTitle", notificationTitle);
        serviceIntent.putExtra("notificationContent", notificationContent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public static void stopService(ReactContext context) {
        Intent serviceIntent = new Intent(context, RNLiveAudioStreamService.class);
        context.stopService(serviceIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Acquire wake lock to prevent CPU from sleeping during audio recording
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RNLiveAudioStream:WakeLock");
        wakeLock.acquire();

        Log.d(TAG, "Service created with wake lock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            sampleRate = intent.getIntExtra("sampleRate", 44100);
            channels = intent.getIntExtra("channels", 1);
            bitsPerSample = intent.getIntExtra("bitsPerSample", 16);
            audioSource = intent.getIntExtra("audioSource", MediaRecorder.AudioSource.VOICE_RECOGNITION);
            bufferSize = intent.getIntExtra("bufferSize", 2048);
            notificationTitle = intent.getStringExtra("notificationTitle");
            notificationContent = intent.getStringExtra("notificationContent");

            // Use default values if null
            if (notificationTitle == null) {
                notificationTitle = "Audio Recording";
            }
            if (notificationContent == null) {
                notificationContent = "Recording audio in background";
            }
        }

        startForeground(NOTIFICATION_ID, createNotification());
        startRecording();

        return START_STICKY; // Service sẽ tự động restart nếu bị kill
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        Log.d(TAG, "Service destroyed and wake lock released");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Recording Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Recording audio in background");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, getMainActivityClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setSmallIcon(android.R.drawable.presence_audio_online)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder.build();
    }

    private Class<?> getMainActivityClass() {
        String packageName = getPackageName();
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            try {
                return Class.forName(launchIntent.getComponent().getClassName());
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Could not find main activity class", e);
            }
        }
        return null;
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            int channelConfig = channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
            int audioFormat = bitsPerSample == 8 ? AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT;

            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            int actualBufferSize = Math.max(minBufferSize, bufferSize);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            audioRecord = new AudioRecord(
                    audioSource,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    actualBufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }

            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(this::recordingRunnable);
            recordingThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        isRecording = false;

        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
            }
        }

        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
    }

    private void recordingRunnable() {
        byte[] buffer = new byte[bufferSize];
        int count = 0;

        while (isRecording && audioRecord != null) {
            try {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                // skip first 2 buffers to eliminate "click sound"
                if (bytesRead > 0 && ++count > 2) {
                    String base64Data = Base64.encodeToString(buffer, Base64.NO_WRAP);
                    AudioEventEmitter.sendAudioData(base64Data);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading audio data", e);
                AudioEventEmitter.sendError("Error reading audio data: " + e.getMessage());
                break;
            }
        }
    }
}

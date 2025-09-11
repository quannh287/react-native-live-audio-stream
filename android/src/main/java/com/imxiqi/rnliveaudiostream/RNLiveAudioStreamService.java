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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.ReactContext;

public class RNLiveAudioStreamService extends Service {
    private static final String TAG = "RNLiveAudioStreamService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AudioRecordingChannel";
    private static final String ACTION_STOP = "com.imxiqi.rnliveaudiostream.ACTION_STOP";

    private AudioRecord audioRecord;
    private volatile boolean isRecording = false;
    private volatile boolean isInitializing = false;
    private Thread recordingThread;
    private AudioConfig audioConfig;

    private HandlerThread audioHandlerThread;
    private Handler audioHandler;

    private Notification cachedNotification;
    private int lastStartId = 0;

    public static boolean isRecording() {
        return isRecording;
    }

    public static void startService(ReactContext context) {
        Intent serviceIntent = new Intent(context, RNLiveAudioStreamService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public static void stopService(ReactContext context) {
        // Prefer to deliver ACTION_STOP to a running service so it can cleanup internally.
        // If background launch is restricted, request system to stop the service directly.
        Intent stopIntent = new Intent(context, RNLiveAudioStreamService.class).setAction(ACTION_STOP);
        try {
            context.startService(stopIntent);
        } catch (IllegalStateException ex) {
            // If we cannot start in background, just stop the service directly.
            context.stopService(new Intent(context, RNLiveAudioStreamService.class));
        }
        // Best-effort ensure stop even if ACTION_STOP wasn't delivered.
        context.stopService(new Intent(context, RNLiveAudioStreamService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioConfig = AudioConfig.getInstance();
        audioHandlerThread = new HandlerThread("AudioServiceThread");
        audioHandlerThread.start();
        audioHandler = new Handler(audioHandlerThread.getLooper());
        createNotificationChannelAsync();
        preCreateNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.lastStartId = startId;

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Log.d(TAG, "Received ACTION_STOP; stopping service");
            stopServiceGracefully();
            return START_NOT_STICKY;
        }
        if (cachedNotification != null) {
            startForeground(NOTIFICATION_ID, cachedNotification);
        } else {
            startForeground(NOTIFICATION_ID, createNotificationSync());
        }
        startRecordingAsync();

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: user removed task; stopping service");
        stopServiceGracefully();

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopServiceGracefully();
        if (audioHandlerThread != null) {
            audioHandlerThread.quitSafely();
            try { audioHandlerThread.join(500); } catch (InterruptedException ignored) {}
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannelAsync() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioHandler.post(() -> {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Audio Recording Service",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Recording audio in background");

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            });
        }
    }

    private void preCreateNotification() {
        audioHandler.post(() -> {
            cachedNotification = createNotificationSync();
            Log.d(TAG, "Notification pre-created successfully");
        });
    }

    private Notification createNotificationSync() {
        try {
            Intent notificationIntent = new Intent(this, getMainActivityClass());
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(audioConfig.getNotificationTitle())
                    .setContentText(audioConfig.getNotificationContent())
                    .setSmallIcon(ResourceHelper.getNotificationIcon(this))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
            // Return minimal notification as fallback
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Audio Recording")
                    .setContentText("Recording...")
                    .setSmallIcon(ResourceHelper.getNotificationIcon(this))
                    .build();
        }
    }

    private Class<?> getMainActivityClass() {
        try {
            String packageName = getPackageName();
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null && launchIntent.getComponent() != null) {
                return Class.forName(launchIntent.getComponent().getClassName());
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not find main activity class", e);
        }
        return null;
    }



    private void startRecordingAsync() {
        if (isRecording || isInitializing) {
            Log.w(TAG, "Recording already in progress or initializing");
            return;
        }

        isInitializing = true;

        audioHandler.post(() -> {
            try {
                Log.d(TAG, "Starting audio initialization...");

                // Permission check
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Audio permission not granted");
                    AudioEventEmitter.sendError("Audio permission not granted");
                    isInitializing = false;
                    return;
                }

                // Audio configuration
                int channelConfig = audioConfig.getChannels() == 1 ?
                        AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
                int audioFormat = audioConfig.getBitsPerSample() == 8 ?
                        AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT;

                int minBufferSize = AudioRecord.getMinBufferSize(
                        audioConfig.getSampleRate(), channelConfig, audioFormat);

                if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Invalid audio configuration");
                    AudioEventEmitter.sendError("Invalid audio configuration");
                    isInitializing = false;
                    return;
                }

                int actualBufferSize = Math.max(minBufferSize, audioConfig.getBufferSize());

                Log.d(TAG, "Creating AudioRecord with buffer size: " + actualBufferSize);

                audioRecord = new AudioRecord(
                        audioConfig.getAudioSource(),
                        audioConfig.getSampleRate(),
                        channelConfig,
                        audioFormat,
                        actualBufferSize
                );

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed");
                    AudioEventEmitter.sendError("AudioRecord initialization failed");
                    if (audioRecord != null) {
                        audioRecord.release();
                        audioRecord = null;
                    }
                    isInitializing = false;
                    return;
                }

                audioRecord.startRecording();
                isRecording = true;
                isInitializing = false;

                recordingThread = new Thread(this::recordingRunnable, "AudioRecordingThread");
                recordingThread.setPriority(Thread.MAX_PRIORITY);
                recordingThread.start();

                Log.d(TAG, "Audio recording started successfully");

                new Handler(Looper.getMainLooper()).post(() -> {
                    AudioEventEmitter.sendRecordingState(true);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in startRecordingAsync", e);
                AudioEventEmitter.sendError("Error starting recording: " + e.getMessage());
                isInitializing = false;

                // Cleanup on error
                if (audioRecord != null) {
                    try {
                        audioRecord.release();
                    } catch (Exception ignored) {}
                    audioRecord = null;
                }
            }
        });
    }

    private void stopRecording() {
        if (!isRecording && !isInitializing) return;

        Log.d(TAG, "Stopping recording...");
        isRecording = false;
        isInitializing = false;

        if (audioHandler != null) {
            audioHandler.post(() -> {
                if (audioRecord != null) {
                    try {
                        audioRecord.stop();
                        audioRecord.release();
                        Log.d(TAG, "AudioRecord stopped and released");
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping AudioRecord", e);
                    } finally {
                        audioRecord = null;
                    }
                }
            });
        }

        if (recordingThread != null) {
            recordingThread.interrupt();
            try {
                recordingThread.join(1000); // Wait max 1 second
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for recording thread to finish");
            }
            recordingThread = null;
        }
    }

    private void recordingRunnable() {
        Log.d(TAG, "Recording thread started");
        byte[] buffer = new byte[audioConfig.getBufferSize()];
        int bufferCount = 0;
        // Skip first buffers to eliminate click sound
        final int SKIP_BUFFER_COUNT = 2;

        try {
            while (isRecording && audioRecord != null && !Thread.currentThread().isInterrupted()) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        if (++bufferCount > SKIP_BUFFER_COUNT) {
                            String base64Data = Base64.encodeToString(buffer, 0, bytesRead, Base64.NO_WRAP);
                            AudioEventEmitter.sendAudioData(base64Data);
                        }
                    } else if (bytesRead < 0) {
                        Log.e(TAG, "Error reading audio data: " + bytesRead);
                        AudioEventEmitter.sendError("Error reading audio data: " + bytesRead);
                        break;
                    }
                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        Log.e(TAG, "Exception in recording thread", e);
                        AudioEventEmitter.sendError("Recording error: " + e.getMessage());
                    }
                    break;
                }
            }
        } finally {
            Log.d(TAG, "Recording thread finished");
            // Ensure the service stops itself when recording ends or errors occur
            new Handler(getMainLooper()).post(() -> {
                stopServiceGracefully();
            });
        }
    }

    private void clearNotification() {
        // Stop foreground and explicitly cancel notification to ensure it's cleared
        try {
            stopForeground(true);
        } catch (Exception ignore) {}
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Exception ignore) {}
        cachedNotification = null;
    }

    private void stopServiceGracefully() {
        try {
            stopRecording();
        } catch (Exception ignore) {}
        clearNotification();
        try {
            if (!stopSelfResult(this.lastStartId)) {
                stopSelf();
            }
        } catch (Exception ignore) {}
    }
}

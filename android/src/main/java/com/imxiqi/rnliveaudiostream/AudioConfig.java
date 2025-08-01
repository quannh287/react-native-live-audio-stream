package com.imxiqi.rnliveaudiostream;

import android.media.MediaRecorder;

public class AudioConfig {
    private static AudioConfig instance;

    // Audio parameters với default values
    private int sampleRate = 44100;
    private int channels = 1;
    private int bitsPerSample = 16;
    private int audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private int bufferSize = 2048;

    // Notification parameters với default values
    private String notificationTitle = "Audio Recording";
    private String notificationContent = "Recording audio in background";

    private int notificationIcon = 0; // 0 = use default logic

    private AudioConfig() {
        // Private constructor để đảm bảo singleton
    }

    public static synchronized AudioConfig getInstance() {
        if (instance == null) {
            instance = new AudioConfig();
        }
        return instance;
    }

    // Getters
    public int getSampleRate() { return sampleRate; }
    public int getChannels() { return channels; }
    public int getBitsPerSample() { return bitsPerSample; }
    public int getAudioSource() { return audioSource; }
    public int getBufferSize() { return bufferSize; }
    public String getNotificationTitle() { return notificationTitle; }
    public String getNotificationContent() { return notificationContent; }
    public int getNotificationIcon() { return notificationIcon; }

    // Setters với validation
    public AudioConfig setSampleRate(int sampleRate) {
        if (sampleRate > 0) {
            this.sampleRate = sampleRate;
        }
        return this;
    }

    public AudioConfig setChannels(int channels) {
        if (channels == 1 || channels == 2) {
            this.channels = channels;
        }
        return this;
    }

    public AudioConfig setBitsPerSample(int bitsPerSample) {
        if (bitsPerSample == 8 || bitsPerSample == 16) {
            this.bitsPerSample = bitsPerSample;
        }
        return this;
    }

    public AudioConfig setAudioSource(int audioSource) {
        this.audioSource = audioSource;
        return this;
    }

    public AudioConfig setBufferSize(int bufferSize) {
        if (bufferSize > 0) {
            this.bufferSize = bufferSize;
        }
        return this;
    }

    public AudioConfig setNotificationTitle(String notificationTitle) {
        if (notificationTitle != null && !notificationTitle.trim().isEmpty()) {
            this.notificationTitle = notificationTitle;
        }
        return this;
    }

    public AudioConfig setNotificationContent(String notificationContent) {
        if (notificationContent != null && !notificationContent.trim().isEmpty()) {
            this.notificationContent = notificationContent;
        }
        return this;
    }

    public AudioConfig setNotificationIcon(int notificationIcon) {
        this.notificationIcon = notificationIcon;
        return this;
    }

    // Reset về default values
    public void resetToDefaults() {
        this.sampleRate = 44100;
        this.channels = 1;
        this.bitsPerSample = 16;
        this.audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
        this.bufferSize = 2048;
        this.notificationTitle = "Audio Recording";
        this.notificationContent = "Recording audio in background";
        this.notificationIcon = 0;
    }
}

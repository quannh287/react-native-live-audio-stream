declare module "react-native-live-audio-record" {
  export type AudioEvent = "data" | "recordingState" | "error" | "serviceState"; // serviceState: Android-only
  export type EventCallback<T> = (data: T) => void;

  export interface AudioEventDataMap {
    data: string;
    recordingState: { isRecording: boolean };
    error: { error: string };
    /**
     * Android-only: native service lifecycle updates
     * - state: currently "stopped" when service is torn down
     * - reason: "action_stop" | "task_removed" | "destroy" (or vendor-specific)
     */
    serviceState: { state: string; reason?: string };
  }

  export interface IAudioRecord {
    init: (options: Options) => void;
    start: () => Promise<string>;
    stop: () => Promise<string>;
    on: <T extends AudioEvent>(event: T, callback: EventCallback<T extends keyof AudioEventDataMap ? AudioEventDataMap[T] : any>) => void;
    addListener: <T extends AudioEvent>(event: T, callback: EventCallback<T extends keyof AudioEventDataMap ? AudioEventDataMap[T] : any>) => void;
    removeListener: (event: AudioEvent) => void;
    removeAllListeners: () => void;
    /**
     * Android-only: synchronous variant of consumeWasKilledFlag.
     */
    consumeWasKilledFlagSync: () => boolean;
    /**
     * Android-only: read flag without clearing (sync).
     */
    getWasKilledFlagSync: () => boolean;
    /**
     * Android-only: clear flag (sync).
     */
    clearWasKilledFlagSync: () => void;
  }

  export type AudioChannel = 1 | 2;
  export type AudioBitsPerSample = 8 | 16;

  export interface Options {
    /**
     * - `44100 | 22050 | 16000 | 11025`
     * Default: `44100`
     */
    sampleRate: number;
    /**
     * - 1: AudioFormat.CHANNEL_IN_MONO
     * - 2: AudioFormat.CHANNEL_IN_STEREO
     * Default: `1`
     */
    channels: AudioChannel;
    /**
     * - 8: AudioFormat.ENCODING_PCM_8BIT
     * - 16: AudioFormat.ENCODING_PCM_16BIT
     * Default: `16`
     */
    bitsPerSample: AudioBitsPerSample;
    /**
     * - 6: VOICE_RECOGNITION
     */
    audioSource?: number;
    wavFile: string;
    bufferSize?: number;
    /**
     * Title for the notification shown when recording in background
     */
    notificationTitle?: string;
    /**
     * Content text for the notification shown when recording in background
     */
    notificationContent?: string;

    /**
     * Icon for the notification shown when recording in background
     * Default: `ic_notification`
     */
    notificationIcon?: string;
  }

  const AudioRecord: IAudioRecord;

  /**
   * Event name constants. Note: SERVICE_STATE is Android-only.
   */
  export const AudioEvents: {
    DATA: 'data';
    RECORDING_STATE: 'recordingState';
    ERROR: 'error';
    SERVICE_STATE: 'serviceState'; // Android-only
  };

  export default AudioRecord;
}

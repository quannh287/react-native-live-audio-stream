declare module "react-native-live-audio-stream" {
  export type AudioEvent = "data" | "recordingState" | "error";
  export type EventCallback<T> = (data: T) => void;

  export interface AudioEventDataMap {
    data: string;
    recordingState: { isRecording: boolean };
    error: { error: string };
  }

  export interface IAudioRecord {
    init: (options: Options) => void;
    start: () => Promise<string>;
    stop: () => Promise<string>;
    on: <T extends AudioEvent>(event: T, callback: EventCallback<T extends keyof AudioEventDataMap ? AudioEventDataMap[T] : any>) => void;
    addListener: <T extends AudioEvent>(event: T, callback: EventCallback<T extends keyof AudioEventDataMap ? AudioEventDataMap[T] : any>) => void;
    removeListener: (event: AudioEvent) => void;
    removeAllListeners: () => void;
  }

  export interface Options {
    sampleRate: number;
    /**
     * - `1 | 2`
     */
    channels: number;
    /**
     * - `8 | 16`
     */
    bitsPerSample: number;
    /**
     * - `6`
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
  }

  const AudioRecord: IAudioRecord;

  export default AudioRecord;
}

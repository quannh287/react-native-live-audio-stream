import { NativeModules, NativeEventEmitter, Platform } from 'react-native';
const { RNLiveAudioStream } = NativeModules;
const EventEmitter = new NativeEventEmitter(RNLiveAudioStream);

const AudioRecord = {};

// Event name constants (SERVICE_STATE is Android-only)
export const AudioEvents = {
  DATA: 'data',
  RECORDING_STATE: 'recordingState',
  ERROR: 'error',
};

AudioRecord.init = options => RNLiveAudioStream.init(options);
AudioRecord.start = () => RNLiveAudioStream.start();
AudioRecord.stop = () => RNLiveAudioStream.stop();

// Read-only then clear manually (Android only)
AudioRecord.getWasKilledFlagSync = () => RNLiveAudioStream.getWasKilledFlagSync?.();
AudioRecord.clearWasKilledFlagSync = () => RNLiveAudioStream.clearWasKilledFlagSync?.();

const eventsMap = {
  data: AudioEvents.DATA,
  recordingState: AudioEvents.RECORDING_STATE,
  error: AudioEvents.ERROR,
};

AudioRecord.on = (event, callback) => {
  const nativeEvent = eventsMap[event];
  if (!nativeEvent) {
    throw new Error(`Invalid event. Available events are: ${Object.keys(eventsMap).join(', ')}`);
  }
  EventEmitter.removeAllListeners(nativeEvent);
  return EventEmitter.addListener(nativeEvent, callback);
};

AudioRecord.addListener = (event, callback) => {
  const nativeEvent = eventsMap[event];
  if (!nativeEvent) {
    throw new Error(`Invalid event. Available events are: ${Object.keys(eventsMap).join(', ')}`);
  }
  return EventEmitter.addListener(nativeEvent, callback);
};

// Helper method to remove all listeners for a specific event
AudioRecord.removeListener = (event) => {
  const nativeEvent = eventsMap[event];
  if (!nativeEvent) {
    throw new Error(`Invalid event. Available events are: ${Object.keys(eventsMap).join(', ')}`);
  }
  EventEmitter.removeAllListeners(nativeEvent);
};

// Helper method to remove all listeners for all events
AudioRecord.removeAllListeners = () => {
  Object.values(eventsMap).forEach(event => {
    EventEmitter.removeAllListeners(event);
  });
};

export default AudioRecord;

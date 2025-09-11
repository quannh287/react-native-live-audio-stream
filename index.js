import { NativeModules, NativeEventEmitter, Platform } from 'react-native';
const { RNLiveAudioStream } = NativeModules;
const EventEmitter = new NativeEventEmitter(RNLiveAudioStream);

const AudioRecord = {};

// Event name constants (SERVICE_STATE is Android-only)
export const AudioEvents = {
  DATA: 'data',
  RECORDING_STATE: 'recordingState',
  ERROR: 'error',
  // Android-only: emitted when native service stops/killed with a reason
  SERVICE_STATE: 'serviceState',
};

AudioRecord.init = options => RNLiveAudioStream.init(options);
AudioRecord.start = () => RNLiveAudioStream.start();
AudioRecord.stop = () => RNLiveAudioStream.stop();

// Returns a promise<boolean>; true if last app session was killed by user (Android only)
AudioRecord.consumeWasKilledFlag = () => RNLiveAudioStream.consumeWasKilledFlag?.();

const eventsMap = {
  data: AudioEvents.DATA,
  recordingState: AudioEvents.RECORDING_STATE,
  error: AudioEvents.ERROR,
  // Android-only
  serviceState: AudioEvents.SERVICE_STATE,
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

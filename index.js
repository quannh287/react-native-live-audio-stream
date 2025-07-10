import { NativeModules, NativeEventEmitter } from 'react-native';
const { RNLiveAudioStream } = NativeModules;
const EventEmitter = new NativeEventEmitter(RNLiveAudioStream);

const AudioRecord = {};

AudioRecord.init = options => RNLiveAudioStream.init(options);
AudioRecord.start = () => RNLiveAudioStream.start();
AudioRecord.stop = () => RNLiveAudioStream.stop();

const eventsMap = {
  data: 'data',
  recordingState: 'recordingState',
  error: 'error'
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

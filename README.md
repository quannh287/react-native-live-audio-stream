
# react-native-live-audio-stream

[![npm](https://img.shields.io/npm/v/react-native-live-audio-stream)](https://www.npmjs.com/package/react-native-live-audio-stream)

Get live audio stream data for React Native with **background recording support**. Ideal for live voice recognition, transcribing, and continuous audio monitoring.

## ⭐ Key Features

- ✅ **Background Audio Recording** - Continue recording when app is backgrounded
- ✅ **Cross-platform** - iOS and Android support
- ✅ **Live Streaming** - Real-time audio data chunks via events
- ✅ **Memory Efficient** - No file operations, direct data streaming
- ✅ **Configurable** - Adjustable sample rate, channels, buffer size
- ✅ **Battery Optimized** - Proper background task and service management

## 📖 About

This library is forked from [xiqi/react-native-live-audio-stream](https://github.com/xiqi/react-native-live-audio-stream) and enhanced with **background audio recording capabilities**.

**What's New:**
- **Android**: Foreground service with wake lock and battery optimization awareness
- **Enhanced Documentation**: Comprehensive setup and troubleshooting guides

## ⚠️ Compatibility

- **React Native**: 0.60+ (uses autolinking)
- **New Architecture**: Not supported yet (Fabric/TurboModules)
- **iOS**: 10.0+
- **Android**: API 16+ (Android 4.1+)

## Install
```
yarn add react-native-live-audio-record
cd ios
pod install
```

## Permissions and Setup

### iOS
Add these lines to ```ios/[YOUR_APP_NAME]/info.plist```
```xml
<key>NSMicrophoneUsageDescription</key>
<string>This app needs access to microphone to record audio for streaming.</string>

<!-- Background modes for audio recording -->
<key>UIBackgroundModes</key>
<array>
    <string>audio</string>
    <string>background-processing</string>
</array>

<!-- Required device capabilities -->
<key>UIRequiredDeviceCapabilities</key>
<array>
    <string>microphone</string>
</array>
```

### Android
Add the following permissions to ```android/app/src/main/AndroidManifest.xml```
```xml
<!-- Required permissions -->
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Service declaration -->
<application>
    <service
        android:name="com.imxiqi.rnliveaudiostream.RNLiveAudioStreamService"
        android:enabled="true"
        android:exported="false"
        android:foregroundServiceType="microphone" />
</application>
```

### Background Recording Support
This library now supports **background audio recording** on both platforms:

- **Android**: Uses foreground service with persistent notification

**Important**: For reliable background recording, users should:
1. **iOS**: Enable "Background App Refresh" for your app
2. **Android**: Disable battery optimization for your app in device settings

## Usage
```javascript
import LiveAudioStream from 'react-native-live-audio-record';
// yarn add buffer
import { Buffer } from 'buffer';

const options = {
  sampleRate: 32000,  // default is 44100 but 32000 is adequate for accurate voice recognition
  channels: 1,        // 1 or 2, default 1
  bitsPerSample: 16,  // 8 or 16, default 16
  audioSource: 6,     // android only (see below)
  bufferSize: 4096    // default is 2048
};

LiveAudioStream.init(options);
LiveAudioStream.on('data', data => {
  // base64-encoded audio data chunks
  var chunk = Buffer.from(data, 'base64');
});
  ...
LiveAudioStream.start();
  ...
LiveAudioStream.stop();
  ...
```

`audioSource` should be one of the constant values from [here](https://developer.android.com/reference/android/media/MediaRecorder.AudioSource). Default value is `6` (`VOICE_RECOGNITION`).

## Contributing

Feel free to submit issues and pull requests. For major changes, please open an issue first to discuss what you would like to change.

### Development Setup
```bash
git clone <your-fork>
cd react-native-live-audio-record
npm install

# Test on example project
cd example
npm install
npx react-native run-ios
npx react-native run-android
```

## Credits/References

**Original Library:**
- [xiqi/react-native-live-audio-stream](https://github.com/xiqi/react-native-live-audio-stream) - Base implementation

**Background Recording Enhancements:**
- [react-native-audio-record](https://github.com/goodatlas/react-native-audio-record) - Audio recording concepts
- iOS [Audio Queues](https://developer.apple.com/library/content/documentation/MusicAudio/Conceptual/AudioQueueProgrammingGuide) - iOS audio implementation
- Android [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord.html) - Android audio API
- [Background Tasks](https://developer.apple.com/documentation/uikit/app_and_environment/scenes/preparing_your_ui_to_run_in_the_background) - iOS background processing
- [Foreground Services](https://developer.android.com/guide/components/foreground-services) - Android background services

**Additional References:**
- [cordova-plugin-audioinput](https://github.com/edimuj/cordova-plugin-audioinput)
- [react-native-recording](https://github.com/qiuxiang/react-native-recording)
- [SpeakHere](https://github.com/shaojiankui/SpeakHere)
- [ringdroid](https://github.com/google/ringdroid)

## License
MIT

## Support

- 📖 [Background Recording Guide](./BACKGROUND_RECORDING.md) - Detailed technical documentation
- 🐛 [Issues](https://github.com/your-repo/react-native-live-audio-record/issues) - Bug reports and feature requests
- 💬 [Discussions](https://github.com/your-repo/react-native-live-audio-record/discussions) - Community support

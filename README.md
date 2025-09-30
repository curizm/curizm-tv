# Curizm TV - Native Android App

A native Android TV application that recreates the web-based Curizm TV experience with better performance and TV optimization.

## Features

### QR Setup Activity
- **Native QR Code Generation**: Uses ZXing library for fast QR code generation
- **Real-time WebSocket Connection**: Connects to Curizm API for session management
- **Connection Code Display**: Shows 6-digit pairing code with countdown timer
- **TV-Optimized UI**: Designed specifically for TV displays with appropriate sizing and colors
- **Auto-retry Logic**: Automatically retries failed connections

### Video Receiver Activity
- **HLS Video Playback**: Uses ExoPlayer for robust HLS streaming
- **WebSocket Control**: Real-time commands from mobile controller
- **Subtitle Support**: SRT and VTT subtitle conversion and display
- **Artwork Overlays**: Shows artwork information with auto-hide functionality
- **Background Music**: Separate audio track support with volume control
- **Playlist Management**: Dynamic playlist updates via WebSocket
- **TV-Optimized Controls**: Hidden controls for clean TV experience

## Architecture

### Activities
1. **MainActivity**: Launcher that determines whether to show QR setup or video receiver
2. **QRSetupActivity**: TV pairing interface with QR code and connection code
3. **VideoReceiverActivity**: Main video playback interface

### Key Components
- **ExoPlayer**: For HLS video streaming
- **Socket.IO**: WebSocket communication with Curizm API
- **ZXing**: QR code generation
- **OkHttp**: HTTP requests for API calls
- **Gson**: JSON parsing

### WebSocket Events
- `HELLO`: Initial connection with role and credentials
- `SET_PLAYLIST`: Receive playlist from controller
- `COMMAND`: Receive playback commands (play, pause, next, etc.)
- `HEARTBEAT`: Send status updates to controller

### Supported Commands
- `PLAY` / `PAUSE`: Playback control
- `NEXT` / `PREV`: Navigate playlist
- `JUMP_TO_INDEX`: Jump to specific playlist item
- `SEEK`: Seek to specific time
- `SET_VIDEO_VOLUME` / `SET_BGM_VOLUME`: Volume control
- `SET_CAPTIONS`: Toggle subtitles

## TV Optimizations

### Performance
- Hardware-accelerated video playback with ExoPlayer
- Efficient WebSocket connection management
- Optimized layouts for TV displays
- Reduced animations for better TV compatibility

### User Experience
- Fullscreen video playback
- Auto-hiding UI elements
- TV-appropriate text sizes and spacing
- Landscape orientation lock
- Screen keep-awake during playback

### Reset Functionality
- **Konami Code Reset**: D-pad sequence (↑↑↓↓←→←→) to reset to QR setup
- Clears saved configuration and returns to pairing mode

## Configuration Storage
The app uses SharedPreferences to store:
- WebSocket URL
- Company name and secret code
- API endpoint URL
- Configuration state

## Dependencies

```kotlin
// Video playback
implementation "com.google.android.exoplayer:exoplayer-core:2.19.1"
implementation "com.google.android.exoplayer:exoplayer-hls:2.19.1"
implementation "com.google.android.exoplayer:exoplayer-ui:2.19.1"

// WebSocket communication
implementation "io.socket:socket.io-client:2.1.1"
implementation "com.squareup.okhttp3:okhttp:4.12.0"

// QR Code generation
implementation "com.google.zxing:core:4.3.0"
implementation "com.journeyapps:zxing-android-embedded:4.3.0"

// JSON parsing
implementation "com.google.code.gson:gson:2.10.1"
```

## Requirements

- **Android 7.0 (API level 24) or higher**
- Android TV device or Android device with TV capabilities

## Installation

1. Build the APK using Android Studio or Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
2. Install the generated APK (`app/build/outputs/apk/debug/app-debug.apk`) on your Android TV device
3. Launch the app - it will show QR setup on first run
4. Use mobile device to scan QR code or enter connection code
5. App will automatically switch to video receiver mode once paired

## Usage Flow

1. **First Launch**: Shows QR Setup Activity
2. **Mobile Pairing**: User scans QR or enters code on mobile
3. **Configuration**: App receives WebSocket URL and room parameters
4. **Video Mode**: Switches to Video Receiver Activity
5. **Subsequent Launches**: Directly opens Video Receiver with saved config
6. **Reset**: Use D-pad sequence to return to QR setup

## Advantages over WebView Approach

### Performance
- Native video decoding with ExoPlayer vs web video
- Direct WebSocket connections vs browser WebSocket
- No web rendering overhead
- Better memory management

### TV Compatibility
- Native Android TV optimizations
- Better remote control handling
- Proper fullscreen video playback
- Hardware acceleration support

### Reliability
- More robust error handling
- Better connection management
- Native lifecycle management
- Proper background/foreground handling

### Features
- Native subtitle rendering
- Better audio mixing capabilities
- TV-specific UI adaptations
- Offline capability for cached content

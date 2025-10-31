# Curizm TV - Android TV App

A native Android TV application for video exhibition playback with mobile controller integration.

**Version**: 1.6.0  
**Platform**: Android TV (API 24+)  
**Language**: Kotlin  

---


AndroidTV Seller Website: https://play.google.com/console/u/0/developers/8872487054887407323/app-list

## 🎯 Overview

Curizm TV는 전시회 영상을 TV에서 재생하는 네이티브 앱입니다. 모바일 기기로 QR 코드 또는 연결 코드를 스캔하여 TV를 페어링하고, WebSocket을 통해 실시간으로 제어합니다.

**Part of Multi-Platform Suite:**
- ✅ Android TV (this project)
- ✅ Samsung TV (Tizen) - `/home/leo/curizm-tv-samsung`
- ✅ LG TV (webOS) - `/home/leo/curizm-tv-lg`

All 3 apps are functionally identical and use the same backend API.

---

## 📱 Features

### QR Setup Activity
- **QR Code Generation**: ZXing library로 실시간 QR 생성
- **6-Digit Connection Code**: 수동 입력용 연결 코드
- **Countdown Timer**: 3분 타이머 (자동 갱신)
- **WebSocket Connection**: Curizm API 서버와 실시간 연결
- **Auto-retry**: 연결 실패 시 자동 재시도
- **Status Indicator**: 연결 상태 시각적 표시

### Video Receiver Activity
- **HLS Streaming**: ExoPlayer로 HLS (m3u8) 비디오 재생
- **WebSocket Control**: 모바일 컨트롤러로 실시간 제어
- **Subtitle Support**: SRT/VTT 자막 자동 로드 및 표시
- **Background Music**: 별도 BGM 트랙 재생 (볼륨 독립 조절)
- **Artwork Overlay**: 작품 정보 오버레이 (자동 숨김)
- **Playlist Management**: 동적 플레이리스트 업데이트
- **Heartbeat**: 2초마다 상태 업데이트 전송
- **Status Pills**: 룸 정보 및 WebSocket 연결 상태 표시

### Reset Functionality
- **Konami Code**: D-pad 입력 (↑↑↓↓←→←→)으로 설정 초기화
- QR 설정 화면으로 복귀

---

## 🏗️ Architecture

### Activities

**1. MainActivity** (Entry Point)
- 저장된 설정 확인
- 설정 있으면 → VideoReceiverActivity
- 설정 없으면 → QRSetupActivity
- Konami 코드 리셋 처리

**2. QRSetupActivity** (Setup Page)
```kotlin
Flow:
1. API 호출하여 세션 생성
2. QR 코드 및 연결 코드 표시
3. WebSocket 연결
4. 모바일 페어링 대기
5. 설정 완료 시 VideoReceiverActivity로 전환
```

**3. VideoReceiverActivity** (Video Player)
```kotlin
Flow:
1. WebSocket 연결
2. 플레이리스트 수신
3. 첫 번째 비디오 자동 재생
4. 명령 수신 및 처리
5. 하트비트 전송 (2초마다)
```

### Key Components

| Component | Purpose | Library |
|-----------|---------|---------|
| **ExoPlayer** | HLS 비디오 스트리밍 | `com.google.android.exoplayer2` |
| **Socket.IO** | WebSocket 통신 | `io.socket:socket.io-client` |
| **ZXing** | QR 코드 생성 | `com.journeyapps:zxing-android-embedded` |
| **OkHttp** | HTTP API 호출 | `com.squareup.okhttp3:okhttp` |
| **Gson** | JSON 파싱 | `com.google.code.gson:gson` |
| **ViewBinding** | UI 바인딩 | Android built-in |

### File Structure

```
app/src/main/
├── java/io/curizm/tv/
│   ├── MainActivity.kt              # Entry point (142 lines)
│   ├── QRSetupActivity.kt           # QR setup (225 lines)
│   └── VideoReceiverActivity.kt     # Video player (782 lines)
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── activity_qr_setup.xml    # QR setup UI
│   │   └── activity_video_receiver.xml  # Video player UI
│   ├── drawable/
│   │   ├── curizm.png               # 32x32 logo
│   │   ├── gradient_background.xml
│   │   ├── glass_card_background.xml
│   │   ├── code_display_background.xml
│   │   ├── timer_circle_background.xml
│   │   ├── status_indicator_*.xml
│   │   └── ... (12 drawables)
│   └── values/
│       ├── strings.xml
│       └── themes.xml
│
└── AndroidManifest.xml              # App configuration
```

---

## 🔌 WebSocket Protocol

### Connection

**Server**: `wss://api.curizm.io` (Socket.IO)

### Events: TV → Server

**HELLO** (Authentication)
```json
{
  "role": "receiver",
  "companyName": "company-name",
  "secretCode": "room-secret"
}
```

**HEARTBEAT** (Status Update - Every 2 seconds)
```json
{
  "ts": 1697203200000,
  "idx": 2,
  "paused": false,
  "t": 45.6
}
```

**TV_JOIN_SESSION** (Session Join - QR Setup only)
```json
{
  "sessionId": "abc123..."
}
```

### Events: Server → TV

**SET_PLAYLIST** (Receive Playlist)
```json
{
  "items": [
    {
      "video": "https://cdn.example.com/video.m3u8",
      "audio": "https://cdn.example.com/bgm.mp3",
      "subtitle": "https://cdn.example.com/subs.srt",
      "artworkName": "작품명",
      "artistName": "작가명",
      "size": "100x150cm",
      "material": "Oil on Canvas",
      "order": 1
    }
  ]
}
```

**COMMAND** (Playback Control)
```json
{
  "action": "PLAY",
  "value": null,
  "startAt": 1697203200000
}
```

**TV_SETUP_COMPLETE** (Configuration Complete - QR Setup only)
```json
{
  "redirectUrl": "receiver.html?ws=wss://...&companyName=...&secretCode=..."
}
```

### Supported Commands

| Command | Value Type | Description |
|---------|------------|-------------|
| `PLAY` | null | 재생 |
| `PAUSE` | null | 일시정지 |
| `NEXT` | null | 다음 영상 |
| `PREV` | null | 이전 영상 |
| `JUMP_TO_INDEX` | number | 특정 인덱스로 이동 |
| `SEEK` | number (seconds) | 특정 시간으로 이동 |
| `SET_VIDEO_VOLUME` | number (0-1) | 비디오 볼륨 |
| `SET_BGM_VOLUME` | number (0-1) | 배경음악 볼륨 |
| `SET_CAPTIONS` | boolean | 자막 ON/OFF |

---

## 🎨 UI Design

### Color Palette (Curizm Brand Colors)

```kotlin
curizm_charcoal = #352B2B    // Background
curizm_coral = #FF5935       // Accent (buttons, text)
curizm_cream = #FFFBE9       // Primary text
curizm_forest = #3F5743      // Secondary (timer)
curizm_cream_dark = #F5F0D9  // Secondary text
```

### QR Setup Layout

```
┌────────────────────────────────────────┐
│  🔸 Curizm TV                          │
│     Setup your TV display              │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │  Enter Code    |   Scan QR Code  │  │
│  │                |                 │  │
│  │  Go to www...  |    ┌────────┐  │  │
│  │                |    │ [QR]   │  │  │
│  │  ⏱️ ABCD12     |    └────────┘  │  │
│  └─────────────────────────────────┘  │
│                                        │
│  ● Connected - Waiting for setup...   │
└────────────────────────────────────────┘
```

### Video Receiver Layout

```
┌────────────────────────────────────────┐
│  Artwork Title          [Room] [WS]    │ ← Info Overlay (top-left)
│  Artist • Size • Material              │   Status Pills (bottom-right)
│                                        │
│          [Full Screen Video]           │
│                                        │
│                                        │
└────────────────────────────────────────┘
```

---

## 🛠️ Technical Details

### Configuration Storage

**SharedPreferences** (`curizm_tv_prefs`)

```kotlin
KEY_HAS_RECEIVER_CONFIG: Boolean  // 설정 존재 여부
KEY_WS_URL: String                // WebSocket URL
KEY_COMPANY_NAME: String          // 회사명
KEY_SECRET_CODE: String           // 비밀번호
KEY_API_URL: String               // API endpoint
```

### ExoPlayer Configuration

```kotlin
DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        5000,   // minBufferMs
        25000,  // maxBufferMs
        1000,   // bufferForPlaybackMs
        3000    // bufferForPlaybackAfterRebufferMs
    )
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()
```

**Video Scaling**: `RESIZE_MODE_FIXED_HEIGHT`  
**Surface Type**: `SurfaceView` (better emulator performance)

### Subtitle Handling

```kotlin
// Auto-detect MIME type from URL
val mimeType = when {
    url.contains(".srt") -> MimeTypes.APPLICATION_SUBRIP
    url.contains(".vtt") -> MimeTypes.TEXT_VTT
    else -> MimeTypes.APPLICATION_SUBRIP
}

// Apply as MediaItem subtitle
val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
    .setMimeType(mimeType)
    .setLanguage("ko")
    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
    .build()
```

### Background Music

별도 ExoPlayer 인스턴스 사용:
```kotlin
bgmPlayer = ExoPlayer.Builder(this).build()
bgmPlayer?.volume = 0.25f
bgmPlayer?.repeatMode = Player.REPEAT_MODE_ALL
```

---

## 📦 Dependencies

```kotlin
// build.gradle.kts (app level)
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    
    // ExoPlayer (Video)
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    
    // WebSocket
    implementation("io.socket:socket.io-client:2.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // QR Code
    implementation("com.google.zxing:core:4.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### Version Catalog

`gradle/libs.versions.toml` contains all version definitions.

---

## 🔧 Build & Deployment

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 11 or 17
- **Gradle**: 8.13.0 (auto-downloaded via wrapper)
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36 (Android 14)

### Build Debug APK

```bash
cd /home/leo/curizm-tv
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

**Signing Configuration:**
```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file("../curizm-tv-release.keystore")
        storePassword = "curizm123"
        keyAlias = "curizm-tv"
        keyPassword = "curizm123"
    }
}
```

⚠️ **Critical**: Keystore files (`curizm-tv-release.keystore`, `curizm-tv-upload.keystore`) are required for all future updates. Never delete them!

### Build App Bundle (for Play Store)

```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Install on Device

```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Via Android Studio
Run → Run 'app'
```

### Clean Build

```bash
./gradlew clean
./gradlew assembleRelease
```

---

## 📲 Installation & Usage

### First Launch

1. Install APK on Android TV
2. Launch app
3. QR setup screen appears
4. User scans QR or enters 6-digit code on mobile
5. Mobile sends configuration via WebSocket
6. TV automatically switches to video player
7. Videos start playing

### Subsequent Launches

- App directly opens video player
- Reconnects to WebSocket with saved config
- Resumes normal operation

### Reset to Setup

Press D-pad sequence on remote: **↑ ↑ ↓ ↓ ← → ← →**

Clears all saved configuration and returns to QR setup.

---

## 🌐 Backend Integration

### API Endpoints

**Generate Session** (QR Setup)
```
GET https://api.curizm.io/api/v1/tv/generate-session

Response:
{
  "sessionId": "abc123...",
  "websocketUrl": "wss://api.curizm.io",
  "connectionCode": "ABCD12"
}
```

**Get Playlist** (Optional - can also receive via WebSocket)
```
GET https://api.curizm.io/api/v1/exhibition/tv?companyName=X&secretCode=Y

Response:
{
  "playlist": [ ... ]
}
```

### WebSocket Flow

```
1. TV connects to WebSocket
2. TV sends HELLO with credentials
3. Server acknowledges connection
4. Controller sends SET_PLAYLIST
5. TV loads and plays videos
6. Controller sends COMMAND events
7. TV sends HEARTBEAT every 2 seconds
```

---

## 🎮 Remote Control

### D-pad Navigation
- ↑↓←→ keys for navigation
- Center/Select for OK
- Back button for back

### Special Sequences
- **Reset**: ↑↑↓↓←→←→ (Konami Code)

### Media Keys (if available)
- Play/Pause buttons work in video player
- Volume buttons control device volume

---

## 🐛 Troubleshooting

### Video Won't Play

**Check:**
1. HLS URL is accessible (ends with .m3u8)
2. Network connectivity
3. SSL certificate (app bypasses SSL for emulator)
4. ExoPlayer logs in Logcat

**Solution:**
```kotlin
// SSL bypass is enabled in VideoReceiverActivity.kt (lines 482-495)
// For production, ensure valid SSL certificates on video CDN
```

### WebSocket Disconnects

**Check:**
1. WebSocket URL is correct
2. Network is stable
3. Server is running

**Auto-reconnect:**
```kotlin
// Implemented in VideoReceiverActivity.kt (lines 344-346)
socket?.on(Socket.EVENT_DISCONNECT) {
    runOnUiThread {
        handler.postDelayed({ connectWebSocket() }, 1500)
    }
}
```

### QR Code Doesn't Generate

**Check:**
1. Internet connectivity
2. API server is accessible
3. ZXing library is included

**Logs:**
```bash
adb logcat | grep QRSetup
```

### App Crashes on Launch

**Check:**
1. Permissions in AndroidManifest.xml
2. Dependencies are synced
3. Gradle build successful

**Clean rebuild:**
```bash
./gradlew clean
./gradlew assembleDebug
```

---

## 🚀 Google Play Store Deployment

### 1. Prepare Release

**Update Version:**
```kotlin
// app/build.gradle.kts
versionCode = 7        // Increment
versionName = "1.7"    // Update
```

**Build Bundle:**
```bash
./gradlew bundleRelease
```

### 2. Google Play Console

1. Go to: https://play.google.com/console
2. Select app (or create new app)
3. Production → Create new release
4. Upload `app-release.aab`
5. Add release notes
6. Submit for review

### 3. Required Materials

- **App Bundle**: .aab file
- **Screenshots**: 1920x1080, 3장 이상
- **Privacy Policy**: URL 필요
- **Content Rating**: 설문 완료
- **Store Listing**: 제목, 설명 (Korean + English)

### 4. Review Process

- **Review Time**: 1-3 days
- **Status**: Check in Play Console
- **Updates**: Incremental rollout recommended

---

## 🧪 Testing

### Android TV Emulator

```bash
# In Android Studio
Tools → AVD Manager
Create Virtual Device → TV → 1080p TV
Start Emulator
Run app
```

### Real Device Testing

```bash
# Enable ADB on Android TV
Settings → Device Preferences → Developer Options → USB Debugging

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "MainActivity|QRSetup|VideoReceiver"
```

### Test Checklist

- [ ] QR code generates correctly
- [ ] Connection code displays (6 digits)
- [ ] Timer counts down from 3:00
- [ ] WebSocket connects (green indicator)
- [ ] Mobile pairing works
- [ ] Redirects to video player
- [ ] Video plays (HLS stream)
- [ ] Subtitles display (if present)
- [ ] BGM plays (if present)
- [ ] Artwork overlay shows and hides
- [ ] PLAY/PAUSE commands work
- [ ] NEXT/PREV commands work
- [ ] Volume controls work
- [ ] Reset sequence works (↑↑↓↓←→←→)
- [ ] Configuration persists on restart

---

## 🔐 Security & Credentials

### Keystore Files

**Release Keystore** (Primary)
```
File: curizm-tv-release.keystore
Password: curizm123
Alias: curizm-tv
Key Password: curizm123
```

**Upload Keystore** (Play Store)
```
File: curizm-tv-upload.keystore
Password: curizm123
```

**Backup Keystore**
```
File: curizm-tv-new.keystore
Password: curizm123
```

⚠️ **CRITICAL**: These keystores are required for ALL future app updates. If lost, you cannot update the app on Play Store and must create a new app with a different package name.

### Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" tools:node="remove" />
```

**Note**: Camera permission is explicitly removed as we generate QR codes, not scan them.

---

## 📊 Performance Optimizations

### Video Playback
- ExoPlayer buffer optimization (5-25 second buffer)
- HLS adaptive bitrate streaming
- Hardware-accelerated decoding
- SurfaceView for better emulator performance
- Periodic layout refresh for x86 emulator stability (every 5 seconds)

### Network
- WebSocket connection pooling
- Auto-reconnect with exponential backoff (1.5 seconds)
- Heartbeat throttling (2-second intervals)

### Memory
- Player release on destroy
- Bitmap optimization for QR codes
- Efficient ViewBinding (no findViewById)

### UI/UX
- Minimal DOM/view updates
- Hardware-accelerated animations (ObjectAnimator)
- Auto-hide overlays to reduce on-screen elements
- Landscape orientation lock
- Keep screen on during playback

---

## 🆚 Platform Comparison

| Feature | Android TV | Samsung TV | LG TV |
|---------|-----------|------------|-------|
| **Language** | Kotlin | JavaScript | JavaScript |
| **Video API** | ExoPlayer | Samsung AVPlay | HTML5 |
| **Package** | .apk / .aab | .wgt | .ipk |
| **Size** | 12 MB | 300 KB | 200 KB |
| **Store** | Google Play | Samsung Apps | LG Content Store |
| **Development** | Android Studio | Tizen Studio | webOS CLI |

**All 3 apps provide identical functionality.**

---

## 📝 Code Statistics

```
Lines of Code:
  MainActivity.kt:          142 lines
  QRSetupActivity.kt:       225 lines
  VideoReceiverActivity.kt: 782 lines
  Total Kotlin:            ~1,149 lines

Resources:
  Layouts:     3 XML files
  Drawables:   12 XML files
  Values:      2 XML files
  
Total Project Size: 1.5 MB (excluding build artifacts)
```

---

## 🔄 Maintenance

### Adding New Commands

1. **Update handleCommand() in VideoReceiverActivity.kt:**
```kotlin
when (action) {
    "NEW_COMMAND" -> {
        // Implementation
        Log.d("VideoReceiver", "New command: $value")
    }
    // ... existing commands
}
```

2. **Test thoroughly**
3. **Update version**
4. **Deploy**

### Updating Dependencies

1. Edit `gradle/libs.versions.toml`
2. Sync Gradle
3. Test thoroughly
4. Build release

### Version Update Process

1. **Update version:**
   ```kotlin
   // app/build.gradle.kts
   versionCode = X + 1
   versionName = "1.X"
   ```

2. **Build:**
   ```bash
   ./gradlew clean bundleRelease
   ```

3. **Test:**
   - Install on test device
   - Verify all features work

4. **Submit to Play Store**

---

## 🆘 Emergency Fixes

### Critical Bug Process

1. **Identify issue** (logs, crash reports)
2. **Fix code** in Kotlin files
3. **Test locally** on device/emulator
4. **Increment version** (versionCode++)
5. **Build release** bundle
6. **Submit to Play Store** with expedited review request

### Rollback

Google Play Console allows rollback to previous versions in Production → Releases.

---

## 📚 Related Documentation

- `TECHNICAL_ARCHITECTURE.md` - 상세 기술 문서
- `DEPLOYMENT_GUIDE.md` - 배포 가이드
- `TROUBLESHOOTING_GUIDE.md` - 문제 해결 가이드
- `USER_GUIDE.md` - 사용자 가이드
- `PRIVACY_POLICY.md` - 개인정보 처리방침
- `/home/leo/CURIZM_TV_HANDOVER.md` - Complete handover doc (all 3 platforms)

---

## 🎯 Quick Reference

### Build Commands
```bash
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK
./gradlew bundleRelease        # Release AAB (Play Store)
./gradlew clean                # Clean build
```

### ADB Commands
```bash
adb devices                    # List devices
adb install app-debug.apk      # Install
adb uninstall io.curizm.tv     # Uninstall
adb logcat | grep Curizm       # View logs
```

### Package Info
```bash
Namespace:       io.curizm.tv
Application ID:  io.curizm.tv
Version Code:    6
Version Name:    1.6
Min SDK:         24 (Android 7.0)
Target SDK:      36 (Android 14)
```

---

## ✅ Production Ready

- ✅ Clean code only
- ✅ All features implemented
- ✅ Keystores secured
- ✅ Documentation complete
- ✅ Ready for Google Play Store

**For complete handover documentation covering all 3 platforms, see:**  
`/home/leo/CURIZM_TV_HANDOVER.md`

---

**Copyright © 2025 Curizm**  
**License**: Proprietary

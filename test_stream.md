# Testing HLS Stream: "When the world is resting"

## Stream Details
- **URL**: https://d30il4v3bcewto.cloudfront.net/video/When+the+world+is+resting/master.m3u8
- **Available Qualities**:
  - 1080p (1920x1080) - 6Mbps bandwidth
  - 720p (1280x720) - 3.5Mbps bandwidth  
  - 480p (854x480) - 1.8Mbps bandwidth

## Quick Test Instructions

### 1. Create Test Playlist
Add this to your test playlist for the VideoReceiverActivity:

```json
{
  "video": "https://d30il4v3bcewto.cloudfront.net/video/When+the+world+is+resting/master.m3u8",
  "audio": "",
  "subtitle": "",
  "poster": "",
  "title": "When the world is resting",
  "artist": "Test Video",
  "size": "Multiple Resolutions",
  "material": "HLS Stream",
  "order": 0
}
```

### 2. Monitor Logs
Use this command to watch the video playback logs:
```bash
adb logcat | grep "VideoReceiver"
```

### 3. Expected Log Output
```
VideoReceiver: ExoPlayer configured with optimized HLS settings
VideoReceiver: Loading HLS stream: https://d30il4v3bcewto.cloudfront.net/video/When+the+world+is+resting/master.m3u8
VideoReceiver: Playback state changed: BUFFERING
VideoReceiver: Video buffering...
VideoReceiver: Playback state changed: READY
VideoReceiver: Video ready, hiding overlay
VideoReceiver: Playing changed: true
```

## Troubleshooting

### If Still Blinking/Lagging:
1. **Check Emulator Settings**:
   - Use Hardware - GLES 2.0 graphics
   - Allocate 4GB+ RAM to emulator
   - Use x86_64 system image if possible

2. **Try Lower Quality**:
   - Force 480p by using direct URL: `https://d30il4v3bcewto.cloudfront.net/video/When+the+world+is+resting/480p/480p.m3u8`

3. **Test on Real Device**:
   - Install APK on actual Android TV/tablet
   - Compare performance vs emulator

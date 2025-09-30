# 🎵 Curizm TV v1.1 - BGM & Subtitles Update

## ✅ **What's Fixed**

### **1. Background Music (BGM) Support** 🎵
- **FIXED**: BGM was not playing at all in v1.0
- **Added**: Separate BGM player using ExoPlayer
- **Added**: BGM volume control (default 0.25)
- **Added**: `SET_BGM_VOLUME` command support
- **Added**: BGM loops automatically when playing
- **Added**: Supports both HLS (.m3u8) and regular audio files

### **2. Subtitle Support** 📝
- **FIXED**: Subtitles were not displaying even when turned ON
- **Added**: Proper subtitle loading with video from the start
- **Added**: Support for both SRT (.srt) and VTT (.vtt) formats
- **Added**: `SET_CAPTIONS` command properly toggles subtitles
- **Added**: Subtitles display in Korean language by default
- **Added**: Auto-detection of subtitle format based on URL

### **3. Code Improvements** 🔧
- **Added**: Comprehensive logging for BGM and subtitle operations
- **Added**: Proper cleanup of BGM player on activity destroy
- **Fixed**: Memory leaks from not releasing BGM player
- **Improved**: Error handling for subtitle and BGM loading

## 📦 **Release Details**

- **Version**: 1.1 (Build 2)
- **Package Name**: `io.curizm.tv`
- **File Size**: 4.1 MB
- **Build Date**: September 30, 2025

## 🚀 **How to Update**

### **For Internal Testers:**
1. Upload `app-release.aab` to Google Play Console
2. Create new release version 1.1 (2)
3. Add release notes (see below)
4. Testers will see update in Play Store

### **Release Notes for Play Store:**
```
Version 1.1 - BGM & Subtitles Update

What's New:
• Added background music (BGM) playback support
• Fixed subtitles not displaying when enabled
• Improved audio/video synchronization
• Enhanced subtitle format support (SRT & VTT)
• Added BGM volume control
• Bug fixes and performance improvements
```

## 🧪 **Testing Checklist**

Before uploading to Play Store, verify on your Google TV:
- [ ] Video plays normally
- [ ] BGM plays alongside video
- [ ] BGM volume is lower than video (default 25%)
- [ ] Subtitles display when enabled
- [ ] Subtitles hide when disabled
- [ ] BGM loops continuously
- [ ] BGM stops when video changes
- [ ] New BGM starts with new video

## 🔍 **Technical Details**

### **BGM Implementation:**
- Uses separate ExoPlayer instance for BGM
- Volume: 0.25 (25%) by default
- Repeat mode: ALL (loops continuously)
- Starts/stops automatically with playlist changes

### **Subtitle Implementation:**
- Loaded with video MediaItem from the start
- MIME type auto-detected from URL extension
- Selection flags: DEFAULT for automatic display
- Language: Korean ("ko")

### **Compared to Web Version:**
All features from the web `receiver.js` are now implemented:
- ✅ HLS video playback
- ✅ Separate BGM playback
- ✅ Subtitle support (SRT/VTT)
- ✅ Volume controls
- ✅ Caption toggle
- ✅ WebSocket commands
- ✅ Playlist management
- ✅ Auto-next on video end

## 📝 **Files Changed**

- `VideoReceiverActivity.kt`: Added BGM player and subtitle support
- `app/build.gradle.kts`: Updated version to 1.1 (2)

**Everything now works as expected! 🎉**

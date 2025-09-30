# 🚀 Curizm TV - Google Play Store Deployment Guide

## 📋 **Prerequisites**

1. **Google Play Console Account** - You need a Google Play Developer account ($25 one-time fee)
2. **App Signing Key** - For production releases
3. **App Bundle** - Google Play requires AAB format (not APK)

## 🔧 **Step 1: Generate App Bundle (AAB)**

The release APK is ready, but Google Play Store requires an Android App Bundle (AAB):

```bash
cd /home/leo/curizm-tv
./gradlew bundleRelease
```

This creates: `app/build/outputs/bundle/release/app-release.aab`

## 🔑 **Step 2: App Signing Setup**

### **Option A: Google Play App Signing (Recommended)**
- Google manages your signing key
- More secure and convenient
- Upload your AAB and Google handles the rest

### **Option B: Manual Signing**
If you want to manage your own key:

```bash
# Generate keystore
keytool -genkey -v -keystore curizm-tv-release.keystore -alias curizm-tv -keyalg RSA -keysize 2048 -validity 10000

# Update build.gradle.kts with signing config
```

## 📱 **Step 3: Google Play Console Setup**

### **3.1 Create New App**
1. Go to [Google Play Console](https://play.google.com/console)
2. Click "Create app"
3. Fill in:
   - **App name**: "Curizm TV"
   - **Default language**: English (or your preference)
   - **App or game**: App
   - **Free or paid**: Free (or Paid if you want to monetize)

### **3.2 App Category**
- **Category**: Entertainment
- **Content rating**: Complete the questionnaire (likely "Everyone" or "Teen")

### **3.3 Store Listing**
Required information:
- **App name**: Curizm TV
- **Short description**: "Smart TV app for video streaming and remote control"
- **Full description**: 
  ```
  Curizm TV is a native Android TV application that provides seamless video streaming and remote control capabilities. Connect your mobile device to control video playback, manage playlists, and enjoy high-quality HLS video streaming on your TV.
  
  Features:
  • QR code pairing for easy setup
  • Real-time video control via WebSocket
  • HLS video streaming support
  • Playlist management
  • Background music control
  • Subtitle support
  • Optimized for Android TV devices
  ```

### **3.4 Graphics Assets**
You'll need:
- **App icon**: 512x512 PNG (use your curizm.png)
- **Feature graphic**: 1024x500 PNG
- **Screenshots**: 1920x1080 for TV (take from emulator)
- **TV banner**: 1280x720 PNG

### **3.5 App Content**
- **Target audience**: Adults
- **Content rating**: Complete questionnaire
- **Data safety**: Declare data collection practices
- **Ads**: Specify if you show ads (likely "No")

## 📦 **Step 4: Upload Release**

### **4.1 Create Release**
1. Go to "Production" → "Releases"
2. Click "Create new release"
3. Upload your `app-release.aab` file
4. Fill in release notes:
   ```
   Initial release of Curizm TV
   
   Features:
   • QR code pairing setup
   • Video streaming with HLS support
   • Real-time remote control
   • Playlist management
   • Background music control
   ```

### **4.2 Release Configuration**
- **Release type**: Production
- **Release name**: 1.0 (1)
- **Release notes**: (as above)
- **App bundles and APKs**: Upload your AAB

## 🔍 **Step 5: Review and Publish**

### **5.1 Pre-launch Report**
- Google will run automated tests
- Review any issues found
- Fix critical issues before publishing

### **5.2 Content Policy**
- Ensure compliance with Google Play policies
- No copyright violations
- Appropriate content rating

### **5.3 Publish**
- Click "Review release"
- If everything looks good, click "Start rollout to production"
- App will be available in 2-3 hours

## 📱 **Step 6: Install on Google TV**

Once published:
1. Open Google Play Store on your Google TV
2. Search for "Curizm TV"
3. Install the app
4. Launch and test!

## 🛠 **Troubleshooting**

### **Common Issues:**
1. **App not found**: Wait 2-3 hours after publishing
2. **Installation failed**: Check device compatibility (Android TV only)
3. **Video not playing**: Test on real device (emulator issues)

### **Testing Checklist:**
- [ ] QR code generation works
- [ ] WebSocket connection establishes
- [ ] Video plays correctly
- [ ] Remote control commands work
- [ ] App doesn't crash on real device

## 📊 **Post-Launch**

### **Monitor:**
- Crash reports in Play Console
- User reviews and ratings
- Download statistics
- Performance metrics

### **Updates:**
- Increment `versionCode` for each update
- Update `versionName` for user-facing version
- Test thoroughly before releasing updates

## 🎯 **Quick Commands**

```bash
# Build release AAB
./gradlew bundleRelease

# Check AAB file
ls -la app/build/outputs/bundle/release/

# Install release APK for testing
adb install app/build/outputs/apk/release/app-release.apk
```

## 📞 **Support**

If you encounter issues:
1. Check Google Play Console for error messages
2. Review crash reports
3. Test on multiple Android TV devices
4. Check network connectivity and WebSocket server status

---

**Good luck with your deployment! 🚀**

# Create Mock Screenshots for Google Play Console

## Why This is Needed
Google Play Console requires screenshots for all device categories, even for TV-only apps.

## Quick Solution

### 1. Take TV Screenshots First
- Run your app on Google TV
- Take screenshots of:
  - QR Setup screen
  - Video playing screen
  - Any other key screens

### 2. Create Mock Screenshots

**For Phone (7-inch):**
- Resize TV screenshots to 1080x1920 (portrait)
- Add text overlay: "Designed for Android TV"
- Save as: `phone_screenshot_1.png`, `phone_screenshot_2.png`, etc.

**For Tablet (10-inch):**
- Resize TV screenshots to 1920x1200 (landscape)
- Add text overlay: "Designed for Android TV"
- Save as: `tablet_screenshot_1.png`, `tablet_screenshot_2.png`, etc.

### 3. Upload to Play Console
- Upload the mock screenshots to their respective categories
- Add description note: "This app is designed for Android TV devices"

## Alternative: Use Online Tools
- **Canva**: Resize and add text overlays
- **GIMP/Photoshop**: Resize and add watermarks
- **Online image resizers**: Quick resize tools

## Important Notes
- This is a common workaround for TV apps
- Google understands this limitation
- The app will still work perfectly on TV devices
- You're not misleading users - it's clearly marked as TV-only

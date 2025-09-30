# Privacy Policy for Curizm TV

**Last updated: September 30, 2025**

## Overview
Curizm TV is a video streaming application for Android TV that displays video content with background music and subtitles, controlled via WebSocket commands.

## Data Collection
**We do not collect any personal data.**

## Permissions Used
- **INTERNET**: Required to stream video content and connect to WebSocket servers
- **ACCESS_NETWORK_STATE**: Required to check network connectivity
- **CAMERA**: Declared by ZXing QR code library but **NOT USED** - we only generate QR codes, we do not access the camera

## Camera Permission
The app declares the CAMERA permission due to the ZXing QR code generation library, but **the app does not access, use, or require the camera**. The QR code generation is purely computational and does not involve camera functionality.

## Data Storage
- The app stores connection settings locally on your device using Android's SharedPreferences
- No data is transmitted to external servers except for video streaming and WebSocket communication

## Third-Party Services
- **Video Streaming**: Uses HLS (HTTP Live Streaming) for video playback
- **WebSocket Communication**: Connects to your specified WebSocket server for remote control
- **QR Code Generation**: Uses ZXing library for QR code display (no camera access)

## Contact
If you have any questions about this privacy policy, please contact us.

## Changes to This Policy
We may update this privacy policy from time to time. Any changes will be posted on this page.

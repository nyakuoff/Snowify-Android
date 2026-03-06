# Snowify Android

An Android port of Snowify — a music streaming app powered by YouTube Music.

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 35

### Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Open project `snowify-dcda0`
3. Add an Android app with package name `com.snowify.app`
4. Download `google-services.json` and place it in `app/`

### Font Setup
The app uses the **Inter** font. Download the Inter font family from https://rsms.me/inter/ and place the TTF files in `app/src/main/res/font/`:
- `inter_regular.ttf` (weight 400)
- `inter_medium.ttf` (weight 500)
- `inter_semibold.ttf` (weight 600)
- `inter_bold.ttf` (weight 700)
- `inter_extrabold.ttf` (weight 800)

### Build
```bash
./gradlew assembleDebug
```

## Architecture

```
app/
├── data/
│   ├── local/          # Room database, DataStore
│   ├── remote/         # API services (YTMusic, LrcLib, Firebase)
│   ├── model/          # Domain models
│   └── repository/     # Repository pattern
├── audio/              # ExoPlayer, MediaSession
├── di/                 # Hilt dependency injection modules
├── ui/
│   ├── theme/          # Colors, Typography, Shapes (7 themes)
│   ├── components/     # Reusable composables
│   ├── screens/        # All app screens
│   └── navigation/     # NavGraph, BottomNavBar
├── viewmodel/          # MVVM ViewModels
└── util/               # Extensions, constants
```

## Features

- 🎵 Stream music from YouTube Music via NewPipe Extractor
- 🎨 7 built-in themes (Dark, Light, Ocean, Forest, Sunset, Rose, Midnight)
- 🔍 Real-time search with debouncing and history
- 📚 Library with playlists and liked songs
- 🎤 Synced lyrics via LRCLIB
- 🔥 Firebase Auth + Firestore cloud sync
- 🔊 Volume capped at 50% (hearing protection)
- 🔁 Shuffle, repeat, autoplay
- 📱 MediaSession for lock screen controls
- ⚡ Gapless playback with ExoPlayer Media3

## Known Limitations

- **Emulator**: NewPipe Extractor stream URL extraction works best on physical devices
  - YouTube may block requests from emulator IPs
  - Test on a real device for actual playback
- **Firebase**: Requires `google-services.json` from the `snowify-dcda0` project
- **Fonts**: Must manually download Inter TTF files (see setup above)


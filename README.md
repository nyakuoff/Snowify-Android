<p align="center">
<img width="342" height="170" alt="snowify-logo-text" src="https://raw.githubusercontent.com/nyakuoff/Snowify/main/assets/snowify-logo-text.png" />
</p>

<p align="center">
The Android port of Snowify — a music player that streams audio from YouTube Music. Clean UI, no ads.
</p>

<p align="center">
<a href="https://discord.gg/JHDZraE5TD"><img src="https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord" /></a>
<a href="https://github.com/nyakuoff/Snowify"><img src="https://img.shields.io/badge/Desktop-Get%20the%20App-6C63FF?style=for-the-badge&logo=windows&logoColor=white" alt="Desktop" /></a>
</p>

> [!WARNING]
> The app is still in Developement and may be missing features, use at your own discretion.

## Features

- **Search** — Find songs, artists, and albums via YouTube Music
- **Playback** — Stream audio directly with play/pause, seek, skip, volume
- **Queue** — View and manage upcoming tracks
- **Smart Queue** — Auto-fills with similar songs when the queue runs out
- **Shuffle & Repeat** — Shuffle queue, repeat one or all
- **Playlists** — Create, rename, delete, and add/remove tracks
- **Liked Songs** — Heart any track to save it
- **Synced Lyrics** — Spotify-like synced lyrics via LRCLIB
- **Artist Pages** — Top songs, discography, about, fans also like
- **Cloud Sync** — Sign in with email to sync your library across devices (shared with desktop)
- **Themes** — 7 built-in color themes (Dark, Light, Ocean, Forest, Sunset, Rose, Midnight)
- **Lock Screen Controls** — Full MediaSession integration with notification controls

## Download

Grab the latest APK from the [Releases](https://github.com/nyakuoff/Snowify-Android/releases) page.

## Building from Source

### Requirements

- Android Studio (Hedgehog+) or just JDK 17
- Android SDK 35

### Setup

```bash
# Clone the repo
git clone https://github.com/nyakuoff/Snowify-Android.git
cd Snowify-Android

# Place your google-services.json in app/
# (from Firebase project snowify-dcda0)

# Build debug APK
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Firebase

The app uses Firebase for authentication and cloud sync (shared with the [desktop app](https://github.com/nyakuoff/Snowify)). You need `google-services.json` from the `snowify-dcda0` Firebase project placed in the `app/` directory.

## Tech Stack

- **Kotlin** — Language
- **Jetpack Compose** — UI framework
- **Media3 ExoPlayer** — Audio playback + MediaSession
- **Hilt** — Dependency injection
- **Room** — Local database for playlists, liked songs, history
- **DataStore** — Preferences storage
- **Firebase** — Auth & Firestore cloud sync
- **Retrofit + OkHttp** — Networking
- **Coil** — Image loading
- **LRCLIB** — Synced lyrics

## Roadmap

- [x] Search (songs, artists, albums)
- [x] Audio playback via NewPipe Extractor
- [x] Queue management
- [x] Playlists
- [x] Liked songs
- [x] Synced lyrics
- [x] Artist pages
- [x] Cloud sync (shared with desktop)
- [x] 7 built-in themes
- [x] Lock screen / notification controls
- [x] Explore page (trending, new releases)
- [ ] Follow artists
- [ ] Crossfade
- [ ] Friends & listen along
- [ ] Spotify playlist import
- [ ] Music videos

## Legal

This app is for **personal and educational use only**. It streams content from publicly available sources. I am not responsible for how anyone chooses to use it.

## Contributing

If you find bugs, have ideas, or want to clean something up, feel free to open an issue or a PR. All skill levels welcome.

> [!NOTE]
> **AI Disclaimer**: Parts of this project were assisted or written by AI. If that's something you're not comfortable with, no hard feelings, I understand and I don't force anyone to use it. The code may have flaws. If you spot something that could be better, contributions are very welcome.

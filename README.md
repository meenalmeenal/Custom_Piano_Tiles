# Custom Piano Tiles

Turn any YouTube song into a playable Piano Tiles game. A Python backend
extracts beat/note onsets from the audio; an Android app streams the song
and drops tiles synced to those onsets.

## Architecture
- **backend/** — FastAPI service. `yt-dlp` downloads a 60s segment, `librosa`
  detects onset times, and `/analyze` returns the onsets plus a direct audio
  stream URL.
- **android/** — Kotlin app (classic Views + ExoPlayer). `GameView` renders
  falling tiles timed to the onsets; tap tiles on the red judgment line to score.

## Prerequisites
- Python 3.10+
- **ffmpeg** on PATH (`sudo apt install ffmpeg` / `brew install ffmpeg`)
- Android Studio + a device or emulator (minSdk 26)
- Phone and computer on the **same Wi-Fi/LAN**

## Run the backend
```bash
cd backend
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
`--host 0.0.0.0` is required so your phone can reach it. Find your machine's
LAN IP (`ipconfig` on Windows, `ip addr` / `ifconfig` on macOS/Linux).

## Point the app at your backend
Edit `android/app/src/main/java/com/example/custompianotiles/ApiService.kt`:
```kotlin
private const val BASE_URL = "http://YOUR_LAN_IP:8000/"
```
Cleartext HTTP is already allowed in the manifest for local dev.

## Run the app
Open `android/` in Android Studio, build, and run. Paste a YouTube URL,
optionally a start time in seconds, and tap **Start**.

## How to play
Tiles fall from the top; tap each one as it reaches the red line. PERFECT/GOOD/OK
scores by accuracy. A missed tile ends the round.

## Known limitations
- Analyzes only a 60-second window.
- Stream URLs are short-lived YouTube links (start a fresh round if playback stalls).
- Onset detection is rhythm-based, not a real note transcription.

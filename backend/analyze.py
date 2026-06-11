import yt_dlp
import librosa
import numpy as np
import tempfile
import os

def analyze_song(yt_url: str):
    with tempfile.TemporaryDirectory() as tmpdir:
        ydl_opts = {
            'format': 'bestaudio',
            'outtmpl': f'{tmpdir}/audio.%(ext)s',
            'postprocessors': [{'key': 'FFmpegExtractAudio', 'preferredcodec': 'wav'}],
            'quiet': True
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([yt_url])

        audio_path = os.path.join(tmpdir, 'audio.wav')
        y, sr = librosa.load(audio_path)
        onset_frames = librosa.onset.onset_detect(y=y, sr=sr)
        onset_times = librosa.frames_to_time(onset_frames, sr=sr)

    stream_url = get_stream_url(yt_url)
    return {"onsets": onset_times.tolist(), "stream_url": stream_url}

def get_stream_url(yt_url: str):
    with yt_dlp.YoutubeDL({'format': 'bestaudio', 'quiet': True}) as ydl:
        info = ydl.extract_info(yt_url, download=False)
        return info['url']

if __name__ == '__main__':
    result = analyze_song("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    print("stream_url:", result['stream_url'][:60])
    print("first 10 onsets:", result['onsets'][:10])
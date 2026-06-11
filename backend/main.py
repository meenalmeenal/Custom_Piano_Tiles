from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from analyze import analyze_song

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/analyze")
def analyze(url: str):
    result = analyze_song(url)
    return result
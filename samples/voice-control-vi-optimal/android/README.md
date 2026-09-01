# Voice control (Vietnamese) - sample (optimal offline + lightweight)

This sample shows an offline-first, on-device voice control architecture optimized for high accuracy and low latency.

Highlights
- STT: Vosk on-device (offline). Use a small/high-quality Vietnamese model.
- NLU: Hybrid approach — fast keyword+fuzzy matching in Kotlin for immediate actions; optional on-device NLU TFLite for better semantic matching.
- Optional: self-hosted Whisper fallback for ultra-high accuracy (only when device uploads audio or transcripts).

Files in this directory:
- download_model.sh — helper to download the Vosk model (not committed in repo).
- app/ — minimal Android sample app showing service + UI.
- tools/nlu-training/ — scripts and notes to train a semantic NLU model and convert to TFLite.

See docs/voice-control-vi.md for architecture and next steps.

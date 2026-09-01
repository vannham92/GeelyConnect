# Voice control architecture (Vietnamese) — design notes

Goal: Accuracy close to cloud-grade but lightweight and offline-first.

Chosen stack
- Primary STT: Vosk (on-device) using a high-quality Vietnamese model.
  - Reason: offline, low-latency, deterministic, free.
- Optional high-accuracy fallback: self-hosted Whisper (server) or Whisper.cpp (on-device where hardware permits). Use only when higher accuracy required.
- NLU: hybrid
  - Quick path: keyword + fuzzy + lightweight embedding similarity in Kotlin for immediate actions.
  - Improved path: on-device TFLite NLU model (small classifier + embedding model) trained locally and updated as user provides feedback.

Data collection & continual learning
- App will collect (with user consent) anonymized pairs: (transcript, chosen_action) to a local store.
- Provide scripts/tools to export annotations and retrain NLU offline (no cloud required).

Privacy & performance
- Offline-first ensures user audio never leaves device unless explicitly opted-in for fallback.
- Model sizes:
  - Vosk small Vietnamese ~ tens to hundreds of MB (download separately).
  - TFLite NLU can be small (~1-10 MB) if using distil/tiny models + quantization.

Next steps (what I'll push)
- Minimal Android sample (already added in this branch).
- Download script for model and instructions.
- NLU training templates and README.

How to test quickly
1. Run download_model.sh on your development machine, transfer the model folder to device storage (/sdcard/vosk-model-small-vn) or app files.
2. Build and install sample app on an Android device.
3. Start the app, grant microphone permission, press Start.
4. Speak Vietnamese commands ("mở đèn", "tắt đèn", "tăng âm lượng"), check logs and UI for recognized transcript and mapped actions.

If you want, I can now:
- Add a simple small TFLite NLU demo model and the Kotlin TFLite inference glue (takes longer because of model conversion step).
- Add Whisper fallback scripts and a docker-compose self-hosted pipeline for higher accuracy.

Choose next: "add-tflite-nlu" or "add-whisper-fallback" or "done".

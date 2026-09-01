# NLU training notes and scripts

This folder contains helper scripts and notes for training a semantic NLU model (sentence embeddings + intent classifier) and exporting to TFLite for on-device inference.

Steps overview
1. Prepare a dataset: CSV with columns [text,intent]. Collect transcripts and label a small seed dataset (~500-2000 examples covering your intents).
2. Train a sentence-transformer or small transformer classifier on CPU/GPU using Hugging Face. Use a light backbone (distilbert or tiny-BERT) to keep model small.
3. Export a small embedding + classifier to ONNX and convert to TFLite (post-training quantization recommended).
4. Ship the .tflite file into the Android app and use TensorFlow Lite inference to compute embeddings + classify.

Scripts
- prepare_data.py — convert CSV to HF dataset format and split.
- train_nlu.py — training script using Transformers & Trainer.
- export_to_tflite.py — helper to convert a saved HF model to TFLite.

Note: full scripts require installing Python packages (transformers, datasets, torch, tensorflow, onnx, onnxruntime). This repo only includes templates; actual training is done outside the app.

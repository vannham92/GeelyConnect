#!/usr/bin/env bash
set -e
# Download Vosk Vietnamese model (not included in repo).
# Visit https://alphacephei.com/vosk/models to choose a model. Example: small/vosk-model-small-vn-0.4
# Replace MODEL_URL below with the direct download link.
MODEL_URL="https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip"
OUT_DIR="models"
mkdir -p "$OUT_DIR"
zipfile="$OUT_DIR/model.zip"

if [ -f "$zipfile" ]; then
  echo "Model archive already exists: $zipfile"
else
  echo "Downloading model from $MODEL_URL"
  curl -L -o "$zipfile" "$MODEL_URL"
fi

echo "Unzipping..."
unzip -o "$zipfile" -d "$OUT_DIR"

echo "Model downloaded and unzipped to $OUT_DIR. Move model folder into device storage or app files directory as instructed in README." 

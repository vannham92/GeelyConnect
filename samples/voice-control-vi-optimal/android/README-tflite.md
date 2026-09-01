# Updated README: include TFLite NLU steps

This README supplements the Android sample with instructions to create and integrate an on-device TFLite NLU model.

1) Create a small labeled dataset
- Prepare a CSV file `data.csv` with lines: "text,intent" (UTF-8, Vietnamese). Include at least ~200-500 examples across intents for a decent small model.

2) Train and export TFLite
- On your dev machine, create a Python environment and install dependencies:
  - pip install tensorflow
- Run the training script (examples):
  - python tools/nlu-training/train_small_nlu.py --data data.csv --model_out nlu_model.tflite --vocab_out vocab.json --labels_out labels.json
- The script produces three files: nlu_model.tflite, vocab.json, labels.json

3) Add files to Android sample
- Copy `nlu_model.tflite`, `vocab.json`, and `labels.json` into `samples/voice-control-vi-optimal/android/app/src/main/assets/`
- Make sure your app Gradle adds TensorFlow Lite dependency, e.g. in module build.gradle:
  implementation 'org.tensorflow:tensorflow-lite:2.11.0'

4) Use TfliteNlu.kt
- The sample includes `TfliteNlu.kt` which loads the model and assets and exposes `classify(text): Pair<label,score>`.
- Integrate TfliteNlu.classify() in VoiceRecognitionService.handleTranscript() to improve semantic matching.

Notes
- For smaller size, consider integer quantization (requires a representative dataset function in converter).
- For higher accuracy, increase vocab_size, maxlen, and model capacity but keep trade-offs for size/latency.

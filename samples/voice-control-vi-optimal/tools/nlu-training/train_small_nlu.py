"""
Train a small NLU classifier and export to TFLite.

Requirements:
- Python 3.8+
- pip install tensorflow datasets

This script trains a tiny text classifier using Keras Tokenizer -> Embedding -> GlobalAveragePooling -> Dense, saves the tokenizer word index (vocab.json) and labels.json, and exports a quantized TFLite model.

Usage:
  python train_small_nlu.py --data data.csv --model_out nlu_model.tflite --vocab_out vocab.json --labels_out labels.json

CSV format: text,intent

"""
import argparse
import json
import os

import numpy as np
import tensorflow as tf
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from tensorflow.keras import layers, models


def load_csv(path):
    texts = []
    labels = []
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line: continue
            parts = line.split(',', 1)
            if len(parts) != 2: continue
            texts.append(parts[0].strip())
            labels.append(parts[1].strip())
    return texts, labels


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--data', required=True)
    parser.add_argument('--model_out', default='nlu_model.tflite')
    parser.add_argument('--vocab_out', default='vocab.json')
    parser.add_argument('--labels_out', default='labels.json')
    parser.add_argument('--maxlen', type=int, default=16)
    parser.add_argument('--vocab_size', type=int, default=5000)
    args = parser.parse_args()

    texts, labels = load_csv(args.data)
    # encode labels
    unique_labels = sorted(list(set(labels)))
    label_to_idx = {l: i for i, l in enumerate(unique_labels)}
    y = np.array([label_to_idx[l] for l in labels])

    tokenizer = Tokenizer(num_words=args.vocab_size, oov_token='<OOV>')
    tokenizer.fit_on_texts(texts)
    seqs = tokenizer.texts_to_sequences(texts)
    X = pad_sequences(seqs, maxlen=args.maxlen, padding='post')

    model = models.Sequential([
        layers.Input(shape=(args.maxlen,), dtype='int32'),
        layers.Embedding(args.vocab_size, 32, input_length=args.maxlen),
        layers.GlobalAveragePooling1D(),
        layers.Dense(64, activation='relu'),
        layers.Dense(len(unique_labels), activation='softmax')
    ])
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    model.summary()

    model.fit(X, y, epochs=10, batch_size=32, validation_split=0.1)

    # Save vocab (word->index)
    word_index = tokenizer.word_index
    # Keep only top vocab_size words
    trimmed = {w: int(i) for w, i in word_index.items() if i < args.vocab_size}
    with open(args.vocab_out, 'w', encoding='utf-8') as f:
        json.dump(trimmed, f, ensure_ascii=False)

    # Save labels as index map
    labels_map = {str(i): unique_labels[i] for i in range(len(unique_labels))}
    with open(args.labels_out, 'w', encoding='utf-8') as f:
        json.dump(labels_map, f, ensure_ascii=False)

    # Convert to tflite with post-training quantization (float16 for small size)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()
    with open(args.model_out, 'wb') as f:
        f.write(tflite_model)

    print('Saved tflite model to', args.model_out)
    print('Saved vocab to', args.vocab_out)
    print('Saved labels to', args.labels_out)


if __name__ == '__main__':
    main()

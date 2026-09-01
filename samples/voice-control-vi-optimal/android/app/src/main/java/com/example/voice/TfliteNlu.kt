package com.example.voice

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

/**
 * Simple TFLite NLU helper.
 * Expects a TFLite model that takes input shape [1, MAX_LEN] of int32 token ids and outputs logits for N classes.
 * Also expects an assets/vocab.json (word->index) and assets/labels.json (index->label).
 *
 * Usage:
 *   val nlu = TfliteNlu(context)
 *   val intent = nlu.classify("mở cửa nhà")
 */
class TfliteNlu(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var vocab: Map<String, Int> = mapOf()
    private var labels: List<String> = listOf()
    private val MAX_LEN = 16 // must match training

    init {
        try {
            interpreter = Interpreter(loadModelFile("nlu_model.tflite"))
            vocab = loadVocab("vocab.json")
            labels = loadLabels("labels.json")
        } catch (e: Exception) {
            Log.e("TfliteNlu", "Init error: ${'$'}e")
        }
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadVocab(filename: String): Map<String, Int> {
        val json = context.assets.open(filename).bufferedReader().use { it.readText() }
        val jo = JSONObject(json)
        val map = mutableMapOf<String, Int>()
        val keys = jo.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = jo.getInt(k)
        }
        return map
    }

    private fun loadLabels(filename: String): List<String> {
        val json = context.assets.open(filename).bufferedReader().use { it.readText() }
        val jo = JSONObject(json)
        val arr = mutableListOf<String>()
        var i = 0
        while (jo.has(i.toString())) {
            arr.add(jo.getString(i.toString()))
            i++
        }
        return arr
    }

    private fun textToSequence(text: String): IntArray {
        val tokens = text.lowercase().split(Regex("\\s+"))
        val seq = IntArray(MAX_LEN) { 0 }
        var i = 0
        for (t in tokens) {
            if (i >= MAX_LEN) break
            val idx = vocab[t] ?: vocab["<OOV>"] ?: 1
            seq[i] = idx
            i++
        }
        return seq
    }

    fun classify(text: String): Pair<String, Float> {
        if (interpreter == null) return Pair("", 0f)
        val input = Array(1) { textToSequence(text) }
        val output = Array(1) { FloatArray(labels.size) }
        interpreter?.run(input, output)
        val scores = output[0]
        var bestIdx = 0
        var best = scores[0]
        for (i in scores.indices) {
            if (scores[i] > best) {
                best = scores[i]
                bestIdx = i
            }
        }
        val label = if (labels.isNotEmpty() && bestIdx < labels.size) labels[bestIdx] else "UNKNOWN"
        return Pair(label, best)
    }

    fun close() {
        interpreter?.close()
    }
}

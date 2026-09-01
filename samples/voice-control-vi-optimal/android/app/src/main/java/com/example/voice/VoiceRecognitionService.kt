package com.example.voice

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.AudioDispatcher
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

/**
 * VoiceRecognitionService (Vosk-based)
 * - Loads Vosk model from app files or external storage
 * - Runs a continuous recognizer and broadcasts transcripts
 * - Performs simple NLU mapping (keyword + fuzzy) to actions
 *
 * Notes:
 * - This sample uses classes from the Vosk Android SDK. Add the Vosk dependency to Gradle.
 *   Example Gradle dependency:
 *     implementation 'org.vosk:vosk-android:0.3.36'
 * - Model should be downloaded separately (see download_model.sh) and placed in
 *   device storage or app files dir. Update MODEL_PATH accordingly.
 */
class VoiceRecognitionService : Service(), RecognitionListener {
    private var model: Model? = null
    private var recognizer: SpeechService? = null

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try loading model from external storage (user should place model there)
                val modelPath = Environment.getExternalStorageDirectory().absolutePath + "/vosk-model-small-vn"
                model = Model(modelPath)
                startRecognizer()
            } catch (e: Exception) {
                Log.e("VoiceRec", "Failed to load model: ${'$'}e")
                // Consider fallback to Android SpeechRecognizer or notify user
            }
        }
    }

    private fun startRecognizer() {
        model?.let { m ->
            // Create recognizer with a large max alternatives and grammar-free (free form)
            val rec = Recognizer(m, 16000.0f)
            recognizer = SpeechService(rec, 16000.0f)
            (recognizer as SpeechService).setListener(this)
            (recognizer as SpeechService).startListening()
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        // optional: broadcast partial
    }

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            // Vosk returns JSON hyp; it may be plain text depending on API usage.
            // We'll broadcast the recognized text and run NLU mapping.
            val text = extractTextFromHypothesis(it)
            broadcastTranscript(text)
            handleTranscript(text)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        onResult(hypothesis)
    }

    override fun onError(exception: Exception?) {
        Log.e("VoiceRec", "Recognizer error: ${'$'}exception")
        // restart or fallback
        restartRecognizerWithDelay()
    }

    override fun onTimeout() {
        restartRecognizerWithDelay()
    }

    private fun restartRecognizerWithDelay() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Thread.sleep(500)
                stopRecognizer()
                startRecognizer()
            } catch (ignored: Exception) { }
        }
    }

    private fun stopRecognizer() {
        recognizer?.stop()
        recognizer = null
    }

    private fun extractTextFromHypothesis(h: String): String {
        // If h is JSON like {"text": "..."} try to extract; otherwise return raw
        return try {
            val key = "text"
            if (h.contains(key)) {
                val idx = h.indexOf(key)
                val start = h.indexOf(':', idx) + 1
                val txt = h.substring(start).replace(Regex("[\"{} ]"), "").trim()
                txt
            } else h
        } catch (e: Exception) { h }
    }

    private fun broadcastTranscript(t: String) {
        val i = Intent("com.example.voice.TRANSCRIPT")
        i.putExtra("transcript", t)
        sendBroadcast(i)
    }

    private fun handleTranscript(text: String) {
        val t = text.lowercase()
        when {
            t.contains("mở đèn") || t.contains("bật đèn") -> performAction("TURN_ON_LIGHT")
            t.contains("tắt đèn") -> performAction("TURN_OFF_LIGHT")
            t.contains("tăng âm lượng") -> performAction("VOLUME_UP")
            t.contains("giảm âm lượng") -> performAction("VOLUME_DOWN")
            else -> {
                // Low-confidence semantic matching: use embeddings or fuzzy match
                val action = semanticMatchIntent(t)
                performAction(action)
            }
        }
    }

    private fun semanticMatchIntent(text: String): String {
        // Lightweight semantic matching placeholder:
        // - Compare text to a small local intent corpus using trigram similarity
        // - In production: use on-device embeddings (TFLite) for better accuracy
        val intents = mapOf(
            "NAVIGATE_HOME" to listOf("đi về nhà", "chỉ đường về nhà"),
            "CALL_HOME" to listOf("gọi về nhà", "gọi mẹ", "gọi bố")
        )
        var best: Pair<String, Int>? = null
        for ((k, examples) in intents) {
            for (ex in examples) {
                val score = simpleSimilarity(text, ex)
                if (best == null || score > best.second) best = k to score
            }
        }
        return if ((best?.second ?: 0) > 30) best!!.first else "UNKNOWN_COMMAND"
    }

    private fun simpleSimilarity(a: String, b: String): Int {
        // crude similarity: common token count * 10
        val ta = a.split(Regex("\\s+"))
        val tb = b.split(Regex("\\s+"))
        val common = ta.intersect(tb)
        return common.size * 10
    }

    private fun performAction(action: String) {
        Log.d("VoiceRec", "Perform action: ${'$'}action")
        // TODO: integrate with app logic (send intents, call APIs, control hardware)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecognizer()
        model?.close()
        super.onDestroy()
    }
}

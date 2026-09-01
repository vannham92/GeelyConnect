package com.example.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var toggleBtn: Button
    private lateinit var transcriptView: TextView

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceService() else transcriptView.text = "Microphone permission required"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.simple_list_item_2)
        toggleBtn = Button(this).apply { text = "Start/Stop Voice" }
        transcriptView = TextView(this)
        val layout = findViewById<android.widget.LinearLayout>(android.R.id.content)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(toggleBtn)
        layout.addView(transcriptView)

        toggleBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                // toggle service
                val intent = Intent(this, VoiceRecognitionService::class.java)
                startService(intent)
            }
        }

        // Listen for broadcasts from the service for transcript updates (simple approach)
        val br = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                val text = intent?.getStringExtra("transcript")
                if (text != null) transcriptView.text = text
            }
        }
        registerReceiver(br, android.content.IntentFilter("com.example.voice.TRANSCRIPT"))
    }
}

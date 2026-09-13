package com.example.screenmirror

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var serverInput: EditText

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serverUrl = serverInput.text.toString().trim()
            if (serverUrl.isEmpty()) {
                Toast.makeText(this, "Enter the server address first", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
                putExtra(ScreenCaptureService.EXTRA_SERVER_URL, serverUrl)
            }
            startForegroundService(intent)
            Toast.makeText(this, "Streaming started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverInput = findViewById(R.id.serverInput)
        // Default only as a hint - replace with your Termux device's LAN IP.
        serverInput.setText("wss://soviet-durham-representative-effect.trycloudflare.com")

        findViewById<Button>(R.id.connectButton).setOnClickListener {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mpm.createScreenCaptureIntent())
        }

        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}

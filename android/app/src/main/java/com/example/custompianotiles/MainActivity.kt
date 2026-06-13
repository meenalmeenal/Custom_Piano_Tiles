package com.example.custompianotiles

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var gameView: GameView
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        val urlInput = findViewById<EditText>(R.id.urlInput)
        val startTimeInput = findViewById<EditText>(R.id.startTimeInput)
        val startBtn = findViewById<Button>(R.id.startBtn)
        val scoreText = findViewById<TextView>(R.id.scoreText)

        player = ExoPlayer.Builder(this).build()

        gameView.onHit = { points ->
            score += points
            runOnUiThread { scoreText.text = "Score: $score" }
        }

        gameView.onMiss = {
            runOnUiThread {
                try {
                    Toast.makeText(this, "Game Over! Score: $score", Toast.LENGTH_LONG).show()
                    player.stop()
                    gameView.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        startBtn.setOnClickListener {
            val url = urlInput.text.toString()
            val startTime = startTimeInput.text.toString().toIntOrNull() ?: 0
            if (url.isNotEmpty()) {
                score = 0
                scoreText.text = "Score: 0"
                startBtn.isEnabled = false
                startBtn.text = "Loading..."
                lifecycleScope.launch {
                    try {
                        val data = RetrofitClient.api.analyze(url, startTime)
                        player.setMediaItem(MediaItem.fromUri(data.stream_url))
                        player.prepare()
                        player.seekTo((startTime * 1000).toLong())
                        player.play()
                        gameView.start(data.onsets)
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(60000)
                            runOnUiThread { player.stop() }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                    } finally {
                        runOnUiThread {
                            startBtn.isEnabled = true
                            startBtn.text = "Start"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
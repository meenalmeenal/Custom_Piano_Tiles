package com.example.custompianotiles


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    data class Tile(var x: Int, var y: Float, val col: Int, var hit: Boolean = false)

    private val blackPaint = Paint().apply { color = Color.BLACK }
    private val hitPaint = Paint().apply { color = Color.DKGRAY }
    private val missPaint = Paint().apply { color = Color.RED }

    private val tiles = mutableListOf<Tile>()
    private var tileWidth = 0
    private var tileHeight = 200
    private var scrollSpeed = 8f
    var onMiss: (() -> Unit)? = null
    var onHit: (() -> Unit)? = null
    private var running = false

    fun start(onsets: List<Double>) {
        tiles.clear()
        running = true
        onsets.forEachIndexed { i, time ->
            val col = (0..3).random()
            tiles.add(Tile(col, -tileHeight - (i * 300f), col))
        }
        invalidate()
    }

    fun stop() { running = false }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        tileWidth = w / 4
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!running) return

        tiles.forEach { tile ->
            tile.y += scrollSpeed
            val rect = RectF(
                (tile.col * tileWidth).toFloat(),
                tile.y,
                (tile.col * tileWidth + tileWidth).toFloat(),
                tile.y + tileHeight
            )
            val paint = when {
                tile.hit -> hitPaint
                else -> blackPaint
            }
            canvas.drawRect(rect, paint)

            if (tile.y > height && !tile.hit) {
                onMiss?.invoke()
                tiles.remove(tile)
                return
            }
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val col = (event.x / tileWidth).toInt()
            val tapped = tiles.firstOrNull {
                !it.hit && it.col == col && it.y < event.y && it.y + tileHeight > event.y
            }
            if (tapped != null) {
                tapped.hit = true
                onHit?.invoke()
            }
        }
        return true
    }
}
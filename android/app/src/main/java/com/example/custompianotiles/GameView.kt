package com.example.custompianotiles

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    enum class TileType { TAP, HOLD }

    data class Tile(
        val col: Int,
        var y: Float,
        val type: TileType,
        var holdLength: Float = 0f,
        var hit: Boolean = false,
        var holding: Boolean = false,
        var missed: Boolean = false
    )

    private val blackPaint = Paint().apply { color = Color.parseColor("#1a1a1a") }
    private val holdPaint = Paint().apply { color = Color.parseColor("#3a3aff") }
    private val hitPaint = Paint().apply { color = Color.parseColor("#444444") }
    private val missPaint = Paint().apply { color = Color.parseColor("#ff3333") }
    private val linePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val dividerPaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 2f
    }

    private val tiles = mutableListOf<Tile>()
    private var tileWidth = 0
    private val tileHeight = 160
    private val scrollSpeed = 6f
    private var judgmentY = 0f
    var onMiss: (() -> Unit)? = null
    var onHit: (() -> Unit)? = null
    private var running = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        tileWidth = w / 4
        judgmentY = h * 0.82f
    }

    fun start(onsets: List<Double>) {
        tiles.clear()
        running = true
        invalidate()

        // group onsets into rows
        val rows = mutableListOf<MutableList<Double>>()
        var i = 0
        while (i < onsets.size) {
            val row = mutableListOf(onsets[i])
            // if next onset is very close (<0.15s), add to same row (multi tile)
            if (i + 1 < onsets.size && onsets[i + 1] - onsets[i] < 0.15) {
                row.add(onsets[i + 1])
                i++
            }
            rows.add(row)
            i++
        }

        rows.forEachIndexed { rowIdx, rowOnsets ->
            val delayMs = (rowOnsets[0] * 1000).toLong()
            val usedCols = mutableSetOf<Int>()

            rowOnsets.forEach { _ ->
                var col: Int
                do { col = (0..3).random() } while (usedCols.contains(col))
                usedCols.add(col)

                // determine hold vs tap based on gap to next row
                val nextRowTime = if (rowIdx + 1 < rows.size) rows[rowIdx + 1][0] else rowOnsets[0] + 0.5
                val gap = nextRowTime - rowOnsets[0]
                val isHold = gap > 0.4

                postDelayed({
                    if (running) {
                        tiles.add(Tile(
                            col = col,
                            y = -tileHeight.toFloat(),
                            type = if (isHold) TileType.HOLD else TileType.TAP,
                            holdLength = if (isHold) (gap * scrollSpeed * 16).toFloat().coerceAtMost(300f) else 0f
                        ))
                    }
                }, delayMs)
            }
        }
    }

    fun stop() {
        running = false
        tiles.clear()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // background
        canvas.drawColor(Color.WHITE)

        // column dividers
        for (c in 1..3) {
            canvas.drawLine(c * tileWidth.toFloat(), 0f, c * tileWidth.toFloat(), height.toFloat(), dividerPaint)
        }

        // judgment line
        canvas.drawLine(0f, judgmentY, width.toFloat(), judgmentY, linePaint)

        if (!running) return

        val toRemove = mutableListOf<Tile>()

        tiles.forEach { tile ->
            tile.y += scrollSpeed

            val left = tile.col * tileWidth.toFloat() + 4
            val right = left + tileWidth - 8
            val top = tile.y
            val bottom = tile.y + tileHeight + tile.holdLength

            val paint = when {
                tile.missed -> missPaint
                tile.hit && tile.type == TileType.TAP -> hitPaint
                tile.type == TileType.HOLD -> holdPaint
                else -> blackPaint
            }

            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, paint)

            // miss detection — tile passed judgment line without being tapped
            if (tile.y > judgmentY + tileHeight && !tile.hit && !tile.holding) {
                tile.missed = true
                onMiss?.invoke()
                toRemove.add(tile)
                return@forEach
            }

            // remove hit tap tiles after they pass
            if (tile.hit && tile.type == TileType.TAP && tile.y > judgmentY + tileHeight) {
                toRemove.add(tile)
            }

            // remove completed hold tiles
            if (tile.holding && bottom > judgmentY + tileHeight + 50) {
                onHit?.invoke()
                toRemove.add(tile)
            }
        }

        tiles.removeAll(toRemove)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return true
        val col = (event.x / tileWidth).toInt().coerceIn(0, 3)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val tile = tiles.firstOrNull {
                    !it.hit && !it.holding && !it.missed &&
                            it.col == col &&
                            it.y < judgmentY + 80 &&
                            it.y + it.tileHeight + it.holdLength > judgmentY - 80
                }
                if (tile != null) {
                    if (tile.type == TileType.TAP) {
                        tile.hit = true
                        onHit?.invoke()
                    } else {
                        tile.holding = true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                tiles.filter { it.holding && it.col == col }.forEach { it.holding = false }
            }
        }
        return true
    }

    private val Tile.tileHeight get() = 160
}
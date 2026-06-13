package com.example.custompianotiles

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    enum class TileType { TAP, HOLD }
    enum class Judgment { PERFECT, GOOD, OK, NONE }

    data class Tile(
        val col: Int,
        var y: Float,
        val type: TileType,
        var holdLength: Float = 0f,
        var hit: Boolean = false,
        var holding: Boolean = false,
        var missed: Boolean = false
    )

    data class JudgmentText(val text: String, val color: Int, var alpha: Float = 255f, var y: Float)

    private val blackPaint = Paint().apply { color = Color.parseColor("#1a1a1a") }
    private val holdPaint = Paint().apply { color = Color.parseColor("#8844ff") }
    private val hitPaint = Paint().apply { color = Color.parseColor("#555555") }
    private val linePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val dividerPaint = Paint().apply {
        color = Color.parseColor("#cccccc")
        strokeWidth = 2f
    }
    private val judgmentPaint = Paint().apply {
        textSize = 72f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val tiles = java.util.concurrent.CopyOnWriteArrayList<Tile>()
    private val judgmentTexts = java.util.concurrent.CopyOnWriteArrayList<JudgmentText>()

    private var tileWidth = 0
    private val tileHeight = 180
    private var scrollSpeed = 3f
    private var judgmentY = 0f
    var onMiss: (() -> Unit)? = null
    var onHit: ((Int) -> Unit)? = null  // passes score delta
    private var running = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        tileWidth = w / 4
        judgmentY = h * 0.80f
    }

    fun start(onsets: List<Double>) {
        tiles.clear()
        judgmentTexts.clear()
        running = true
        invalidate()

        // filter onsets — enforce minimum gap of 0.25s between rows
        val filtered = mutableListOf<Double>()
        var lastTime = -1.0
        for (onset in onsets) {
            if (onset - lastTime >= 0.25) {
                filtered.add(onset)
                lastTime = onset
            }
        }

        // group into rows — if two onsets within 0.1s, same row (max 2)
        val rows = mutableListOf<MutableList<Double>>()
        var i = 0
        while (i < filtered.size) {
            val row = mutableListOf(filtered[i])
            if (i + 1 < filtered.size && filtered[i + 1] - filtered[i] < 0.1) {
                row.add(filtered[i + 1])
                i++
            }
            rows.add(row)
            i++
        }

        // calculate BPM-based scroll speed
        if (rows.size > 4) {
            val avgGap = (rows[4][0] - rows[0][0]) / 4.0
            val bpm = 60.0 / avgGap
            scrollSpeed = (bpm / 120f * 5f).toFloat().coerceIn(3f, 8f)
        }

        rows.forEachIndexed { rowIdx, rowOnsets ->
            val delayMs = (rowOnsets[0] * 1000).toLong()
            val usedCols = mutableSetOf<Int>()

            val nextRowTime = if (rowIdx + 1 < rows.size) rows[rowIdx + 1][0] else rowOnsets[0] + 0.5
            val gap = nextRowTime - rowOnsets[0]
            val isHold = gap > 0.5 && rowOnsets.size == 1  // only single tiles can be hold

            rowOnsets.forEach { _ ->
                var col: Int
                do { col = (0..3).random() } while (usedCols.contains(col))
                usedCols.add(col)

                postDelayed({
                    if (running) {
                        tiles.add(Tile(
                            col = col,
                            y = -tileHeight.toFloat(),
                            type = if (isHold) TileType.HOLD else TileType.TAP,
                            holdLength = if (isHold) (gap * scrollSpeed * 20f).toFloat().coerceIn(150f, 600f) else 0f
                        ))
                    }
                }, delayMs)
            }
        }
    }

    fun stop() {
        running = false
        tiles.clear()
        judgmentTexts.clear()
    }

    private fun getJudgment(tileY: Float): Pair<Judgment, Int> {
        val tileCenterY = tileY + tileHeight / 2
        val dist = Math.abs(tileCenterY - judgmentY)
        return when {
            dist < 60 -> Pair(Judgment.PERFECT, 3)
            dist < 130 -> Pair(Judgment.GOOD, 2)
            else -> Pair(Judgment.OK, 1)
        }
    }

    private fun showJudgment(judgment: Judgment, x: Float) {
        val (text, color) = when (judgment) {
            Judgment.PERFECT -> Pair("PERFECT", Color.parseColor("#FFD700"))
            Judgment.GOOD -> Pair("GOOD", Color.parseColor("#00FF88"))
            Judgment.OK -> Pair("OK", Color.parseColor("#FFFFFF"))
            Judgment.NONE -> return
        }
        judgmentTexts.add(JudgmentText(text, color, 255f, judgmentY - 100f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

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

            val left = tile.col * tileWidth.toFloat() + 6
            val right = left + tileWidth - 12
            val top = tile.y
            val bottom = tile.y + tileHeight + tile.holdLength

            val paint = when {
                tile.hit && tile.type == TileType.TAP -> hitPaint
                tile.type == TileType.HOLD -> holdPaint
                else -> blackPaint
            }

            canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, paint)

            // miss — passed judgment line untapped
            if (tile.y > judgmentY + tileHeight && !tile.hit && !tile.holding) {
                onMiss?.invoke()
                toRemove.add(tile)
                return@forEach
            }

            // remove tapped tiles after passing
            if (tile.hit && tile.y > judgmentY + tileHeight) toRemove.add(tile)

            // remove completed hold tiles
            if (tile.holding && bottom > height) {
                onHit?.invoke(1)
                toRemove.add(tile)
            }
        }

        tiles.removeAll(toRemove)

        // draw + fade judgment texts
        val textsToRemove = mutableListOf<JudgmentText>()
        judgmentTexts.forEach { jt ->
            judgmentPaint.color = Color.argb(jt.alpha.toInt(), Color.red(jt.color), Color.green(jt.color), Color.blue(jt.color))
            canvas.drawText(jt.text, width / 2f, jt.y, judgmentPaint)
            jt.alpha -= 8f
            jt.y -= 2f
            if (jt.alpha <= 0) textsToRemove.add(jt)
        }
        judgmentTexts.removeAll(textsToRemove)

        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return true
        val col = (event.x / tileWidth).toInt().coerceIn(0, 3)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // tap anywhere on the tile — score based on distance from judgment line
                android.util.Log.d("TOUCH", "touchY=${event.y} col=$col tiles=${tiles.map { "${it.y} to ${it.y + it.tileHeight}" }}")
                val tile = tiles.firstOrNull {
                    !it.hit && !it.holding && it.col == col
                }
                if (tile != null) {
                    val (judgment, points) = getJudgment(tile.y)
                    showJudgment(judgment, event.x)
                    if (tile.type == TileType.TAP) {
                        tile.hit = true
                        onHit?.invoke(points)
                    } else {
                        tile.holding = true
                        onHit?.invoke(points)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                tiles.filter { it.holding && it.col == col }.forEach { it.holding = false }
            }
        }
        return true
    }

    private val Tile.tileHeight get() = 180
}
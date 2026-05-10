package com.example.recorder.presentation.record

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.recorder.R
import kotlin.math.max

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.waveform_bar)
        style = Paint.Style.FILL
    }
    private val rect = RectF()
    private var amplitudes = listOf<Int>()

    private val barWidthDp = 3f
    private val barGapDp = 2f
    private val minBarHeightDp = 4f
    private val cornerRadiusDp = 2f

    private val density = context.resources.displayMetrics.density
    private val barWidth = barWidthDp * density
    private val barGap = barGapDp * density
    private val minBarHeight = minBarHeightDp * density
    private val cornerRadius = cornerRadiusDp * density

    private val maxAmplitude = 32767f

    fun setAmplitudes(list: List<Int>) {
        amplitudes = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return

        val totalBarWidth = barWidth + barGap
        val visibleBars = (width / totalBarWidth).toInt().coerceAtLeast(1)
        val bars = amplitudes.takeLast(visibleBars)

        val centerY = height / 2f
        val maxHalfHeight = height / 2f - minBarHeight

        bars.forEachIndexed { index, amplitude ->
            val x = index * totalBarWidth
            val normalized = (amplitude / maxAmplitude).coerceIn(0f, 1f)
            val halfHeight = max(minBarHeight, normalized * maxHalfHeight)

            rect.set(x, centerY - halfHeight, x + barWidth, centerY + halfHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)
        }
    }
}

package com.example.riomarappnav.modelYolov8n

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.riomarappnav.R

/**
 * OverlayView é responsável por desenhar as caixas delimitadoras e seus rótulos.
 */
class OverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var boundingBoxes: List<BoundingBox> = emptyList()

    // Configuração dos Paints
    private val boxPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.bounding_box_color)
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        style = Paint.Style.FILL
    }
    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val textPadding = 8

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Itera sobre cada bounding box e desenha a caixa e o rótulo
        boundingBoxes.forEach { box ->
            val left = box.x1 * width
            val top = box.y1 * height
            val right = box.x2 * width
            val bottom = box.y2 * height

            // Desenha o retângulo da caixa
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Prepara e desenha o fundo e o texto do rótulo
            val label = box.clsName
            val textBounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)
            canvas.drawRect(
                left,
                top,
                left + textBounds.width() + textPadding,
                top + textBounds.height() + textPadding,
                backgroundPaint
            )
            canvas.drawText(label, left, top + textBounds.height().toFloat(), textPaint)
        }
    }

    /**
     * Atualiza as caixas delimitadoras e solicita o redesenho da view.
     */
    fun setResults(boxes: List<BoundingBox>) {
        boundingBoxes = boxes
        invalidate()
    }

    /**
     * Limpa as caixas desenhadas.
     */
    fun clear() {
        boundingBoxes = emptyList()
        invalidate()
    }
}

package com.fft.kitchen.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.fft.kitchen.data.LabelElement
import com.fft.kitchen.data.LabelLayout
import com.fft.kitchen.data.FieldType
import java.text.SimpleDateFormat
import java.util.*

class LabelPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var layout: LabelLayout? = null
    private var draggedElement: LabelElement? = null
    private var selectedElement: LabelElement? = null
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var onElementSelected: ((LabelElement) -> Unit)? = null
    private var customText: String = ""
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
    }

    fun setLayout(layout: LabelLayout) {
        this.layout = layout
        invalidate()
    }

    fun setOnElementSelectedListener(listener: (LabelElement) -> Unit) {
        onElementSelected = listener
    }

    fun updateCustomText(text: String) {
        customText = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw label background (blue like in label manager)
        canvas.drawColor(Color.rgb(200, 220, 255))

        // Draw grid lines for reference
        drawGrid(canvas)

        // Draw elements
        layout?.elements?.forEach { element ->
            textPaint.apply {
                textSize = element.fontSize
                typeface = if (element.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                color = if (element == selectedElement) Color.RED else Color.BLACK
            }

            // Get the display text based on field type
            val displayText = when (element.fieldType) {
                FieldType.PRODUCT_NAME -> if (customText.isNotEmpty()) customText else element.text
                FieldType.BATCH_NO -> "${element.text}${System.currentTimeMillis() % 10000}"
                FieldType.EMPLOYEE_NAME -> "${element.text}User"
                FieldType.PREP_DATE -> {
                    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    "${element.text}${dateFormat.format(Date())}"
                }
                FieldType.USE_BY_DATE -> {
                    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    val expiryDate = Date(System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000)) // 3 days from now
                    "${element.text}${dateFormat.format(expiryDate)}"
                }
                else -> element.text
            }

            canvas.drawText(displayText, element.x, element.y, textPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Draw vertical lines
        for (x in 0..width step 50) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }

        // Draw horizontal lines
        for (y in 0..height step 50) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Find touched element
                layout?.elements?.firstOrNull { element ->
                    val textBounds = android.graphics.Rect()
                    textPaint.getTextBounds(element.text, 0, element.text.length, textBounds)
                    x >= element.x && x <= element.x + textBounds.width() &&
                    y >= element.y - textBounds.height() && y <= element.y
                }?.let { element ->
                    draggedElement = element
                    selectedElement = element
                    lastTouchX = x
                    lastTouchY = y
                    onElementSelected?.invoke(element)
                    invalidate()
                } ?: run {
                    selectedElement = null
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                draggedElement?.let { element ->
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    element.x += dx
                    element.y += dy
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                draggedElement = null
            }
        }
        return true
    }
} 
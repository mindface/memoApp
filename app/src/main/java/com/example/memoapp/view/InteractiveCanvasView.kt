package com.example.memoapp.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.memoapp.model.CanvasElement

class InteractiveCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val elements = mutableListOf<CanvasElement>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        color = Color.BLACK
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4285F4")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4285F4")
        style = Paint.Style.FILL
    }

    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f

    private var selectedElement: CanvasElement? = null
    private var isDragging = false
    private var isResizing = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val handleSize = 30f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (selectedElement != null) return false // Don't zoom if editing element
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.1f, 5.0f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (isDragging || isResizing) return false
            offsetX -= distanceX
            offsetY -= distanceY
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val canvasX = (e.x - width / 2f) / scaleFactor + width / 2f - offsetX / scaleFactor
            val canvasY = (e.y - height / 2f) / scaleFactor + height / 2f - offsetY / scaleFactor
            
            // Map screen touch to canvas coordinates
            val pts = floatArrayOf(e.x, e.y)
            val inverseMatrix = Matrix()
            canvasMatrix.invert(inverseMatrix)
            inverseMatrix.mapPoints(pts)
            val cx = pts[0]
            val cy = pts[1]

            // Check for selection (top-most element first)
            val hit = elements.findLast { it.contains(cx, cy) }
            if (hit != null) {
                selectedElement = hit
                bringToFront(hit)
                onSelectionChangedListener?.invoke(hit)
            } else {
                selectedElement = null
                onSelectionChangedListener?.invoke(null)
                onCanvasClickListener?.invoke(cx, cy)
            }
            invalidate()
            return true
        }
    })

    private fun CanvasElement.contains(cx: Float, cy: Float): Boolean {
        return cx >= x && cx <= x + width && cy >= y && cy <= y + height
    }

    private fun isOnHandle(element: CanvasElement, cx: Float, cy: Float): Boolean {
        val hx = element.x + element.width
        val hy = element.y + element.height
        return cx >= hx - handleSize && cx <= hx + handleSize && cy >= hy - handleSize && cy <= hy + handleSize
    }

    private val canvasMatrix = Matrix()
    var onCanvasClickListener: ((x: Float, y: Float) -> Unit)? = null
    var onSelectionChangedListener: ((CanvasElement?) -> Unit)? = null

    fun setElements(newElements: List<CanvasElement>) {
        elements.clear()
        elements.addAll(newElements.sortedBy { it.zIndex })
        selectedElement = null
        invalidate()
    }

    fun getSelectedElement(): CanvasElement? = selectedElement

    fun setSelectedElementColor(color: Int) {
        selectedElement?.let {
            it.color = color
            invalidate()
        }
    }

    private fun bringToFront(element: CanvasElement) {
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
        if (element.zIndex <= maxZ) {
            element.zIndex = maxZ + 1
        }
        elements.remove(element)
        elements.add(element)
    }

    fun sendSelectedToBack() {
        selectedElement?.let { element ->
            val minZ = elements.minOfOrNull { it.zIndex } ?: 0
            element.zIndex = minZ - 1
            elements.remove(element)
            elements.add(0, element)
            invalidate()
        }
    }

    fun deleteSelectedElement() {
        selectedElement?.let {
            elements.remove(it)
            selectedElement = null
            onSelectionChangedListener?.invoke(null)
            invalidate()
        }
    }

    fun getElements(): List<CanvasElement> = elements

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvasMatrix.reset()
        canvasMatrix.postTranslate(offsetX, offsetY)
        canvasMatrix.postScale(scaleFactor, scaleFactor, width / 2f, height / 2f)

        canvas.save()
        canvas.concat(canvasMatrix)

        // Draw in order of elements list (which should be sorted by zIndex)
        for (element in elements) {
            paint.color = element.color
            paint.style = Paint.Style.FILL
            when (element.type) {
                "RECTANGLE" -> {
                    canvas.drawRect(element.x, element.y, element.x + element.width, element.y + element.height, paint)
                }
                "CIRCLE" -> {
                    canvas.drawCircle(element.x + element.width / 2, element.y + element.height / 2, element.width / 2, paint)
                }
                "TEXT" -> {
                    textPaint.textSize = element.fontSize
                    canvas.drawText(element.text, element.x, element.y + element.fontSize, textPaint) // Offset for baseline
                }
            }

            // Selection UI
            if (element == selectedElement) {
                canvas.drawRect(element.x, element.y, element.x + element.width, element.y + element.height, selectionPaint)
                // Resize handle at bottom-right
                canvas.drawRect(element.x + element.width - 20f, element.y + element.height - 20f, 
                               element.x + element.width + 20f, element.y + element.height + 20f, handlePaint)
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val pts = floatArrayOf(event.x, event.y)
        val inverseMatrix = Matrix()
        canvasMatrix.invert(inverseMatrix)
        inverseMatrix.mapPoints(pts)
        val cx = pts[0]
        val cy = pts[1]

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedElement?.let {
                    if (isOnHandle(it, cx, cy)) {
                        isResizing = true
                    } else if (it.contains(cx, cy)) {
                        isDragging = true
                    }
                }
                lastTouchX = cx
                lastTouchY = cy
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = cx - lastTouchX
                val dy = cy - lastTouchY
                
                if (isResizing) {
                    selectedElement?.let {
                        if (it.type == "TEXT") {
                            // テキストの場合は高さの変更をフォントサイズに反映し、アスペクト比を維持して枠を更新
                            val newSize = (it.fontSize + dy).coerceAtLeast(10f)
                            it.fontSize = newSize
                            
                            val bounds = Rect()
                            textPaint.textSize = it.fontSize
                            textPaint.getTextBounds(it.text, 0, it.text.length, bounds)
                            it.width = bounds.width().toFloat() + 20f
                            it.height = it.fontSize + 10f
                        } else {
                            it.width = (it.width + dx).coerceAtLeast(50f)
                            it.height = (it.height + dy).coerceAtLeast(50f)
                        }
                        invalidate()
                    }
                } else if (isDragging) {
                    selectedElement?.let {
                        it.x += dx
                        it.y += dy
                        invalidate()
                    }
                }
                lastTouchX = cx
                lastTouchY = cy
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isResizing = false
            }
        }
        return true
    }


    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

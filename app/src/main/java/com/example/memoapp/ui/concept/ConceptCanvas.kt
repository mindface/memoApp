package com.example.memoapp.ui.concept

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAny
import com.example.memoapp.model.CanvasElement

@Composable
fun ConceptCanvas(
    elements: List<CanvasElement>,
    selectedElement: CanvasElement?,
    onSelectElement: (CanvasElement?) -> Unit,
    onCanvasClick: (Float, Float) -> Unit,
    onElementUpdate: (CanvasElement) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // UI用のドラッグ/リサイズ一時状態
    var activeElementId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var sizeDelta by remember { mutableStateOf(Offset.Zero) }
    var isResizing by remember { mutableStateOf(false) }
    var isDraggingCanvas by remember { mutableStateOf(false) }

    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
        }
    }
    
    val selectionPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4285F4")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }
    }
    
    val handlePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4285F4")
            style = android.graphics.Paint.Style.FILL
        }
    }

    val elementPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(elements, selectedElement, scale, offset) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val downPos = down.position
                    
                    // キャンバス座標に変換
                    val cx = (downPos.x - size.width / 2f) / scale + size.width / 2f - offset.x / scale
                    val cy = (downPos.y - size.height / 2f) / scale + size.height / 2f - offset.y / scale
                    
                    // ハンドルの当たり判定（選択中かつハンドル上か）
                    val handleHit = selectedElement?.let { isOnHandle(it, cx, cy) } ?: false
                    val bodyHit = elements.findLast { it.contains(cx, cy) }
                    
                    var totalDrag = Offset.Zero
                    var isMultiTouch = false
                    
                    if (handleHit) {
                        activeElementId = selectedElement?.id
                        isResizing = true
                    } else if (bodyHit != null) {
                        activeElementId = bodyHit.id
                        onSelectElement(bodyHit)
                    } else {
                        activeElementId = null
                        isDraggingCanvas = true
                    }

                    do {
                        val event = awaitPointerEvent()
                        isMultiTouch = event.changes.size > 1
                        
                        if (isMultiTouch) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            scale = (scale * zoom).coerceIn(0.1f, 5f)
                            offset += pan
                            activeElementId = null
                            isResizing = false
                        } else {
                            val change = event.changes[0]
                            if (change.pressed) {
                                val delta = change.position - change.previousPosition
                                totalDrag += delta
                                
                                if (isResizing) {
                                    sizeDelta += Offset(delta.x / scale, delta.y / scale)
                                } else if (activeElementId != null) {
                                    dragDelta += Offset(delta.x / scale, delta.y / scale)
                                } else if (isDraggingCanvas) {
                                    offset += delta
                                }
                                change.consume()
                            }
                        }
                    } while (event.changes.fastAny { it.pressed })

                    // ジェスチャー終了時の確定
                    if (activeElementId != null) {
                        val element = elements.find { it.id == activeElementId }
                        if (element != null) {
                            if (isResizing && sizeDelta != Offset.Zero) {
                                val newWidth = (element.width + sizeDelta.x).coerceAtLeast(50f)
                                val newHeight = (element.height + sizeDelta.y).coerceAtLeast(50f)
                                val updated = if (element.type == "TEXT") {
                                    val newFontSize = (element.fontSize + sizeDelta.y).coerceAtLeast(10f)
                                    element.copy(width = newWidth, height = newHeight, fontSize = newFontSize)
                                } else {
                                    element.copy(width = newWidth, height = newHeight)
                                }
                                onElementUpdate(updated)
                            } else if (!isResizing && dragDelta != Offset.Zero) {
                                onElementUpdate(element.copy(x = element.x + dragDelta.x, y = element.y + dragDelta.y))
                            }
                        }
                    }
                    
                    // タップ判定
                    if (!isMultiTouch && !isResizing && totalDrag.getDistance() < 10f) {
                        if (bodyHit == null && !handleHit) {
                            onSelectElement(null)
                            onCanvasClick(cx, cy)
                        }
                    }

                    // クリーンアップ
                    activeElementId = null
                    dragDelta = Offset.Zero
                    sizeDelta = Offset.Zero
                    isResizing = false
                    isDraggingCanvas = false
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            
            nativeCanvas.save()
            nativeCanvas.translate(offset.x, offset.y)
            nativeCanvas.scale(scale, scale, size.width / 2f, size.height / 2f)

            for (element in elements) {
                val isDragging = element.id == activeElementId && !isResizing
                val isBeingResized = element.id == activeElementId && isResizing
                
                elementPaint.color = element.color
                elementPaint.alpha = if (isDragging || isBeingResized) 180 else 255
                
                val renderX = if (isDragging) element.x + dragDelta.x else element.x
                val renderY = if (isDragging) element.y + dragDelta.y else element.y
                val renderW = if (isBeingResized) (element.width + sizeDelta.x).coerceAtLeast(50f) else element.width
                val renderH = if (isBeingResized) (element.height + sizeDelta.y).coerceAtLeast(50f) else element.height

                when (element.type) {
                    "RECTANGLE" -> {
                        nativeCanvas.drawRect(renderX, renderY, renderX + renderW, renderY + renderH, elementPaint)
                    }
                    "CIRCLE" -> {
                        nativeCanvas.drawCircle(renderX + renderW / 2, renderY + renderH / 2, renderW / 2, elementPaint)
                    }
                    "TEXT" -> {
                        val renderSize = if (isBeingResized) (element.fontSize + sizeDelta.y).coerceAtLeast(10f) else element.fontSize
                        textPaint.textSize = renderSize
                        textPaint.color = element.color
                        textPaint.alpha = if (isDragging || isBeingResized) 180 else 255
                        nativeCanvas.drawText(element.text, renderX, renderY + renderSize, textPaint)
                    }
                }
                
                if (element == selectedElement) {
                    nativeCanvas.drawRect(renderX, renderY, renderX + renderW, renderY + renderH, selectionPaint)
                    nativeCanvas.drawRect(
                        renderX + renderW - 20f, 
                        renderY + renderH - 20f, 
                        renderX + renderW + 20f, 
                        renderY + renderH + 20f, 
                        handlePaint
                    )
                }
            }
            nativeCanvas.restore()
        }
    }
}

private fun CanvasElement.contains(cx: Float, cy: Float): Boolean {
    return cx >= x && cx <= x + width && cy >= y && cy <= y + height
}

private fun isOnHandle(element: CanvasElement, cx: Float, cy: Float): Boolean {
    val handleSize = 40f
    val hx = element.x + element.width
    val hy = element.y + element.height
    return cx >= hx - handleSize && cx <= hx + handleSize && cy >= hy - handleSize && cy <= hy + handleSize
}

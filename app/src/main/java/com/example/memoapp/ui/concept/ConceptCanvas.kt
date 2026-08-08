package com.example.memoapp.ui.concept

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import com.example.memoapp.model.CanvasElement

@Composable
fun ConceptCanvas(
    elements: List<CanvasElement>,
    selectedElement: CanvasElement?,
    gyroOffset: Offset,
    onSelectElement: (CanvasElement?) -> Unit,
    onCanvasClick: (Float, Float) -> Unit,
    onElementUpdate: (CanvasElement) -> Unit,
    modifier: Modifier = Modifier
) {
    // 状態の更新をジェスチャーループに伝えるための rememberUpdatedState
    // これにより pointerInput(Unit) 内で常に最新の値を参照できる
    val currentElements by rememberUpdatedState(elements)
    val currentSelectedElement by rememberUpdatedState(selectedElement)
    val currentOnSelectElement by rememberUpdatedState(onSelectElement)
    val currentOnCanvasClick by rememberUpdatedState(onCanvasClick)
    val currentOnElementUpdate by rememberUpdatedState(onElementUpdate)
    val currentGyroOffset by rememberUpdatedState(gyroOffset)

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // UI用のドラッグ/リサイズ一時状態
    var activeElementId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var sizeDelta by remember { mutableStateOf(Offset.Zero) }
    var isResizing by remember { mutableStateOf(false) }
    var isDraggingCanvas by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()
    val selectionColor = Color(0xFF4285F4)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // 鍵を Unit にすることで、座標更新によるセンサーのリセットを防ぐ
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val downPos = down.position
                    
                    // キャンバス座標に変換
                    val currentScaleAtStart = if (scale > 0.001f) scale else 1f
                    val totalOffsetAtStart = offset + currentGyroOffset
                    val cx = (downPos.x - totalOffsetAtStart.x) / currentScaleAtStart
                    val cy = (downPos.y - totalOffsetAtStart.y) / currentScaleAtStart
                    
                    // ハンドルの当たり判定
                    val handleHit = currentSelectedElement?.let { isOnHandle(it, cx, cy) } ?: false
                    val bodyHit = currentElements.findLast { it.contains(cx, cy) }
                    
                    var totalDrag = Offset.Zero
                    var everMultiTouch = false
                    
                    if (handleHit) {
                        activeElementId = currentSelectedElement?.id
                        isResizing = true
                    } else if (bodyHit != null) {
                        activeElementId = bodyHit.id
                        currentOnSelectElement(bodyHit)
                    } else {
                        activeElementId = null
                        isDraggingCanvas = true
                    }

                    do {
                        val event = awaitPointerEvent()
                        val isMultiTouch = event.changes.size > 1
                        if (isMultiTouch) everMultiTouch = true
                        
                        if (isMultiTouch) {
                            val zoomAmount = event.calculateZoom()
                            val panAmount = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)

                            if (zoomAmount != 1f || panAmount != Offset.Zero) {
                                val oldScale = scale
                                val newScale = (oldScale * zoomAmount).coerceIn(0.1f, 5f)
                                val scaleRatio = newScale / oldScale
                                
                                // 支点（指の中心）を維持したまま拡大縮小
                                offset = centroid - (centroid - offset) * scaleRatio + panAmount
                                scale = newScale
                            }
                            
                            activeElementId = null
                            isResizing = false
                        } else {
                            val change = event.changes[0]
                            if (change.pressed) {
                                val delta = change.position - change.previousPosition
                                totalDrag += delta
                                
                                if (isResizing) {
                                    // 要素ごとの制約
                                    val element = currentElements.find { it.id == activeElementId }
                                    if (element?.type == "CIRCLE") {
                                        // 円は均等リサイズ
                                        val d = (delta.x + delta.y) / 2
                                        sizeDelta += Offset(d / scale, d / scale)
                                    } else {
                                        sizeDelta += Offset(delta.x / scale, delta.y / scale)
                                    }
                                } else if (activeElementId != null) {
                                    dragDelta += Offset(delta.x / scale, delta.y / scale)
                                } else if (isDraggingCanvas) {
                                    offset += delta
                                }
                                change.consume()
                            }
                        }
                    } while (event.changes.fastAny { it.pressed })

                    // 確定処理
                    if (activeElementId != null) {
                        val element = currentElements.find { it.id == activeElementId }
                        if (element != null) {
                            if (isResizing && sizeDelta != Offset.Zero) {
                                val newWidth = (element.width + sizeDelta.x).coerceAtLeast(50f)
                                val newHeight = (element.height + sizeDelta.y).coerceAtLeast(50f)
                                val updated = if (element.type == "TEXT") {
                                    // テキストはフォントサイズと連動
                                    val newFontSize = (element.fontSize + sizeDelta.y).coerceAtLeast(10f)
                                    element.copy(width = newWidth, height = newHeight, fontSize = newFontSize)
                                } else {
                                    element.copy(width = newWidth, height = newHeight)
                                }
                                currentOnElementUpdate(updated)
                            } else if (!isResizing && dragDelta != Offset.Zero) {
                                currentOnElementUpdate(element.copy(x = element.x + dragDelta.x, y = element.y + dragDelta.y))
                            }
                        }
                    }
                    
                    if (!everMultiTouch && !isResizing && totalDrag.getDistance() < 10f) {
                        if (bodyHit == null && !handleHit) {
                            currentOnSelectElement(null)
                            currentOnCanvasClick(cx, cy)
                        }
                    }

                    activeElementId = null
                    dragDelta = Offset.Zero
                    sizeDelta = Offset.Zero
                    isResizing = false
                    isDraggingCanvas = false
                }
            }
    ) {
        // ハードウェア加速を最大限活かす DrawScope での描画
        withTransform({
            translate(offset.x + gyroOffset.x, offset.y + gyroOffset.y)
            scale(scale, scale, Offset.Zero)
        }) {
            for (element in elements) {
                val isDragging = element.id == activeElementId && !isResizing
                val isBeingResized = element.id == activeElementId && isResizing
                
                val alpha = if (isDragging || isBeingResized) 0.7f else 1.0f
                val color = Color(element.color).copy(alpha = alpha)
                
                val renderX = if (isDragging) element.x + dragDelta.x else element.x
                val renderY = if (isDragging) element.y + dragDelta.y else element.y
                val renderW = if (isBeingResized) (element.width + sizeDelta.x).coerceAtLeast(50f) else element.width
                val renderH = if (isBeingResized) (element.height + sizeDelta.y).coerceAtLeast(50f) else element.height

                when (element.type) {
                    "RECTANGLE" -> {
                        drawRect(
                            color = color,
                            topLeft = Offset(renderX, renderY),
                            size = Size(renderW, renderH)
                        )
                    }
                    "CIRCLE" -> {
                        drawCircle(
                            color = color,
                            center = Offset(renderX + renderW / 2, renderY + renderH / 2),
                            radius = renderW / 2
                        )
                    }
                    "TEXT" -> {
                        val renderSize = if (isBeingResized) (element.fontSize + sizeDelta.y).coerceAtLeast(10f) else element.fontSize
                        drawText(
                            textMeasurer = textMeasurer,
                            text = element.text,
                            topLeft = Offset(renderX, renderY),
                            style = androidx.compose.ui.text.TextStyle(
                                color = color,
                                fontSize = renderSize.sp
                            )
                        )
                    }
                }
                
                if (element == selectedElement) {
                    val safeScale = if (scale > 0.001f) scale else 1f
                    // 選択枠
                    drawRect(
                        color = selectionColor,
                        topLeft = Offset(renderX, renderY),
                        size = Size(renderW, renderH),
                        style = Stroke(width = 4f / safeScale)
                    )
                    // リサイズハンドル
                    drawRect(
                        color = selectionColor,
                        topLeft = Offset(renderX + renderW - (20f / safeScale), renderY + renderH - (20f / safeScale)),
                        size = Size(40f / safeScale, 40f / safeScale)
                    )
                }
            }
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

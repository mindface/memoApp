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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import com.example.memoapp.model.CanvasElement
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ConceptCanvas(
    elements: List<CanvasElement>,
    selectedElement: CanvasElement?,
    viewOffset: Offset,
    viewScale: Float,
    isGridEnabled: Boolean,
    onSelectElement: (CanvasElement?) -> Unit,
    onCanvasClick: (Float, Float) -> Unit,
    onElementUpdate: (CanvasElement) -> Unit,
    onViewStateUpdate: (Offset, Float) -> Unit,
    onSnapToGrid: (Float) -> Float,
    modifier: Modifier = Modifier
) {
    // 状態の更新をジェスチャーループに伝えるための rememberUpdatedState
    // これにより pointerInput(Unit) 内で常に最新の値を参照できる
    val currentElements by rememberUpdatedState(elements)
    val currentSelectedElement by rememberUpdatedState(selectedElement)
    val currentOnSelectElement by rememberUpdatedState(onSelectElement)
    val currentOnCanvasClick by rememberUpdatedState(onCanvasClick)
    val currentOnElementUpdate by rememberUpdatedState(onElementUpdate)
    val currentOnViewStateUpdate by rememberUpdatedState(onViewStateUpdate)
    val currentOnSnapToGrid by rememberUpdatedState(onSnapToGrid)

    var scale by remember { mutableFloatStateOf(viewScale) }
    var offset by remember { mutableStateOf(viewOffset) }

    // ViewModelからの外部的な表示状態の変更を検知して同期
    LaunchedEffect(viewOffset, viewScale) {
        offset = viewOffset
        scale = viewScale
    }
    
    // UI用のドラッグ/リサイズ/回転一時状態
    var activeElementId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var sizeDelta by remember { mutableStateOf(Offset.Zero) }
    var rotationDelta by remember { mutableFloatStateOf(0f) }
    var isResizing by remember { mutableStateOf(false) }
    var isRotating by remember { mutableStateOf(false) }
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
                    val totalOffsetAtStart = offset
                    val cx = (downPos.x - totalOffsetAtStart.x) / currentScaleAtStart
                    val cy = (downPos.y - totalOffsetAtStart.y) / currentScaleAtStart
                    
                    // 当たり判定
                    val rotationHandleHit = currentSelectedElement?.let { isOnRotationHandle(it, cx, cy) } ?: false
                    val resizeHandleHit = currentSelectedElement?.let { isOnHandle(it, cx, cy) } ?: false
                    val bodyHit = currentElements.findLast { it.contains(cx, cy) }
                    
                    var totalDrag = Offset.Zero
                    var everMultiTouch = false
                    var startAngle = 0f
                    
                    if (rotationHandleHit) {
                        activeElementId = currentSelectedElement?.id
                        isRotating = true
                        // startAngle is not strictly needed for absolute rotation, 
                        // but let's keep it to support relative rotation if needed.
                        val center = currentSelectedElement!!.center()
                        startAngle = Math.toDegrees(atan2((cy - center.y).toDouble(), (cx - center.x).toDouble())).toFloat()
                    } else if (resizeHandleHit) {
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
                                currentOnViewStateUpdate(offset, scale)
                            }
                            
                            activeElementId = null
                            isResizing = false
                            isRotating = false
                        } else {
                            val change = event.changes[0]
                            if (change.pressed) {
                                val delta = change.position - change.previousPosition
                                totalDrag += delta
                                
                                val currentCX = (change.position.x - offset.x) / scale
                                val currentCY = (change.position.y - offset.y) / scale

                                if (isRotating) {
                                    val element = currentElements.find { it.id == activeElementId }
                                    if (element != null) {
                                        val center = element.center()
                                        val dx = currentCX - center.x
                                        val dy = currentCY - center.y
                                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                                        
                                        // デッドゾーン: 中心に近すぎる場合は回転を計算しない
                                        if (distance > 20f / scale) {
                                            val currentAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                            // 基準点（startAngle）からの相対変化を足す
                                            rotationDelta = currentAngle - startAngle
                                            
                                            // グリッド有効時は15度スナップ
                                            if (isGridEnabled) {
                                                val totalRotation = element.rotation + rotationDelta
                                                val snapped = (totalRotation / 15f).roundToInt() * 15f
                                                rotationDelta = snapped - element.rotation
                                            }
                                        }
                                    }
                                } else if (isResizing) {
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
                                    currentOnViewStateUpdate(offset, scale)
                                }
                                change.consume()
                            }
                        }
                    } while (event.changes.fastAny { it.pressed })

                    // 確定処理
                    if (activeElementId != null) {
                        val element = currentElements.find { it.id == activeElementId }
                        if (element != null) {
                            if (isRotating) {
                                currentOnElementUpdate(element.copy(rotation = (element.rotation + rotationDelta) % 360f))
                            } else if (isResizing && sizeDelta != Offset.Zero) {
                                val newWidth = currentOnSnapToGrid((element.width + sizeDelta.x).coerceAtLeast(50f))
                                val newHeight = currentOnSnapToGrid((element.height + sizeDelta.y).coerceAtLeast(50f))
                                
                                val updated = if (element.type == "TEXT") {
                                    // テキストはフォントサイズと連動。実際の描画サイズを計測してwidth/heightを決める
                                    val newFontSize = (element.fontSize + sizeDelta.y).coerceAtLeast(10f)
                                    val layoutResult = textMeasurer.measure(
                                        text = element.text,
                                        style = androidx.compose.ui.text.TextStyle(fontSize = newFontSize.sp)
                                    )
                                    element.copy(
                                        fontSize = newFontSize,
                                        width = layoutResult.size.width.toFloat(),
                                        height = layoutResult.size.height.toFloat()
                                    )
                                } else {
                                    element.copy(width = newWidth, height = newHeight)
                                }
                                currentOnElementUpdate(updated)
                            } else if (!isResizing && dragDelta != Offset.Zero) {
                                val newX = currentOnSnapToGrid(element.x + dragDelta.x)
                                val newY = currentOnSnapToGrid(element.y + dragDelta.y)
                                currentOnElementUpdate(element.copy(x = newX, y = newY))
                            }
                        }
                    }
                    
                    if (!everMultiTouch && !isResizing && !isRotating && totalDrag.getDistance() < 10f) {
                        if (bodyHit == null && !resizeHandleHit && !rotationHandleHit) {
                            currentOnSelectElement(null)
                            currentOnCanvasClick(cx, cy)
                        }
                    }

                    activeElementId = null
                    dragDelta = Offset.Zero
                    sizeDelta = Offset.Zero
                    rotationDelta = 0f
                    isResizing = false
                    isRotating = false
                    isDraggingCanvas = false
                }
            }
    ) {
        if (isGridEnabled) {
            drawGrid(offset, scale)
        }

        // ハードウェア加速を最大限活かす DrawScope での描画
        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, Offset.Zero)
        }) {
            for (element in elements) {
                val isDragging = element.id == activeElementId && !isResizing && !isRotating
                val isBeingResized = element.id == activeElementId && isResizing
                val isBeingRotated = element.id == activeElementId && isRotating
                
                val alpha = if (isDragging || isBeingResized || isBeingRotated) 0.7f else 1.0f
                val color = Color(element.color).copy(alpha = alpha)
                
                // ドラッグ/リサイズ中のプレビューでもスナップを適用
                val rawX = if (isDragging) element.x + dragDelta.x else element.x
                val rawY = if (isDragging) element.y + dragDelta.y else element.y
                val renderX = if (isDragging) currentOnSnapToGrid(rawX) else rawX
                val renderY = if (isDragging) currentOnSnapToGrid(rawY) else rawY

                val renderSize = if (isBeingResized && element.type == "TEXT") {
                    (element.fontSize + sizeDelta.y).coerceAtLeast(10f)
                } else element.fontSize

                var renderW = if (isBeingResized) currentOnSnapToGrid((element.width + sizeDelta.x).coerceAtLeast(50f)) else element.width
                var renderH = if (isBeingResized) currentOnSnapToGrid((element.height + sizeDelta.y).coerceAtLeast(50f)) else element.height
                
                // テキストの場合、リサイズ中はフォントサイズに合わせて枠を再計算
                if (element.type == "TEXT" && isBeingResized) {
                    val layout = textMeasurer.measure(element.text, androidx.compose.ui.text.TextStyle(fontSize = renderSize.sp))
                    renderW = layout.size.width.toFloat()
                    renderH = layout.size.height.toFloat()
                }

                val renderRotation = if (isBeingRotated) element.rotation + rotationDelta else element.rotation
                val renderCenter = Offset(renderX + renderW / 2, renderY + renderH / 2)

                withTransform({
                    rotate(renderRotation, renderCenter)
                }) {
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
                        "ARROW" -> {
                            drawArrow(renderX, renderY, renderX + renderW, renderY + renderH, color)
                        }
                        "TEXT" -> {
                            // テキストが画面外で強制的に折り返されないように、十分な幅のConstraintsを指定
                            val layoutResult = textMeasurer.measure(
                                text = element.text,
                                style = androidx.compose.ui.text.TextStyle(
                                    color = color,
                                    fontSize = renderSize.sp
                                ),
                                softWrap = false,
                                constraints = androidx.compose.ui.unit.Constraints(maxWidth = 10000)
                            )
                            drawText(
                                textLayoutResult = layoutResult,
                                topLeft = Offset(renderX, renderY)
                            )
                        }
                    }
                }
                
                if (element == selectedElement) {
                    val safeScale = if (scale > 0.001f) scale else 1f
                    
                    withTransform({
                        rotate(renderRotation, renderCenter)
                    }) {
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

                    // 回転ハンドル本体 (中心に配置)
                    val handleCenter = renderCenter
                    drawCircle(
                        color = selectionColor,
                        center = handleCenter,
                        radius = 25f / safeScale
                    )
                    // 回転アイコン風の十字を描画
                    drawLine(
                        color = Color.White,
                        start = Offset(handleCenter.x - 10f / safeScale, handleCenter.y),
                        end = Offset(handleCenter.x + 10f / safeScale, handleCenter.y),
                        strokeWidth = 2f / safeScale
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(handleCenter.x, handleCenter.y - 10f / safeScale),
                        end = Offset(handleCenter.x, handleCenter.y + 10f / safeScale),
                        strokeWidth = 2f / safeScale
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(offset: Offset, scale: Float) {
    val gridSize = 50f * scale
    val startX = offset.x % gridSize
    val startY = offset.y % gridSize
    
    val dotColor = Color.LightGray.copy(alpha = 0.5f)
    val dotRadius = 2f
    
    var x = startX
    while (x < size.width) {
        var y = startY
        while (y < size.height) {
            drawCircle(dotColor, radius = dotRadius, center = Offset(x, y))
            y += gridSize
        }
        x += gridSize
    }
}

private fun DrawScope.drawArrow(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
    val headSize = 30f
    val angle = atan2(y2 - y1, x2 - x1)
    
    // 主線
    drawLine(
        color = color,
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = 10f
    )
    
    // 矢印の頭
    val path = Path().apply {
        moveTo(x2, y2)
        lineTo(
            x2 - headSize * cos(angle - 0.5f).toFloat(),
            y2 - headSize * sin(angle - 0.5f).toFloat()
        )
        lineTo(
            x2 - headSize * cos(angle + 0.5f).toFloat(),
            y2 - headSize * sin(angle + 0.5f).toFloat()
        )
        close()
    }
    drawPath(path, color)
}

private fun CanvasElement.center(): Offset {
    return Offset(x + width / 2, y + height / 2)
}

private fun rotatePoint(point: Offset, center: Offset, angleDegrees: Float): Offset {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)
    val dx = point.x - center.x
    val dy = point.y - center.y
    return Offset(
        (center.x + dx * cosA - dy * sinA).toFloat(),
        (center.y + dx * sinA + dy * cosA).toFloat()
    )
}

private fun CanvasElement.contains(cx: Float, cy: Float): Boolean {
    // 回転を考慮した当たり判定
    val center = center()
    val rotatedPoint = rotatePoint(Offset(cx, cy), center, -rotation)
    return rotatedPoint.x >= x && rotatedPoint.x <= x + width &&
           rotatedPoint.y >= y && rotatedPoint.y <= y + height
}

private fun isOnHandle(element: CanvasElement, cx: Float, cy: Float): Boolean {
    val handleSize = 40f
    val center = element.center()
    val rotatedPoint = rotatePoint(Offset(cx, cy), center, -element.rotation)
    val hx = element.x + element.width
    val hy = element.y + element.height
    return rotatedPoint.x >= hx - handleSize && rotatedPoint.x <= hx + handleSize &&
           rotatedPoint.y >= hy - handleSize && rotatedPoint.y <= hy + handleSize
}

private fun isOnRotationHandle(element: CanvasElement, cx: Float, cy: Float): Boolean {
    val handleSize = 50f
    val center = element.center()
    
    val dx = cx - center.x
    val dy = cy - center.y
    return (dx * dx + dy * dy) <= handleSize * handleSize
}

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
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
    val currentElements by rememberUpdatedState(elements)
    val currentSelectedElement by rememberUpdatedState(selectedElement)
    val currentOnSelectElement by rememberUpdatedState(onSelectElement)
    val currentOnCanvasClick by rememberUpdatedState(onCanvasClick)
    val currentOnElementUpdate by rememberUpdatedState(onElementUpdate)
    val currentOnViewStateUpdate by rememberUpdatedState(onViewStateUpdate)
    val currentOnSnapToGrid by rememberUpdatedState(onSnapToGrid)

    var scale by remember { mutableFloatStateOf(viewScale) }
    var offset by remember { mutableStateOf(viewOffset) }

    LaunchedEffect(viewOffset, viewScale) {
        offset = viewOffset
        scale = viewScale
    }
    
    var activeElementId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var sizeDelta by remember { mutableStateOf(Offset.Zero) }
    var rotationDelta by remember { mutableFloatStateOf(0f) }
    var isResizing by remember { mutableStateOf(false) }
    var isRotating by remember { mutableStateOf(false) }
    var isDraggingCanvas by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()
    val selectionColor = Color(0xFF4285F4)

    // Helper to get measured bounds of an element
    fun getElementBounds(el: CanvasElement, resizeDelta: Offset = Offset.Zero): Rect {
        var w = el.width
        var h = el.height
        
        if (el.type == "TEXT") {
            val fontSize = (el.fontSize + resizeDelta.y).coerceAtLeast(10f)
            val layout = textMeasurer.measure(
                el.text,
                androidx.compose.ui.text.TextStyle(fontSize = fontSize.sp),
                softWrap = false,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = 10000)
            )
            w = layout.size.width.toFloat()
            h = layout.size.height.toFloat()
        } else {
            w = (w + resizeDelta.x).coerceAtLeast(50f)
            h = (h + resizeDelta.y).coerceAtLeast(50f)
        }
        
        return Rect(el.x, el.y, el.x + w, el.y + h)
    }

    // Auto-update newly added elements
    LaunchedEffect(currentElements) {
        currentElements.forEach { element ->
            if (element.type == "TEXT" && element.width <= 1f && element.text.isNotEmpty()) {
                val bounds = getElementBounds(element)
                currentOnElementUpdate(element.copy(width = bounds.width, height = bounds.height))
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val downPos = down.position
                    val cx = (downPos.x - offset.x) / scale
                    val cy = (downPos.y - offset.y) / scale
                    
                    val sel = currentSelectedElement
                    val bounds = sel?.let { getElementBounds(it) }
                    
                    val rotationHandleHit = sel != null && bounds != null && isOnRotationHandle(sel, cx, cy, bounds)
                    val resizeHandleHit = sel != null && bounds != null && isOnHandle(sel, cx, cy, bounds)
                    
                    val bodyHit = currentElements.findLast { el ->
                        contains(el, cx, cy, getElementBounds(el))
                    }
                    
                    var totalDrag = Offset.Zero
                    var everMultiTouch = false
                    var startAngle = 0f
                    
                    if (rotationHandleHit) {
                        activeElementId = sel?.id
                        isRotating = true
                        val center = bounds!!.center
                        startAngle = Math.toDegrees(atan2((cy - center.y).toDouble(), (cx - center.x).toDouble())).toFloat()
                    } else if (resizeHandleHit) {
                        activeElementId = sel?.id
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
                                        val center = getElementBounds(element).center
                                        val currentAngle = Math.toDegrees(atan2((currentCY - center.y).toDouble(), (currentCX - center.x).toDouble())).toFloat()
                                        rotationDelta = currentAngle - startAngle
                                        if (isGridEnabled) {
                                            val totalRotation = element.rotation + rotationDelta
                                            val snapped = (totalRotation / 15f).roundToInt() * 15f
                                            rotationDelta = snapped - element.rotation
                                        }
                                    }
                                } else if (isResizing) {
                                    sizeDelta += Offset(delta.x / scale, delta.y / scale)
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

                    if (activeElementId != null) {
                        val element = currentElements.find { it.id == activeElementId }
                        if (element != null) {
                            if (isRotating) {
                                currentOnElementUpdate(element.copy(rotation = (element.rotation + rotationDelta) % 360f))
                            } else if (isResizing && sizeDelta != Offset.Zero) {
                                val b = getElementBounds(element, sizeDelta)
                                val updated = if (element.type == "TEXT") {
                                    element.copy(
                                        fontSize = (element.fontSize + sizeDelta.y).coerceAtLeast(10f),
                                        width = b.width,
                                        height = b.height
                                    )
                                } else {
                                    element.copy(
                                        width = currentOnSnapToGrid(b.width),
                                        height = currentOnSnapToGrid(b.height)
                                    )
                                }
                                currentOnElementUpdate(updated)
                            } else if (!isResizing && dragDelta != Offset.Zero) {
                                currentOnElementUpdate(element.copy(
                                    x = currentOnSnapToGrid(element.x + dragDelta.x),
                                    y = currentOnSnapToGrid(element.y + dragDelta.y)
                                ))
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
        if (isGridEnabled) drawGrid(offset, scale)

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
                
                val renderX = if (isDragging) currentOnSnapToGrid(element.x + dragDelta.x) else element.x
                val renderY = if (isDragging) currentOnSnapToGrid(element.y + dragDelta.y) else element.y

                val currentSizeDelta = if (isBeingResized) sizeDelta else Offset.Zero
                val b = getElementBounds(element, currentSizeDelta)
                val renderW = b.width
                val renderH = b.height
                val renderSize = if (isBeingResized && element.type == "TEXT") (element.fontSize + sizeDelta.y).coerceAtLeast(10f) else element.fontSize

                val renderRotation = if (isBeingRotated) element.rotation + rotationDelta else element.rotation
                val renderCenter = Offset(renderX + renderW / 2, renderY + renderH / 2)
                val safeScale = if (scale > 0.001f) scale else 1f

                withTransform({ rotate(renderRotation, renderCenter) }) {
                    when (element.type) {
                        "RECTANGLE" -> {
                            // Fill
                            if (element.drawStyle == 0 || element.drawStyle == 1) {
                                drawRect(
                                    color = color,
                                    topLeft = Offset(renderX, renderY),
                                    size = Size(renderW, renderH)
                                )
                            }
                            // Outline
                            if (element.drawStyle == 0 || element.drawStyle == 2) {
                                drawRect(
                                    color = Color(element.strokeColor),
                                    topLeft = Offset(renderX, renderY),
                                    size = Size(renderW, renderH),
                                    style = Stroke(width = 2f / safeScale)
                                )
                            }
                        }
                        "CIRCLE" -> {
                            // Fill
                            if (element.drawStyle == 0 || element.drawStyle == 1) {
                                drawCircle(
                                    color = color,
                                    radius = renderW / 2,
                                    center = renderCenter
                                )
                            }
                            // Outline
                            if (element.drawStyle == 0 || element.drawStyle == 2) {
                                drawCircle(
                                    color = Color(element.strokeColor),
                                    radius = renderW / 2,
                                    center = renderCenter,
                                    style = Stroke(width = 2f / safeScale)
                                )
                            }
                        }
                        "ARROW" -> drawArrow(
                            x1 = renderX,
                            y1 = renderY + renderH / 2,
                            x2 = renderX + renderW,
                            y2 = renderY + renderH / 2,
                            color = color,
                            strokeColor = Color(element.strokeColor),
                            drawStyle = element.drawStyle
                        )
                        "TEXT" -> {
                            val layout = textMeasurer.measure(
                                text = element.text,
                                style = androidx.compose.ui.text.TextStyle(color = color, fontSize = renderSize.sp),
                                softWrap = false,
                                constraints = androidx.compose.ui.unit.Constraints(maxWidth = 10000)
                            )
                            drawText(
                                textLayoutResult = layout,
                                topLeft = Offset(renderX, renderY)
                            )
                        }
                    }
                }

                // クラウド共有中のインジケーター（雲アイコン）
                if (element.isShared) {
                    val cloudIconSize = 24f / safeScale
                    val cx = renderX + renderW - cloudIconSize
                    val cy = renderY - cloudIconSize
                    
                    drawCircle(
                        color = Color.LightGray,
                        radius = cloudIconSize / 2,
                        center = Offset(cx, cy)
                    )
                    // シンプルな雲の形（3つの丸）を擬似的に描画
                    drawCircle(Color.LightGray, cloudIconSize / 3, Offset(cx - 5f / safeScale, cy))
                    drawCircle(Color.LightGray, cloudIconSize / 3, Offset(cx + 5f / safeScale, cy))
                }

                // リンクされたアイテムのインジケーター（書類アイコン）
                if (element.linkedItemId != null) {
                    val linkIconSize = 24f / safeScale
                    val lx = renderX + 4f / safeScale
                    val ly = renderY + 4f / safeScale
                    
                    // アイコン背景
                    drawRect(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = Offset(lx, ly),
                        size = Size(linkIconSize, linkIconSize)
                    )
                    // シンプルな書類アイコン風の線
                    drawLine(Color.Gray, Offset(lx + 4f/safeScale, ly + 6f/safeScale), Offset(lx + 20f/safeScale, ly + 6f/safeScale), 2f/safeScale)
                    drawLine(Color.Gray, Offset(lx + 4f/safeScale, ly + 12f/safeScale), Offset(lx + 20f/safeScale, ly + 12f/safeScale), 2f/safeScale)
                    drawLine(Color.Gray, Offset(lx + 4f/safeScale, ly + 18f/safeScale), Offset(lx + 12f/safeScale, ly + 18f/safeScale), 2f/safeScale)
                }
                
                if (element == selectedElement) {
                    val safeScale = if (scale > 0.001f) scale else 1f
                    withTransform({ rotate(renderRotation, renderCenter) }) {
                        drawRect(
                            color = selectionColor,
                            topLeft = Offset(renderX, renderY),
                            size = Size(renderW, renderH),
                            style = Stroke(width = 4f / safeScale)
                        )
                        drawRect(
                            color = selectionColor,
                            topLeft = Offset(renderX + renderW - (20f / safeScale), renderY + renderH - (20f / safeScale)),
                            size = Size(40f / safeScale, 40f / safeScale)
                        )
                    }
                    val handleCenter = rotatePoint(Offset(renderX + renderW / 2, renderY - (60f / safeScale)), renderCenter, renderRotation)
                    drawCircle(
                        color = selectionColor,
                        radius = 25f / safeScale,
                        center = handleCenter
                    )
                    drawLine(Color.White, Offset(handleCenter.x - 10f / safeScale, handleCenter.y), Offset(handleCenter.x + 10f / safeScale, handleCenter.y), 2f / safeScale)
                    drawLine(Color.White, Offset(handleCenter.x, handleCenter.y - 10f / safeScale), Offset(handleCenter.x, handleCenter.y + 10f / safeScale), 2f / safeScale)
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(offset: Offset, scale: Float) {
    val gridSize = 50f * scale
    val dotColor = Color.LightGray.copy(alpha = 0.5f)
    var x = offset.x % gridSize
    while (x < size.width) {
        var y = offset.y % gridSize
        while (y < size.height) {
            drawCircle(dotColor, 2f, Offset(x, y))
            y += gridSize
        }
        x += gridSize
    }
}

private fun DrawScope.drawArrow(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, strokeColor: Color, drawStyle: Int) {
    val headSize = 40f
    val angle = atan2(y2 - y1, x2 - x1)
    
    // Main shaft uses stroke color
    if (drawStyle == 0 || drawStyle == 2) {
        drawLine(strokeColor, Offset(x1, y1), Offset(x2, y2), 10f)
    }
    
    val path = Path().apply {
        moveTo(x2, y2)
        lineTo(x2 - headSize * cos(angle - 0.4f), y2 - headSize * sin(angle - 0.4f))
        lineTo(x2 - (headSize * 0.7f) * cos(angle), y2 - (headSize * 0.7f) * sin(angle))
        lineTo(x2 - headSize * cos(angle + 0.4f), y2 - headSize * sin(angle + 0.4f))
        close()
    }
    
    // Fill
    if (drawStyle == 0 || drawStyle == 1) {
        drawPath(path, color)
    }
    
    // Outline
    if (drawStyle == 0 || drawStyle == 2) {
        drawPath(path, strokeColor, style = Stroke(width = 2f))
    }
}

private fun rotatePoint(point: Offset, center: Offset, angleDegrees: Float): Offset {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val cosA = cos(angleRad); val sinA = sin(angleRad)
    val dx = point.x - center.x; val dy = point.y - center.y
    return Offset((center.x + dx * cosA - dy * sinA).toFloat(), (center.y + dx * sinA + dy * cosA).toFloat())
}

private fun contains(el: CanvasElement, cx: Float, cy: Float, bounds: Rect): Boolean {
    val rotatedPoint = rotatePoint(Offset(cx, cy), bounds.center, -el.rotation)
    val minHit = 48f
    val hitW = bounds.width.coerceAtLeast(minHit); val hitH = bounds.height.coerceAtLeast(minHit)
    val left = bounds.left - (hitW - bounds.width) / 2; val top = bounds.top - (hitH - bounds.height) / 2
    return rotatedPoint.x >= left && rotatedPoint.x <= left + hitW && rotatedPoint.y >= top && rotatedPoint.y <= top + hitH
}

private fun isOnHandle(el: CanvasElement, cx: Float, cy: Float, bounds: Rect): Boolean {
    val rotatedPoint = rotatePoint(Offset(cx, cy), bounds.center, -el.rotation)
    val hs = 40f
    return rotatedPoint.x >= bounds.right - hs && rotatedPoint.x <= bounds.right + hs && rotatedPoint.y >= bounds.bottom - hs && rotatedPoint.y <= bounds.bottom + hs
}

private fun isOnRotationHandle(el: CanvasElement, cx: Float, cy: Float, bounds: Rect): Boolean {
    val handlePos = rotatePoint(Offset(bounds.left + bounds.width / 2, bounds.top - 60f), bounds.center, el.rotation)
    val dx = cx - handlePos.x; val dy = cy - handlePos.y
    return (dx * dx + dy * dy) <= 50f * 50f
}

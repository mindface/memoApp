package com.example.memoapp.ui.measurement

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import com.example.memoapp.model.CounterElement

@Composable
fun MeasurementCanvas(
    elements: List<CounterElement>,
    viewOffset: Offset,
    viewScale: Float,
    onIncrement: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBringToFront: (String) -> Unit,
    onElementUpdate: (CounterElement) -> Unit,
    onCanvasClick: (Float, Float) -> Unit,
    onViewStateUpdate: (Offset, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentElements by rememberUpdatedState(elements)
    val currentOnIncrement by rememberUpdatedState(onIncrement)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnBringToFront by rememberUpdatedState(onBringToFront)
    val currentOnElementUpdate by rememberUpdatedState(onElementUpdate)
    val currentOnCanvasClick by rememberUpdatedState(onCanvasClick)
    val currentOnViewStateUpdate by rememberUpdatedState(onViewStateUpdate)

    var scale by remember { mutableFloatStateOf(viewScale) }
    var offset by remember { mutableStateOf(viewOffset) }

    LaunchedEffect(viewOffset, viewScale) {
        offset = viewOffset
        scale = viewScale
    }

    var activeElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var sizeDelta by remember { mutableStateOf(Offset.Zero) }
    var isResizing by remember { mutableStateOf(false) }
    var isDraggingCanvas by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()
    val selectionColor = Color(0xFF4285F4)

    fun getElementBounds(el: CounterElement, resizeDelta: Offset = Offset.Zero): Rect {
        val w = (el.width + resizeDelta.x).coerceAtLeast(220f)
        val h = (el.height + resizeDelta.y).coerceAtLeast(100f)
        return Rect(el.x, el.y, el.x + w, el.y + h)
    }

    fun isOnResizeHandle(cx: Float, cy: Float, bounds: Rect): Boolean {
        val hs = 40f
        return cx >= bounds.right - hs && cx <= bounds.right + hs && cy >= bounds.bottom - hs && cy <= bounds.bottom + hs
    }

    fun isDeleteButtonHit(cx: Float, cy: Float, bounds: Rect): Boolean {
        val deleteButtonRect = Rect(bounds.right - 40f, bounds.top, bounds.right, bounds.top + 40f)
        return deleteButtonRect.contains(Offset(cx, cy))
    }

    fun isPlusButtonHit(cx: Float, cy: Float, bounds: Rect): Boolean {
        val plusRect = Rect(bounds.right - 65f, bounds.top + (bounds.height - 45f) / 2f, bounds.right - 10f, bounds.top + (bounds.height - 45f) / 2f + 45f)
        return plusRect.contains(Offset(cx, cy))
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

                    val sel = currentElements.find { it.id == selectedElementId }
                    val selBounds = sel?.let { getElementBounds(it) }

                    val resizeHandleHit = sel != null && selBounds != null && isOnResizeHandle(cx, cy, selBounds)
                    val deleteHit = sel != null && selBounds != null && isDeleteButtonHit(cx, cy, selBounds)
                    val plusHit = sel != null && selBounds != null && isPlusButtonHit(cx, cy, selBounds)
                    
                    val bodyHit = currentElements.findLast { el ->
                        getElementBounds(el).contains(Offset(cx, cy))
                    }

                    var totalDrag = Offset.Zero
                    var everMultiTouch = false

                    if (deleteHit || plusHit) {
                        activeElementId = null
                        if (plusHit && sel != null) {
                            currentOnBringToFront(sel.id)
                        }
                    } else if (resizeHandleHit) {
                        activeElementId = sel?.id
                        isResizing = true
                        sel?.id?.let { currentOnBringToFront(it) }
                    } else if (bodyHit != null) {
                        activeElementId = bodyHit.id
                        selectedElementId = bodyHit.id
                        currentOnBringToFront(bodyHit.id)
                    } else {
                        activeElementId = null
                        selectedElementId = null
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
                                    currentOnViewStateUpdate(offset, scale)
                                }
                                change.consume()
                            }
                        }
                    } while (event.changes.fastAny { it.pressed })

                    if (activeElementId != null) {
                        val element = currentElements.find { it.id == activeElementId }
                        if (element != null) {
                            if (isResizing && sizeDelta != Offset.Zero) {
                                val b = getElementBounds(element, sizeDelta)
                                currentOnElementUpdate(
                                    element.copy(
                                        width = b.width - element.x,
                                        height = b.height - element.y
                                    )
                                )
                            } else if (!isResizing && dragDelta != Offset.Zero) {
                                currentOnElementUpdate(
                                    element.copy(
                                        x = element.x + dragDelta.x,
                                        y = element.y + dragDelta.y
                                    )
                                )
                            }
                        }
                    }

                    // Tap detection
                    if (!everMultiTouch && !isResizing && totalDrag.getDistance() < 10f) {
                        if (deleteHit && sel != null) {
                            currentOnDelete(sel.id)
                            selectedElementId = null
                        } else if (plusHit && sel != null) {
                            currentOnIncrement(sel.id)
                        } else if (resizeHandleHit) {
                            // Handled
                        } else if (bodyHit != null) {
                            selectedElementId = bodyHit.id
                        } else {
                            selectedElementId = null
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
        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, Offset.Zero)
        }) {
            for (element in elements) {
                val isDragging = element.id == activeElementId && !isResizing
                val isBeingResized = element.id == activeElementId && isResizing

                val renderX = if (isDragging) element.x + dragDelta.x else element.x
                val renderY = if (isDragging) element.y + dragDelta.y else element.y

                val currentSizeDelta = if (isBeingResized) sizeDelta else Offset.Zero
                val b = getElementBounds(element, currentSizeDelta)
                val renderW = b.width - b.left
                val renderH = b.height - b.top

                drawRect(
                    color = Color.White,
                    topLeft = Offset(renderX, renderY),
                    size = Size(renderW, renderH)
                )
                drawRect(
                    color = Color(0xFF4285F4),
                    topLeft = Offset(renderX, renderY),
                    size = Size(renderW, renderH),
                    style = Stroke(width = 3f)
                )

                // Text layout leaving room for the right-side plus button
                val textLayout = textMeasurer.measure(
                    text = "${element.title}\nCount: ${element.count}",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.Black,
                        fontSize = maxOf(element.fontSize, 18f).sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (renderW - 85f).coerceAtLeast(10f).toInt())
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        renderX + 12f,
                        renderY + (renderH - textLayout.size.height) / 2f
                    )
                )

                // Plus button inside block (right side, vertically centered)
                val plusRect = Rect(renderX + renderW - 65f, renderY + (renderH - 45f) / 2f, renderX + renderW - 10f, renderY + (renderH - 45f) / 2f + 45f)
                drawRect(
                    color = Color(0xFF4285F4),
                    topLeft = Offset(plusRect.left, plusRect.top),
                    size = Size(plusRect.width, plusRect.height)
                )
                val plusTextLayout = textMeasurer.measure(
                    text = "+",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = plusTextLayout,
                    topLeft = Offset(
                        plusRect.left + (plusRect.width - plusTextLayout.size.width) / 2f,
                        plusRect.top + (plusRect.height - plusTextLayout.size.height) / 2f
                    )
                )

                // If selected, show delete button (top-right) and resize handle
                if (element.id == selectedElementId) {
                    val safeScale = if (scale > 0.001f) scale else 1f
                    
                    // Selection border
                    drawRect(
                        color = selectionColor,
                        topLeft = Offset(renderX, renderY),
                        size = Size(renderW, renderH),
                        style = Stroke(width = 4f / safeScale)
                    )
                    
                    // Resize handle (bottom-right)
                    drawRect(
                        color = selectionColor,
                        topLeft = Offset(renderX + renderW - (20f / safeScale), renderY + renderH - (20f / safeScale)),
                        size = Size(40f / safeScale, 40f / safeScale)
                    )

                    // Delete button (top-right)
                    val deleteCenter = Offset(renderX + renderW - 20f / safeScale, renderY + 20f / safeScale)
                    drawCircle(
                        color = Color.Red,
                        radius = 16f / safeScale,
                        center = deleteCenter
                    )
                    drawLine(Color.White, Offset(deleteCenter.x - 6f / safeScale, deleteCenter.y - 6f / safeScale), Offset(deleteCenter.x + 6f / safeScale, deleteCenter.y + 6f / safeScale), 3f / safeScale)
                    drawLine(Color.White, Offset(deleteCenter.x + 6f / safeScale, deleteCenter.y - 6f / safeScale), Offset(deleteCenter.x - 6f / safeScale, deleteCenter.y + 6f / safeScale), 3f / safeScale)
                }
            }
        }
    }
}

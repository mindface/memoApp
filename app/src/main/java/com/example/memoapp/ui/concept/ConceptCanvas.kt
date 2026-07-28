package com.example.memoapp.ui.concept

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
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
            .pointerInput(selectedElement) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (selectedElement == null) {
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                        offset += pan
                    } else {
                        // Dragging selected element
                        val dx = pan.x / scale
                        val dy = pan.y / scale
                        selectedElement.x += dx
                        selectedElement.y += dy
                        onElementUpdate(selectedElement)
                    }
                }
            }
            .pointerInput(elements, scale, offset) {
                detectTapGestures { tapOffset ->
                    val cx = (tapOffset.x - size.width / 2f) / scale + size.width / 2f - offset.x / scale
                    val cy = (tapOffset.y - size.height / 2f) / scale + size.height / 2f - offset.y / scale

                    val hit = elements.findLast { it.contains(cx, cy) }
                    if (hit != null) {
                        onSelectElement(hit)
                    } else {
                        onSelectElement(null)
                        onCanvasClick(cx, cy)
                    }
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            
            nativeCanvas.save()
            nativeCanvas.translate(offset.x, offset.y)
            nativeCanvas.scale(scale, scale, size.width / 2f, size.height / 2f)

            for (element in elements) {
                elementPaint.color = element.color
                
                when (element.type) {
                    "RECTANGLE" -> {
                        nativeCanvas.drawRect(element.x, element.y, element.x + element.width, element.y + element.height, elementPaint)
                    }
                    "CIRCLE" -> {
                        nativeCanvas.drawCircle(element.x + element.width / 2, element.y + element.height / 2, element.width / 2, elementPaint)
                    }
                    "TEXT" -> {
                        textPaint.textSize = element.fontSize
                        nativeCanvas.drawText(element.text, element.x, element.y + element.fontSize, textPaint)
                    }
                }
                
                if (element == selectedElement) {
                    nativeCanvas.drawRect(element.x, element.y, element.x + element.width, element.y + element.height, selectionPaint)
                    nativeCanvas.drawRect(
                        element.x + element.width - 20f, 
                        element.y + element.height - 20f, 
                        element.x + element.width + 20f, 
                        element.y + element.height + 20f, 
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

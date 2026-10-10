package com.example.memoapp.ui.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memoapp.model.CounterElement
import kotlin.math.roundToInt

@Composable
fun MeasurementCanvas(
    elements: List<CounterElement>,
    selectedElementId: String?,
    viewOffset: Offset,
    viewScale: Float,
    onIncrement: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSelectElement: (String?) -> Unit,
    onMoveSelected: (Float, Float) -> Unit,
    onRepositionSelected: (String, Float, Float) -> Unit,
    onElementUpdate: (CounterElement) -> Unit,
    onCanvasClick: (Float, Float) -> Unit,
    onViewStateUpdate: (Offset, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(viewScale) }
    var offset by remember { mutableStateOf(viewOffset) }

    LaunchedEffect(viewOffset, viewScale) {
        offset = viewOffset
        scale = viewScale
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.1f, 5f)
                    scale = newScale
                    offset += pan
                    onViewStateUpdate(offset, scale)
                }
            }
            .pointerInput(selectedElementId) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val cx = (tapOffset.x - offset.x) / scale
                        val cy = (tapOffset.y - offset.y) / scale
                        if (selectedElementId != null) {
                            onRepositionSelected(selectedElementId, cx, cy)
                        } else {
                            onCanvasClick(cx, cy)
                        }
                    }
                )
            }
    ) {
        elements.forEach { element ->
            val screenX = element.x * scale + offset.x
            val screenY = element.y * scale + offset.y
            val isSelected = element.id == selectedElementId

            Card(
                modifier = Modifier
                    .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                    .width(230.dp)
                    .pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = { onSelectElement(element.id) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val updated = element.copy(
                                    x = element.x + (dragAmount.x / scale),
                                    y = element.y + (dragAmount.y / scale)
                                )
                                onElementUpdate(updated)
                            }
                        )
                    }
                    .clickable {
                        onSelectElement(element.id)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4285F4)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = element.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onDelete(element.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("×", style = MaterialTheme.typography.titleMedium, color = Color.Red)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Count: ${element.count}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { onIncrement(element.id) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("+1")
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onMoveSelected(-30f, 0f) },
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("←", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { onMoveSelected(0f, -30f) },
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("↑", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { onMoveSelected(0f, 30f) },
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("↓", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { onMoveSelected(30f, 0f) },
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.size(32.dp)
                            ) { Text("→", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

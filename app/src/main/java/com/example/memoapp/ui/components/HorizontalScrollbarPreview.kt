package com.example.memoapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HorizontalScrollbarPreview(
    lines: List<String>,
    onJumpToLine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    var previewText by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        AnimatedVisibility(
            visible = previewIndex != null,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            if (previewIndex != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clickable {
                            previewIndex?.let { onJumpToLine(it) }
                            previewIndex = null
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "タップして移動 (${previewIndex!! + 1}行目)",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = previewText.ifEmpty { "(空行)" },
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { previewIndex = null }) {
                            Text("閉じる")
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(28.dp)
                .background(Color(0xFFF0F0F0), shape = MaterialTheme.shapes.small)
                .pointerInput(lines) {
                    detectTapGestures { tapOffset ->
                        if (lines.isNotEmpty()) {
                            val ratio = (tapOffset.y / size.height).coerceIn(0f, 1f)
                            val index = (ratio * (lines.size - 1)).toInt().coerceIn(0, lines.size - 1)
                            previewIndex = index
                            previewText = lines[index]
                        }
                    }
                }
                .padding(4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 4f
                drawRect(
                    color = Color.LightGray,
                    topLeft = Offset(size.width / 2f - trackWidth / 2f, 0f),
                    size = Size(trackWidth, size.height)
                )
                previewIndex?.let { idx ->
                    val progress = idx.toFloat() / lines.size.coerceAtLeast(1)
                    val thumbY = size.height * progress
                    drawCircle(
                        color = Color(0xFF4285F4),
                        radius = 6f,
                        center = Offset(size.width / 2f, thumbY)
                    )
                }
            }
        }
    }
}

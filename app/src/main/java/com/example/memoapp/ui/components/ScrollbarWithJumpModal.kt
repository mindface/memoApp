package com.example.memoapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ScrollbarWithJumpModal(
    lines: List<String>,
    onJumpToLine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showModal by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedText by remember { mutableStateOf("") }

    if (showModal) {
        AlertDialog(
            onDismissRequest = { showModal = false },
            title = {
                Text(
                    text = "位置の確認 (${selectedIndex + 1}行目)",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = selectedText.ifEmpty { "(空行)" },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJumpToLine(selectedIndex)
                        showModal = false
                    }
                ) {
                    Text("移動する")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showModal = false }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .background(Color(0xFFF0F0F0), shape = MaterialTheme.shapes.small)
            .pointerInput(lines) {
                detectTapGestures { tapOffset ->
                    if (lines.isNotEmpty()) {
                        val ratio = (tapOffset.y / size.height).coerceIn(0f, 1f)
                        val index = (ratio * (lines.size - 1)).toInt().coerceIn(0, lines.size - 1)
                        selectedIndex = index
                        selectedText = lines[index]
                        showModal = true
                    }
                }
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = 4f
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(size.width / 2f - trackWidth / 2f, 0f),
                size = Size(trackWidth, size.height)
            )
            if (showModal) {
                val progress = selectedIndex.toFloat() / lines.size.coerceAtLeast(1)
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

package com.example.memoapp.ui.log

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogItemDetailModal(
    isShared: Boolean,
    noteContent: String,
    onToggleShare: (Boolean) -> Unit,
    onSymbolize: () -> Unit,
    onShareExternally: () -> Unit,
    onPopOut: () -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val lines = remember(noteContent) { noteContent.lines() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "ノートの詳細・共有",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Content Preview with Minimap
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFFAFAFA), shape = MaterialTheme.shapes.small)
                        .padding(8.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = noteContent.ifEmpty { "(本文がありません)" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF0F0F0), shape = MaterialTheme.shapes.small)
                        .pointerInput(lines) {
                            detectTapGestures { tapOffset ->
                                if (lines.isNotEmpty()) {
                                    val ratio = (tapOffset.y / size.height).coerceIn(0f, 1f)
                                    val targetScroll = (scrollState.maxValue * ratio).toInt()
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(targetScroll)
                                    }
                                }
                            }
                        }
                        .padding(4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (lines.isNotEmpty()) {
                            val lineSpacing = size.height / lines.size.coerceAtLeast(1)
                            lines.forEachIndexed { index, line ->
                                if (line.isNotBlank()) {
                                    val y = index * lineSpacing
                                    val lineWidth = (size.width * (line.length.toFloat() / 50f).coerceIn(0.2f, 0.9f))
                                    drawLine(
                                        color = Color.DarkGray,
                                        start = Offset(4f, y + 2f),
                                        end = Offset(4f + lineWidth, y + 2f),
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Actions Group
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { onCopy(noteContent) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_edit), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("コピー")
                }
                
                Button(
                    onClick = { onPopOut() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_slideshow), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("小窓")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Firebase Cloud Sync
            ListItem(
                headlineContent = { Text("Firebase クラウド同期") },
                supportingContent = { Text("このノートをクラウドに保存して同期します") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_upload),
                        contentDescription = null,
                        tint = if (isShared) MaterialTheme.colorScheme.primary else Color.Gray
                    ) 
                },
                trailingContent = {
                    Switch(
                        checked = isShared,
                        onCheckedChange = { onToggleShare(it) }
                    )
                }
            )

            HorizontalDivider()

            // Symbolize Action
            ListItem(
                headlineContent = { Text("シンボルとして保存") },
                supportingContent = { Text("内容をシンボルリストに登録し、Concepts等で再利用可能にします") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_save), 
                        contentDescription = null 
                    ) 
                },
                modifier = Modifier.clickable { onSymbolize() }
            )

            // External Share
            ListItem(
                headlineContent = { Text("外部アプリで共有") },
                supportingContent = { Text("他のアプリにテキストを送信します") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_share), 
                        contentDescription = null 
                    ) 
                },
                modifier = Modifier.clickable { onShareExternally() }
            )
        }
    }
}

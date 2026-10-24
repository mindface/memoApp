package com.example.memoapp.ui.concept

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun ConceptLinkedItemDetailModal(
    title: String,
    content: String,
    onOpenFullEditor: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val lines = remember(content) { content.lines() }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                
                Button(onClick = onOpenFullEditor) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_directions),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("フルエディタで開く")
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 500.dp)
                    .padding(16.dp)
            ) {
                // Main Text Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = content.ifEmpty { "(内容がありません)" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Side Line Overview Minimap Bar with Tooltip
                com.example.memoapp.ui.components.ScrollbarWithTooltip(
                    lines = lines,
                    onJumpToLine = { index ->
                        if (lines.isNotEmpty()) {
                            val ratio = index.toFloat() / lines.size.coerceAtLeast(1)
                            val targetScroll = (scrollState.maxValue * ratio).toInt()
                            coroutineScope.launch {
                                scrollState.animateScrollTo(targetScroll)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

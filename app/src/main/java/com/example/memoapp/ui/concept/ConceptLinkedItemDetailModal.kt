package com.example.memoapp.ui.concept

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onContentChange: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var currentContent by remember(content) { mutableStateOf(content) }
    val lines = remember(currentContent) { currentContent.lines() }

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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                // Main Text Content with Checklist support
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                ) {
                    com.example.memoapp.ui.components.ChecklistTextView(
                        content = currentContent.ifEmpty { "(内容がありません)" },
                        onContentChange = { updated ->
                            currentContent = updated
                            onContentChange?.invoke(updated)
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Side Line Overview Minimap Bar with Tooltip
                com.example.memoapp.ui.components.ScrollbarWithJumpModal(
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

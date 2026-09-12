package com.example.memoapp.ui.concept

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.memoapp.model.CanvasElement
import com.example.memoapp.model.Symbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptDetailModal(
    selectedElement: CanvasElement,
    availableSymbols: List<Symbol>,
    onToggleShare: () -> Unit,
    onInsertSymbol: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                text = "詳細設定",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Sharing Section
            ListItem(
                headlineContent = { Text("Firebase クラウド同期") },
                supportingContent = { Text("オンにすると他のデバイスと共有されます") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_upload),
                        contentDescription = null,
                        tint = if (selectedElement.isShared) MaterialTheme.colorScheme.primary else Color.Gray
                    ) 
                },
                trailingContent = {
                    Switch(
                        checked = selectedElement.isShared,
                        onCheckedChange = { onToggleShare() }
                    )
                }
            )

            HorizontalDivider()

            if (selectedElement.type == "TEXT") {
                // Actions Group
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopyText(selectedElement.text) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painter = painterResource(id = android.R.drawable.ic_menu_edit), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("コピー")
                    }
                    OutlinedButton(
                        onClick = { onShareText(selectedElement.text) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painter = painterResource(id = android.R.drawable.ic_menu_share), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("外部共有")
                    }
                }

                Text(
                    text = "保存済みシンボルから挿入",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(availableSymbols) { symbol ->
                        ListItem(
                            headlineContent = { Text(symbol.title) },
                            supportingContent = { 
                                Text(
                                    text = symbol.content.take(50),
                                    maxLines = 1
                                ) 
                            },
                            modifier = Modifier.clickable { onInsertSymbol(symbol.content) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("この要素には詳細なテキスト設定はありません", color = Color.Gray)
                }
            }
        }
    }
}

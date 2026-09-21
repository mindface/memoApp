package com.example.memoapp.ui.concept

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memoapp.model.CanvasElement
import com.example.memoapp.model.Symbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptStyleModal(
    selectedElement: CanvasElement,
    availableSymbols: List<Symbol>,
    quickColors: List<Int>,
    onInsertSymbol: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit,
    onUpdateBodyColor: (Int) -> Unit,
    onPickBodyColor: () -> Unit,
    onUpdateStrokeColor: (Int) -> Unit,
    onPickStrokeColor: () -> Unit,
    onUpdateDrawStyle: (Int) -> Unit,
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
                text = "オブジェクトの見た目",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // 1. Draw Style Section (Shapes only)
            if (selectedElement.type == "RECTANGLE" || selectedElement.type == "CIRCLE") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "表示スタイル",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to "塗りつぶし+枠",
                            1 to "塗りつぶしのみ",
                            2 to "枠のみ"
                        ).forEach { (styleId, label) ->
                            FilterChip(
                                selected = selectedElement.drawStyle == styleId,
                                onClick = { onUpdateDrawStyle(styleId) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 2. Fill Color Section
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "塗りつぶしの色",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    quickColors.forEach { colorInt ->
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .border(
                                    width = if (selectedElement.color == colorInt) 2.dp else 0.dp,
                                    color = if (selectedElement.color == colorInt) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            color = Color(colorInt),
                            shape = CircleShape,
                            onClick = { onUpdateBodyColor(colorInt) }
                        ) {}
                    }
                    
                    IconButton(
                        onClick = onPickBodyColor,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_manage),
                            contentDescription = "Pick Fill Color",
                            tint = Color.DarkGray
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 3. Stroke Color Section (Non-text only)
            if (selectedElement.type != "TEXT") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "枠線の色",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        quickColors.forEach { colorInt ->
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(
                                        width = if (selectedElement.strokeColor == colorInt) 2.dp else 0.dp,
                                        color = if (selectedElement.strokeColor == colorInt) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                color = Color(colorInt),
                                shape = CircleShape,
                                onClick = { onUpdateStrokeColor(colorInt) }
                            ) {}
                        }
                        
                        IconButton(
                            onClick = onPickStrokeColor,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_manage),
                                contentDescription = "Pick Stroke Color",
                                tint = Color.DarkGray
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 4. Text Actions & Symbols
            if (selectedElement.type == "TEXT") {
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
            }
        }
    }
}

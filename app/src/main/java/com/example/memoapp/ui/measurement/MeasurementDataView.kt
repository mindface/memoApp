package com.example.memoapp.ui.measurement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.memoapp.model.CounterElement

@Composable
fun MeasurementDataView(
    elements: List<CounterElement>,
    onIncrement: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val totalCount = elements.sumOf { it.count }
    val maxCount = elements.maxOfOrNull { it.count } ?: 1

    if (elements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("カウント対象のアイテムがありません。\nCanvasタブから追加してください。", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Summary Dashboard Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "比較ダッシュボード概要",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "総カウント数", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "$totalCount", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text(text = "登録アイテム数", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${elements.size} 件", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 2. Item Comparison Cards with Progress Bars
            items(elements) { el ->
                val progress = if (maxCount > 0) el.count.toFloat() / maxCount else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = el.title, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Type: ${el.linkedItemType ?: "CUSTOM"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${el.count}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp))
                                Button(onClick = { onIncrement(el.id) }) {
                                    Text("+1")
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { onDelete(el.id) }) {
                                    Text("削除")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Proportional Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

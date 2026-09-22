package com.example.memoapp.ui.concept

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.memoapp.model.Concept
import com.example.memoapp.model.LogItem
import com.example.memoapp.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptItemPickerModal(
    availableNotes: List<Note>,
    availableLogItems: List<LogItem>,
    availableConcepts: List<Concept>,
    onItemSelected: (itemType: String, itemId: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White
    ) {
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("Notes", "Logs", "Concepts")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "リンクするアイテムを選択",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.heightIn(min = 300.dp, max = 500.dp)) {
                when (selectedTabIndex) {
                    0 -> ItemList(availableNotes.map { it.id to it.title }, "NOTE", onItemSelected)
                    1 -> ItemList(availableLogItems.map { it.id to it.title }, "LOG_ITEM", onItemSelected)
                    2 -> ItemList(availableConcepts.map { it.id to it.title }, "CONCEPT", onItemSelected)
                }
            }
        }
    }
}

@Composable
private fun ItemList(
    items: List<Pair<String, String>>,
    type: String,
    onSelected: (String, String, String) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("アイテムがありません", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items) { (id, title) ->
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text("ID: ${id.take(8)}...") },
                    modifier = Modifier.clickable { onSelected(type, id, title) }
                )
            }
        }
    }
}

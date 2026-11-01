package com.example.memoapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChecklistTextView(
    content: String,
    onContentChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lines = remember(content) { content.lines() }

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            val trimmed = line.trimStart()
            val isChecked = trimmed.startsWith("- [x]") || trimmed.startsWith("[x]") || trimmed.startsWith("- [X]") || trimmed.startsWith("[X]")
            val isUnchecked = trimmed.startsWith("- [ ]") || trimmed.startsWith("[ ]")

            if (isChecked || isUnchecked) {
                val taskText = trimmed
                    .removePrefix("- [x]").removePrefix("[x]")
                    .removePrefix("- [X]").removePrefix("[X]")
                    .removePrefix("- [ ]").removePrefix("[ ]")
                    .trim()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(enabled = onContentChange != null) {
                            if (onContentChange != null) {
                                val newChecked = !isChecked
                                val newPrefix = if (newChecked) "- [x] " else "- [ ] "
                                val updatedLines = lines.toMutableList()
                                updatedLines[index] = "$newPrefix$taskText"
                                onContentChange(updatedLines.joinToString("\n"))
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            if (onContentChange != null) {
                                val newPrefix = if (checked) "- [x] " else "- [ ] "
                                val updatedLines = lines.toMutableList()
                                updatedLines[index] = "$newPrefix$taskText"
                                onContentChange(updatedLines.joinToString("\n"))
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = taskText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp,
                        color = if (isChecked) Color.Gray else Color.Black,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                    )
                }
            } else {
                Text(
                    text = line.ifEmpty { " " },
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

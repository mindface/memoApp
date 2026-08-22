package com.example.memoapp

import android.content.Context
import com.example.memoapp.model.LogItem
import java.io.File
import java.util.UUID

/** Stores one LogItem per Markdown file in the app's private storage. */
class LogItemRepository(context: Context) {
    private val directory = File(context.filesDir, "logitems")

    fun create(title: String, content: String): LogItem {
        val now = System.currentTimeMillis().toString()
        val item = LogItem(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            content = content,
            createdAt = now,
            updatedAt = now
        )

        save(item)
        return item
    }

    fun getAll(): List<LogItem> {
        if (!directory.exists()) return emptyList()
        return directory.listFiles { file -> file.extension == "md" }
            ?.mapNotNull { file -> runCatching { parseMarkdown(file.readText()) }.getOrNull() }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    fun getById(id: String): LogItem? {
        val file = File(directory, "$id.md")
        if (!file.exists()) return null
        return runCatching { parseMarkdown(file.readText()) }.getOrNull()
    }

    fun save(item: LogItem) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("ログ保存フォルダを作成できませんでした")
        }
        File(directory, "${item.id}.md").writeText(item.toMarkdown(), Charsets.UTF_8)
    }

    private fun parseMarkdown(text: String): LogItem {
        val lines = text.lines()
        if (lines.firstOrNull() != "---") throw IllegalArgumentException("Invalid format")
        
        // indexOf(it) always returns the first delimiter (index 0), so search from line 1.
        val headerEnd = lines.drop(1).indexOf("---").let { index ->
            if (index < 0) -1 else index + 1
        }
        if (headerEnd == -1) throw IllegalArgumentException("Invalid header")

        val metadata = lines.subList(1, headerEnd).associate {
            val parts = it.split(": ", limit = 2)
            parts[0] to parts.getOrElse(1) { "" }
        }

        val id = metadata["id"] ?: ""
        val createdAt = metadata["createdAt"] ?: ""
        val updatedAt = metadata["updatedAt"] ?: ""

        val bodyLines = lines.subList(headerEnd + 1, lines.size)
        val titleLineIndex = bodyLines.indexOfFirst { it.startsWith("# ") }
        val title = if (titleLineIndex != -1) bodyLines[titleLineIndex].removePrefix("# ") else ""
        
        val contentLines = if (titleLineIndex != -1 && titleLineIndex + 1 < bodyLines.size) {
            val afterTitle = bodyLines.subList(titleLineIndex + 1, bodyLines.size)
            // Skip only the first empty line added by toMarkdown for better consistency
            if (afterTitle.firstOrNull()?.isBlank() == true) {
                afterTitle.drop(1)
            } else {
                afterTitle
            }
        } else {
            emptyList()
        }
        
        val content = contentLines.joinToString("\n")

        return LogItem(id, title, content, createdAt, updatedAt)
    }

    private fun LogItem.toMarkdown(): String = buildString {
        appendLine("---")
        appendLine("id: $id")
        appendLine("createdAt: $createdAt")
        appendLine("updatedAt: $updatedAt")
        appendLine("---")
        appendLine()
        appendLine("# ${title.ifBlank { "無題" }}")
        // Always add a separator line if content exists, and ensure we only add one
        if (content.isNotBlank()) {
            appendLine()
            append(content.trimStart('\n'))
            if (!content.endsWith("\n")) appendLine()
        }
    }
}

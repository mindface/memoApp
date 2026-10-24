package com.example.memoapp

import android.content.Context
import com.example.memoapp.model.Note
import com.google.gson.Gson
import java.io.File

// TODO: Note: Cloud connection (Firebase Firestore sync) requires separate cloud configuration tasks.
class NoteLocalRepository(context: Context) {
    private val directory = File(context.filesDir, "notes")
    private val gson = Gson()

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun saveLocal(note: Note) {
        val json = gson.toJson(note)
        File(directory, "${note.id}.json").writeText(json, Charsets.UTF_8)
    }

    fun loadLocal(noteId: String): Note? {
        val file = File(directory, "$noteId.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(Charsets.UTF_8), Note::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLocal(noteId: String) {
        val file = File(directory, "$noteId.json")
        if (file.exists()) {
            file.delete()
        }
    }

    fun getAllLocalData(): List<Note> {
        return directory.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(Charsets.UTF_8), Note::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
    }

    fun getLocalFilePath(noteId: String): String {
        return File(directory, "$noteId.json").absolutePath
    }
}

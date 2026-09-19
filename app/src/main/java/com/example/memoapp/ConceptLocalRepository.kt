package com.example.memoapp

import android.content.Context
import com.example.memoapp.model.CanvasElement
import com.example.memoapp.model.Concept
import com.google.gson.Gson
import java.io.File

data class LocalConceptData(
    val concept: Concept,
    val elements: List<CanvasElement>
)

class ConceptLocalRepository(context: Context) {
    private val directory = File(context.filesDir, "concepts")
    private val gson = Gson()

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun saveLocal(concept: Concept, elements: List<CanvasElement>) {
        val data = LocalConceptData(concept, elements)
        val json = gson.toJson(data)
        File(directory, "${concept.id}.json").writeText(json, Charsets.UTF_8)
    }

    fun loadLocal(conceptId: String): LocalConceptData? {
        val file = File(directory, "$conceptId.json")
        if (!file.exists()) return null
        return try {
            val json = file.readText(Charsets.UTF_8)
            gson.fromJson(json, LocalConceptData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLocal(conceptId: String) {
        val file = File(directory, "$conceptId.json")
        if (file.exists()) {
            file.delete()
        }
    }

    fun getAllLocalData(): List<LocalConceptData> {
        return directory.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file -> 
                try {
                    val json = file.readText(Charsets.UTF_8)
                    gson.fromJson(json, LocalConceptData::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
    }
}

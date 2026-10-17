package com.example.memoapp

import android.content.Context
import com.example.memoapp.model.CanvasElement
import com.example.memoapp.model.ReFormation
import com.google.gson.Gson
import java.io.File

data class LocalReFormationData(
    val reformation: ReFormation,
    val elements: List<CanvasElement>
)

class ReFormationLocalRepository(context: Context) {
    private val directory = File(context.filesDir, "reformations")
    private val gson = Gson()

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun saveLocal(reFormation: ReFormation, elements: List<CanvasElement>) {
        val data = LocalReFormationData(reFormation, elements)
        val json = gson.toJson(data)
        File(directory, "${reFormation.id}.json").writeText(json, Charsets.UTF_8)
    }

    fun loadLocal(reFormationId: String): LocalReFormationData? {
        val file = File(directory, "$reFormationId.json")
        if (!file.exists()) return null
        return try {
            val json = file.readText(Charsets.UTF_8)
            gson.fromJson(json, LocalReFormationData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLocal(reFormationId: String) {
        val file = File(directory, "$reFormationId.json")
        if (file.exists()) {
            file.delete()
        }
    }
}

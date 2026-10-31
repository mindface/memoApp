package com.example.memoapp

import android.content.Context
import com.example.memoapp.model.CounterElement
import com.example.memoapp.model.MeasurementPage
import com.google.gson.Gson
import java.io.File

data class LocalMeasurementData(
    val page: MeasurementPage,
    val elements: List<CounterElement>
)

// TODO: Note: Cloud connection (Firebase Firestore sync) requires separate cloud configuration tasks.
class MeasurementLocalRepository(context: Context) {
    private val directory = File(context.filesDir, "measurements")
    private val gson = Gson()

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun saveLocal(page: MeasurementPage, elements: List<CounterElement>) {
        val data = LocalMeasurementData(page, elements)
        val json = gson.toJson(data)
        File(directory, "${page.id}.json").writeText(json, Charsets.UTF_8)
    }

    fun loadLocal(pageId: String): LocalMeasurementData? {
        val file = File(directory, "$pageId.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(Charsets.UTF_8), LocalMeasurementData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLocal(pageId: String) {
        val file = File(directory, "$pageId.json")
        if (file.exists()) {
            file.delete()
        }
    }

    fun getAllLocalData(): List<LocalMeasurementData> {
        return directory.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(Charsets.UTF_8), LocalMeasurementData::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
    }

    fun getLocalFilePath(pageId: String): String {
        return File(directory, "$pageId.json").absolutePath
    }
}

package com.example.memoapp

import android.content.Context
import com.example.memoapp.model.Symbol
import com.google.gson.Gson
import java.io.File

// TODO: Note: Cloud connection (Firebase Firestore sync) requires separate cloud configuration tasks.
class SymbolLocalRepository(context: Context) {
    private val directory = File(context.filesDir, "symbols")
    private val gson = Gson()

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun saveLocal(symbol: Symbol) {
        val json = gson.toJson(symbol)
        File(directory, "${symbol.id}.json").writeText(json, Charsets.UTF_8)
    }

    fun loadLocal(symbolId: String): Symbol? {
        val file = File(directory, "$symbolId.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(Charsets.UTF_8), Symbol::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLocal(symbolId: String) {
        val file = File(directory, "$symbolId.json")
        if (file.exists()) {
            file.delete()
        }
    }

    fun getAllLocalData(): List<Symbol> {
        return directory.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(Charsets.UTF_8), Symbol::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
    }

    fun getLocalFilePath(symbolId: String): String {
        return File(directory, "$symbolId.json").absolutePath
    }
}

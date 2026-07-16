package com.meet.libraryinsight.cli

import com.meet.libraryinsight.model.LibraryApiIndex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object DatabaseHelper {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun saveIndex(index: LibraryApiIndex, file: File) {
        val jsonStr = json.encodeToString(index)
        file.writeText(jsonStr)
    }

    fun loadIndex(file: File): LibraryApiIndex? {
        if (!file.exists()) return null
        return try {
            val jsonStr = file.readText()
            json.decodeFromString<LibraryApiIndex>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }
}

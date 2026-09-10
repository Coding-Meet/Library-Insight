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

    private val memberIndexCacheDir: File
        get() {
            val userHome = System.getProperty("user.home")
            val base = if (File("build").exists() || File("settings.gradle").exists() || File("settings.gradle.kts").exists()) {
                File("build/library-insight/cache/indexes")
            } else {
                File(userHome, ".library-insight/cache/indexes")
            }
            base.mkdirs()
            return base
        }

    fun getCachedMemberIndex(coordinate: String): LibraryApiIndex? {
        val safeName = coordinate.replace(':', '_').replace('/', '_') + ".json"
        val file = File(memberIndexCacheDir, safeName)
        return loadIndex(file)
    }

    fun saveCachedMemberIndex(coordinate: String, index: LibraryApiIndex) {
        val safeName = coordinate.replace(':', '_').replace('/', '_') + ".json"
        val file = File(memberIndexCacheDir, safeName)
        saveIndex(index, file)
    }
}

package com.meet.libraryinsight.export

import com.meet.libraryinsight.model.LibraryApiIndex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonExporter {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Exports the LibraryApiIndex to a pretty-printed JSON string.
     */
    fun export(index: LibraryApiIndex): String {
        return json.encodeToString(index)
    }
}

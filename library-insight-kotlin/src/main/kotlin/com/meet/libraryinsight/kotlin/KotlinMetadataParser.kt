package com.meet.libraryinsight.kotlin

import com.meet.libraryinsight.parser.RawClassData
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import org.objectweb.asm.tree.AnnotationNode

object KotlinMetadataParser {

    /**
     * Attempts to parse Kotlin metadata from raw class data.
     */
    fun parseMetadata(rawClassData: RawClassData): KotlinClassMetadata? {
        val node = rawClassData.metadataAnnotation ?: return null
        return try {
            val kind = node.getValue("k") as? Int
            @Suppress("UNCHECKED_CAST")
            val metadataVersion = (node.getValue("mv") as? List<Int>)?.toIntArray()
            @Suppress("UNCHECKED_CAST")
            val data1 = (node.getValue("d1") as? List<String>)?.toTypedArray()
            @Suppress("UNCHECKED_CAST")
            val data2 = (node.getValue("d2") as? List<String>)?.toTypedArray()
            val extraString = node.getValue("xs") as? String
            val packageName = node.getValue("pn") as? String
            val extraInt = node.getValue("xi") as? Int

            val metadata = Metadata(
                kind = kind,
                metadataVersion = metadataVersion,
                data1 = data1,
                data2 = data2,
                extraString = extraString,
                packageName = packageName,
                extraInt = extraInt
            )

            KotlinClassMetadata.readLenient(metadata)
        } catch (e: Exception) {
            // Log or fallback if metadata is corrupted
            null
        }
    }

    private fun AnnotationNode.getValue(name: String): Any? {
        val list = values ?: return null
        for (i in 0 until list.size step 2) {
            if (list[i] == name) {
                return list[i + 1]
            }
        }
        return null
    }
}

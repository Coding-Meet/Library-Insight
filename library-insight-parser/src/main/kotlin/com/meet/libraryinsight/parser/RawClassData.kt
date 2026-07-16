package com.meet.libraryinsight.parser

import org.objectweb.asm.tree.AnnotationNode

data class RawClassData(
    val name: String, // Fully qualified name using dots (e.g. com.meet.Library)
    val internalName: String, // JVM internal name (e.g. com/meet/Library)
    val superName: String?,
    val interfaces: List<String>,
    val signature: String?,
    val access: Int,
    val annotations: List<RawAnnotation>,
    val fields: List<RawField>,
    val methods: List<RawMethod>,
    val innerClasses: List<RawInnerClass>,
    val metadataAnnotation: AnnotationNode? // Kotlin @Metadata annotation if present
)

data class RawAnnotation(
    val desc: String,
    val values: Map<String, Any?>
)

data class RawField(
    val name: String,
    val desc: String,
    val signature: String?,
    val access: Int,
    val value: Any?,
    val annotations: List<RawAnnotation>
)

data class RawMethod(
    val name: String,
    val desc: String,
    val signature: String?,
    val access: Int,
    val annotations: List<RawAnnotation>,
    val parameterAnnotations: List<List<RawAnnotation>>,
    val parameterNames: List<String>
)

data class RawInnerClass(
    val name: String,
    val outerName: String?,
    val innerName: String?,
    val access: Int
)

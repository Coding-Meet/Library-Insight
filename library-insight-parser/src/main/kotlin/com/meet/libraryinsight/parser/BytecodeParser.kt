package com.meet.libraryinsight.parser

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.io.InputStream

object BytecodeParser {

    /**
     * Parses a compiled .class file from bytes and extracts its raw JVM structures.
     */
    fun parseClass(classBytes: ByteArray): RawClassData {
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        // Skip code and debug tables we don't need, but keep parameters table to get parameter names if present
        classReader.accept(classNode, ClassReader.SKIP_CODE)

        val annotations = mergeAnnotations(classNode.visibleAnnotations, classNode.invisibleAnnotations)
        val fields = classNode.fields.map { fieldNode ->
            RawField(
                name = fieldNode.name,
                desc = fieldNode.desc,
                signature = fieldNode.signature,
                access = fieldNode.access,
                value = fieldNode.value,
                annotations = mergeAnnotations(fieldNode.visibleAnnotations, fieldNode.invisibleAnnotations)
            )
        }

        val methods = classNode.methods.map { methodNode ->
            // Try to extract parameter names from the method's parameters list (if present)
            val paramNames = methodNode.parameters?.mapNotNull { it.name } ?: emptyList()

            // Merge parameter annotations
            val paramAnnotations = mutableListOf<List<RawAnnotation>>()
            val visibleParamAnnos = methodNode.visibleParameterAnnotations
            val invisibleParamAnnos = methodNode.invisibleParameterAnnotations
            val numParams = Type.getArgumentTypes(methodNode.desc).size
            for (i in 0 until numParams) {
                val vis = visibleParamAnnos?.getOrNull(i)
                val invis = invisibleParamAnnos?.getOrNull(i)
                paramAnnotations.add(mergeAnnotations(vis, invis))
            }

            RawMethod(
                name = methodNode.name,
                desc = methodNode.desc,
                signature = methodNode.signature,
                access = methodNode.access,
                annotations = mergeAnnotations(methodNode.visibleAnnotations, methodNode.invisibleAnnotations),
                parameterAnnotations = paramAnnotations,
                parameterNames = paramNames
            )
        }

        val innerClasses = classNode.innerClasses.map { innerClassNode ->
            RawInnerClass(
                name = innerClassNode.name,
                outerName = innerClassNode.outerName,
                innerName = innerClassNode.innerName,
                access = innerClassNode.access
            )
        }

        // Find @Metadata annotation (invisible annotation "Lkotlin/Metadata;")
        val metadataAnnotation = classNode.invisibleAnnotations?.firstOrNull { it.desc == "Lkotlin/Metadata;" }
            ?: classNode.visibleAnnotations?.firstOrNull { it.desc == "Lkotlin/Metadata;" }

        return RawClassData(
            name = classNode.name.replace('/', '.'),
            internalName = classNode.name,
            superName = classNode.superName,
            interfaces = classNode.interfaces ?: emptyList(),
            signature = classNode.signature,
            access = classNode.access,
            annotations = annotations,
            fields = fields,
            methods = methods,
            innerClasses = innerClasses,
            metadataAnnotation = metadataAnnotation
        )
    }

    private fun mergeAnnotations(visible: List<AnnotationNode>?, invisible: List<AnnotationNode>?): List<RawAnnotation> {
        val merged = mutableListOf<RawAnnotation>()
        visible?.forEach { merged.add(parseAnnotation(it)) }
        invisible?.forEach { merged.add(parseAnnotation(it)) }
        return merged
    }

    private fun parseAnnotation(node: AnnotationNode): RawAnnotation {
        val values = mutableMapOf<String, Any?>()
        val list = node.values
        if (list != null) {
            for (i in 0 until list.size step 2) {
                val key = list[i] as String
                val value = list[i + 1]
                values[key] = parseAnnotationValue(value)
            }
        }
        return RawAnnotation(node.desc, values)
    }

    private fun parseAnnotationValue(value: Any?): Any? {
        return when (value) {
            is AnnotationNode -> parseAnnotation(value)
            is List<*> -> value.map { parseAnnotationValue(it) }
            is Array<*> -> value.map { parseAnnotationValue(it) }
            else -> value
        }
    }
}
// We import Type from org.objectweb.asm to calculate parameter types count
private typealias Type = org.objectweb.asm.Type

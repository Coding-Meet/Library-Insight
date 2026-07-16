package com.meet.libraryinsight.export

import com.meet.libraryinsight.model.ClassKind
import com.meet.libraryinsight.model.LibraryApiIndex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AiExporter {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Serializable
    data class AiContext(
        val library: String,
        val version: String,
        val classes: List<AiClass>
    )

    @Serializable
    data class AiClass(
        val name: String,
        val kind: String,
        val modifiers: List<String>,
        val superTypes: List<String>,
        val properties: List<AiProperty>,
        val constructors: List<String>,
        val methods: List<AiMethod>,
        val doc: String? = null
    )

    @Serializable
    data class AiProperty(
        val signature: String,
        val doc: String? = null
    )

    @Serializable
    data class AiMethod(
        val signature: String,
        val doc: String? = null
    )

    /**
     * Converts a detailed LibraryApiIndex into a highly compact, token-efficient AI context JSON.
     */
    fun export(index: LibraryApiIndex): String {
        val aiClasses = mutableListOf<AiClass>()

        for (pkg in index.packages) {
            for (clazz in pkg.classes) {
                val kindStr = when (clazz.kind) {
                    ClassKind.CLASS -> "class"
                    ClassKind.INTERFACE -> "interface"
                    ClassKind.ENUM -> "enum"
                    ClassKind.ANNOTATION -> "annotation"
                    ClassKind.OBJECT -> "object"
                    ClassKind.COMPANION_OBJECT -> "companion object"
                }

                val properties = clazz.properties.map { prop ->
                    val mutability = if (prop.isMutable) "var" else "val"
                    val visibility = prop.visibility.name.lowercase()
                    val constStr = if (prop.isConst) "const " else ""
                    val lateinitStr = if (prop.isLateinit) "lateinit " else ""
                    val sig = "$visibility $constStr$lateinitStr$mutability ${prop.name}: ${prop.type}"
                    AiProperty(sig, prop.doc)
                }

                val constructors = clazz.constructors.map { cons ->
                    val visibility = cons.visibility.name.lowercase()
                    val params = cons.parameters.joinToString { param ->
                        "${param.name}: ${param.type}${if (param.hasDefaultValue) " = ..." else ""}"
                    }
                    "$visibility constructor($params)"
                }

                val methods = clazz.methods.map { method ->
                    val visibility = method.visibility.name.lowercase()
                    val mods = mutableListOf<String>()
                    if (method.flags.isSuspend) mods.add("suspend")
                    if (method.flags.isInline) mods.add("inline")
                    if (method.flags.isOperator) mods.add("operator")
                    if (method.flags.isInfix) mods.add("infix")
                    if (method.flags.isStatic) mods.add("static")
                    
                    val modsStr = if (mods.isNotEmpty()) mods.joinToString(" ") + " " else ""
                    val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                    val params = method.parameters.joinToString { param ->
                        "${param.name}: ${param.type}${if (param.hasDefaultValue) " = ..." else ""}"
                    }
                    val sig = "$visibility ${modsStr}fun $receiver${method.name}($params): ${method.returnType}"
                    AiMethod(sig, method.doc)
                }

                aiClasses.add(
                    AiClass(
                        name = clazz.name,
                        kind = kindStr,
                        modifiers = clazz.modifiers,
                        superTypes = clazz.superTypes,
                        properties = properties,
                        constructors = constructors,
                        methods = methods,
                        doc = clazz.doc
                    )
                )
            }
        }

        val context = AiContext(
            library = index.libraryName,
            version = index.version,
            classes = aiClasses
        )

        return json.encodeToString(context)
    }
}

package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.common.MavenResolver
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.search.SearchEngine
import com.meet.libraryinsight.model.LibraryApiIndex
import kotlinx.serialization.json.*
import java.io.File

class McpCommand : CliktCommand(
    name = "mcp",
    help = "Start the Model Context Protocol (MCP) server listening on stdio."
) {
    private val dbFile by option(
        "--db",
        help = "Index database JSON file path to read from and write to"
    ).file().default(File("build/library-insight-index.json"))

    override fun run() {
        val scanner = java.util.Scanner(System.`in`)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine().trim()
            if (line.isEmpty()) continue
            try {
                val request = Json.parseToJsonElement(line).jsonObject
                val id = request["id"]
                val method = request["method"]?.jsonPrimitive?.content ?: ""

                when (method) {
                    "initialize" -> {
                        sendResponse(id, buildJsonObject {
                            put("protocolVersion", "2024-11-05")
                            putJsonObject("capabilities") {}
                            putJsonObject("serverInfo") {
                                put("name", "library-insight-mcp")
                                put("version", "1.0.0")
                            }
                        })
                    }
                    "tools/list" -> {
                        sendResponse(id, buildJsonObject {
                            putJsonArray("tools") {
                                addJsonObject {
                                    put("name", "scan_library")
                                    put("description", "Scans a Java/Kotlin library (local file/directory or Maven coordinate) and creates an API index.")
                                    putJsonObject("inputSchema") {
                                        put("type", "object")
                                        putJsonObject("properties") {
                                            putJsonObject("pathOrCoordinate") {
                                                put("type", "string")
                                                put("description", "Maven coordinate (groupId:artifactId:version) or path to a local JAR/AAR/directory")
                                            }
                                        }
                                        putJsonArray("required") { add("pathOrCoordinate") }
                                    }
                                }
                                addJsonObject {
                                    put("name", "search_symbols")
                                    put("description", "Search for packages, classes, methods, or properties in the active library index.")
                                    putJsonObject("inputSchema") {
                                        put("type", "object")
                                        putJsonObject("properties") {
                                            putJsonObject("query") {
                                                put("type", "string")
                                                put("description", "Symbol name or search keyword")
                                            }
                                        }
                                        putJsonArray("required") { add("query") }
                                    }
                                }
                                addJsonObject {
                                    put("name", "explain_class")
                                    put("description", "Print detailed structure, constructors, methods, and Javadocs of a specific class.")
                                    putJsonObject("inputSchema") {
                                        put("type", "object")
                                        putJsonObject("properties") {
                                            putJsonObject("className") {
                                                put("type", "string")
                                                put("description", "Fully qualified or simple name of the class (e.g. retrofit2.Retrofit)")
                                            }
                                        }
                                        putJsonArray("required") { add("className") }
                                    }
                                }
                            }
                        })
                    }
                    "tools/call" -> {
                        val params = request["params"]?.jsonObject
                        val toolName = params?.get("name")?.jsonPrimitive?.content ?: ""
                        val arguments = params?.get("arguments")?.jsonObject ?: buildJsonObject {}
                        handleToolCall(id, toolName, arguments)
                    }
                    else -> {
                        // Standard generic result for other MCP methods to avoid client exceptions
                        if (id != null) {
                            sendResponse(id, buildJsonObject {})
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently ignore or write to stderr
                System.err.println("MCP Error: ${e.message}")
            }
        }
    }

    private fun handleToolCall(id: JsonElement?, toolName: String, arguments: JsonObject) {
        val resultText = when (toolName) {
            "scan_library" -> {
                val pathOrCoordinate = arguments["pathOrCoordinate"]?.jsonPrimitive?.content ?: ""
                if (pathOrCoordinate.isEmpty()) {
                    "Error: Missing pathOrCoordinate argument."
                } else {
                    try {
                        val index = if (MavenResolver.isCoordinate(pathOrCoordinate)) {
                            val resolved = MavenResolver.resolve(pathOrCoordinate)
                            val parts = pathOrCoordinate.split(':')
                            LibraryAnalyzer.analyze(resolved.binaryFile, parts[1], parts[2], resolved.sourcesFile)
                        } else {
                            val file = File(pathOrCoordinate)
                            if (!file.exists()) {
                                "Error: path '$pathOrCoordinate' does not exist."
                            } else {
                                LibraryAnalyzer.analyze(file, file.nameWithoutExtension, "1.0.0", null)
                            }
                        }
                        if (index is LibraryApiIndex) {
                            DatabaseHelper.saveIndex(index, dbFile)
                            val classesCount = index.packages.flatMap { it.classes }.size
                            "SUCCESS: Scanned library $pathOrCoordinate. Found $classesCount classes across ${index.packages.size} packages. Index saved to ${dbFile.path}."
                        } else {
                            "Error: Failed to analyze library structure."
                        }
                    } catch (e: Exception) {
                        "Error analyzing library: ${e.message}"
                    }
                }
            }
            "search_symbols" -> {
                val query = arguments["query"]?.jsonPrimitive?.content ?: ""
                val index = DatabaseHelper.loadIndex(dbFile)
                if (index == null) {
                    "Error: No library index database found. Please call scan_library tool first."
                } else {
                    val results = SearchEngine.search(index, query)
                    if (results.isEmpty()) {
                        "No matching symbols found for '$query'."
                    } else {
                        buildString {
                            appendLine("Found ${results.size} matches for '$query':")
                            appendLine("--------------------------------------------------")
                            for (res in results) {
                                val typePrefix = "[${res.type.name}]".padEnd(12)
                                appendLine("$typePrefix ${res.details}")
                            }
                            appendLine("--------------------------------------------------")
                        }
                    }
                }
            }
            "explain_class" -> {
                val className = arguments["className"]?.jsonPrimitive?.content ?: ""
                val index = DatabaseHelper.loadIndex(dbFile)
                if (index == null) {
                    "Error: No library index database found. Please call scan_library tool first."
                } else {
                    val allClasses = index.packages.flatMap { it.classes }
                    val clazz = allClasses.firstOrNull { it.name == className || it.simpleName == className }
                    if (clazz == null) {
                        "Error: Class '$className' not found in the index."
                    } else {
                        val pkgName = index.packages.first { it.classes.contains(clazz) }.name
                        buildString {
                            appendLine("==================================================")
                            appendLine(" CLASS EXPLAIN REPORT")
                            appendLine("==================================================")
                            appendLine("Class:       ${clazz.name}")
                            appendLine("Package:     $pkgName")
                            appendLine("Kind:        ${clazz.kind.name.lowercase()}")
                            appendLine("Visibility:  ${clazz.visibility.name.lowercase()}")
                            if (clazz.modifiers.isNotEmpty()) {
                                appendLine("Modifiers:   ${clazz.modifiers.joinToString(", ")}")
                            }
                            if (clazz.superTypes.isNotEmpty()) {
                                appendLine("Supertypes:  ${clazz.superTypes.joinToString(", ")}")
                            }
                            val classDoc = clazz.doc
                            if (classDoc != null) {
                                appendLine("--------------------------------------------------")
                                appendLine("Documentation:\n${classDoc.trim().lines().joinToString("\n") { "  * $it" }}")
                            }
                            appendLine("==================================================")
                            if (clazz.constructors.isNotEmpty()) {
                                appendLine("\nConstructors:")
                                for (cons in clazz.constructors) {
                                    val params = cons.parameters.joinToString { "${it.name}: ${it.type}" }
                                    appendLine("  - ${cons.visibility.name.lowercase()} constructor($params)")
                                }
                            }
                            if (clazz.methods.isNotEmpty()) {
                                appendLine("\nMethods:")
                                for (method in clazz.methods) {
                                    val params = method.parameters.joinToString { "${it.name}: ${it.type}" }
                                    val suspendMarker = if (method.flags.isSuspend) "suspend " else ""
                                    appendLine("  - $suspendMarker${method.visibility.name.lowercase()} fun ${method.name}($params): ${method.returnType}")
                                }
                            }
                            if (clazz.properties.isNotEmpty()) {
                                appendLine("\nProperties:")
                                for (prop in clazz.properties) {
                                    val mutMarker = if (prop.isMutable) "var" else "val"
                                    appendLine("  - ${prop.visibility.name.lowercase()} $mutMarker ${prop.name}: ${prop.type}")
                                }
                            }
                        }
                    }
                }
            }
            else -> "Error: Unknown tool name '$toolName'."
        }

        sendResponse(id, buildJsonObject {
            putJsonArray("content") {
                addJsonObject {
                    put("type", "text")
                    put("text", resultText)
                }
            }
        })
    }

    private fun sendResponse(id: JsonElement?, result: JsonObject) {
        val response = buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) {
                put("id", id)
            }
            put("result", result)
        }
        println(response.toString())
    }
}

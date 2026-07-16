package com.meet.libraryinsight.core

object SourcesParser {

    data class ParsedSourceDetails(
        val classes: Map<String, ClassSourceInfo>, // Maps simple class name to ClassSourceInfo
        val classSource: String?,
        val methods: Map<String, List<MethodSourceInfo>>, // Maps method name to list of overloads
        val properties: Map<String, PropertySourceInfo>
    )

    data class ClassSourceInfo(
        val doc: String?
    )

    data class MethodSourceInfo(
        val doc: String?,
        val sourceCode: String?,
        val paramCount: Int
    )

    data class PropertySourceInfo(
        val doc: String?
    )

    /**
     * Parses a Java or Kotlin source code text and extracts comments (Javadoc/KDoc)
     * and method source code bodies.
     */
    fun parse(sourceText: String): ParsedSourceDetails {
        val classes = mutableMapOf<String, ClassSourceInfo>()
        val methods = mutableMapOf<String, MutableList<MethodSourceInfo>>()
        val properties = mutableMapOf<String, PropertySourceInfo>()

        // 1. Clean up comments and associate them with declarations
        val lines = sourceText.lines()
        var currentComment: StringBuilder? = null
        var inComment = false

        var lineIdx = 0
        while (lineIdx < lines.size) {
            val line = lines[lineIdx].trim()

            if (line.startsWith("/**")) {
                inComment = true
                currentComment = StringBuilder()
                val inlineContent = line.removePrefix("/**").removeSuffix("*/").trim()
                if (inlineContent.isNotEmpty()) {
                    currentComment.append(inlineContent)
                }
                if (line.endsWith("*/")) {
                    inComment = false
                }
                lineIdx++
                continue
            }

            if (inComment) {
                if (line.endsWith("*/")) {
                    inComment = false
                    val cleanLine = line.removeSuffix("*/").trim().removePrefix("*").trim()
                    if (cleanLine.isNotEmpty()) {
                        currentComment?.append("\n")?.append(cleanLine)
                    }
                } else {
                    val cleanLine = line.trim().removePrefix("*").trim()
                    currentComment?.append("\n")?.append(cleanLine)
                }
                lineIdx++
                continue
            }

            if (line.isEmpty() || line.startsWith("//") || line.startsWith("@")) {
                // Skip empty lines, single line comments, or annotations when searching for declarations
                lineIdx++
                continue
            }

            // We found a non-comment line. If we have a comment buffer, process the declaration
            val doc = currentComment?.toString()?.trim()
            currentComment = null

            // Detect declaration type
            when {
                // Class declaration
                line.contains("class ") || line.contains("interface ") || line.contains("object ") || line.contains("enum ") -> {
                    val className = parseClassName(line)
                    if (className != null && doc != null) {
                        classes[className] = ClassSourceInfo(doc)
                    }
                }

                // Method declaration: contains "fun " (Kotlin) or looks like a Java method (contains '(' and ')')
                (line.contains("fun ") || (line.contains("(") && line.contains(")") && !line.contains("="))) && !line.contains("val ") && !line.contains("var ") -> {
                    val methodDetails = parseMethodDeclaration(lines, lineIdx, doc)
                    if (methodDetails != null) {
                        val (name, info) = methodDetails
                        methods.getOrPut(name) { mutableListOf() }.add(info)
                    }
                }

                // Property declaration: Kotlin val/var or Java field
                line.contains("val ") || line.contains("var ") || (line.endsWith(";") && !line.contains("(")) -> {
                    val propName = parsePropertyDeclaration(line)
                    if (propName != null && doc != null) {
                        properties[propName] = PropertySourceInfo(doc)
                    }
                }
            }

            lineIdx++
        }

        return ParsedSourceDetails(
            classes = classes,
            classSource = sourceText, // Store full class source
            methods = methods,
            properties = properties
        )
    }

    private fun parseClassName(line: String): String? {
        val cleaned = line.replace("sealed", "").replace("data", "").replace("value", "").replace("inner", "").replace("open", "").replace("abstract", "").replace("private", "").replace("public", "").replace("protected", "").replace("internal", "").trim()
        val tokens = cleaned.split(Regex("\\s+"))
        val keywordIdx = tokens.indexOfFirst { it == "class" || it == "interface" || it == "object" || it == "enum" }
        if (keywordIdx != -1 && keywordIdx + 1 < tokens.size) {
            val name = tokens[keywordIdx + 1].substringBefore(':').substringBefore('{').trim()
            return if (name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' }) name else null
        }
        return null
    }

    private fun parseMethodDeclaration(lines: List<String>, startLineIdx: Int, doc: String?): Pair<String, MethodSourceInfo>? {
        // Construct the full declaration signature (handles multi-line signatures)
        val sb = StringBuilder()
        var currentLineIdx = startLineIdx
        var foundBrace = false
        var openParenCount = 0
        var closedParenCount = 0

        while (currentLineIdx < lines.size) {
            val l = lines[currentLineIdx]
            sb.append(l).append("\n")
            
            openParenCount += l.count { it == '(' }
            closedParenCount += l.count { it == ')' }
            
            if (l.contains("{") || l.contains("=") || (openParenCount > 0 && openParenCount == closedParenCount)) {
                foundBrace = l.contains("{")
                break
            }
            currentLineIdx++
        }

        val declaration = sb.toString().trim()
        val parenIdx = declaration.indexOf('(')
        if (parenIdx == -1) return null

        // Extract method name: token before '('
        val beforeParen = declaration.substring(0, parenIdx).trim()
        val nameTokens = beforeParen.split(Regex("\\s+"))
        var methodName = nameTokens.last()
        if (methodName == "fun" && nameTokens.size > 1) {
            // Kotlin extension functions e.g. "fun String.shout" -> name is "shout"
            methodName = nameTokens.last().substringAfterLast('.')
        } else if (methodName.contains('.')) {
            // Handles cases like "fun List<T>.foo"
            methodName = methodName.substringAfterLast('.')
        }

        // Count parameter count based on commas inside parenthesized part
        val closingParenIdx = declaration.indexOf(')', parenIdx)
        val paramsPart = if (closingParenIdx != -1) declaration.substring(parenIdx + 1, closingParenIdx).trim() else ""
        val paramCount = when {
            paramsPart.isEmpty() -> 0
            else -> paramsPart.split(',').size
        }

        // Try to extract body using bracket matching if '{' was found
        var sourceCode: String? = null
        if (foundBrace) {
            val bodyBuilder = StringBuilder()
            var bracketCount = 0
            var bodyStarted = false
            var fileIdx = startLineIdx

            while (fileIdx < lines.size) {
                val l = lines[fileIdx]
                bodyBuilder.append(l).append("\n")
                
                for (char in l) {
                    if (char == '{') {
                        bracketCount++
                        bodyStarted = true
                    } else if (char == '}') {
                        bracketCount--
                    }
                }
                
                if (bodyStarted && bracketCount == 0) {
                    break
                }
                fileIdx++
            }
            sourceCode = bodyBuilder.toString().trim()
        } else {
            // Expression body e.g. "fun foo() = 42" or Java abstract method ending in ";"
            sourceCode = declaration
        }

        return methodName to MethodSourceInfo(doc = doc, sourceCode = sourceCode, paramCount = paramCount)
    }

    private fun parsePropertyDeclaration(line: String): String? {
        // Kotlin property: "val name: String" or "var count = 0"
        if (line.contains("val ") || line.contains("var ")) {
            val cleaned = line.replace("const", "").replace("lateinit", "").replace("private", "").replace("public", "").replace("protected", "").replace("internal", "").trim()
            val token = if (cleaned.startsWith("val ")) "val " else "var "
            val afterToken = cleaned.substringAfter(token).trim()
            val name = afterToken.substringBefore(':').substringBefore('=').trim()
            return if (name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' }) name else null
        }
        
        // Java field: "private String name;"
        if (line.endsWith(";")) {
            val cleaned = line.removeSuffix(";").trim()
            val spaceIdx = cleaned.lastIndexOf(' ')
            if (spaceIdx != -1) {
                val name = cleaned.substring(spaceIdx + 1).trim()
                return if (name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' }) name else null
            }
        }

        return null
    }
}

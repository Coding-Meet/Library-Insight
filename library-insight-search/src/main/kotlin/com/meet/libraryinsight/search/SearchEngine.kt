package com.meet.libraryinsight.search

import com.meet.libraryinsight.model.*

object SearchEngine {

    enum class MatchType {
        PACKAGE, CLASS, METHOD, PROPERTY, TYPE_ALIAS
    }

    data class SearchResult(
        val type: MatchType,
        val packageName: String,
        val className: String? = null,
        val name: String,
        val details: String // Modifiers, signature, etc.
    )

    /**
     * Searches a library index for matches matching the given query string (case-insensitive).
     * Supports annotation search (@AnnotationName), lambda/signature search ((A, B) -> C),
     * suspend modifiers, and generic boundary searches.
     */
    fun search(index: LibraryApiIndex, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val lowercaseQuery = query.lowercase().trim()

        val isAnnotationQuery = lowercaseQuery.startsWith("@") || lowercaseQuery.startsWith("anno:")
        val targetAnnotation = when {
            lowercaseQuery.startsWith("@") -> lowercaseQuery.removePrefix("@")
            lowercaseQuery.startsWith("anno:") -> lowercaseQuery.removePrefix("anno:")
            else -> ""
        }

        val isSignatureQuery = lowercaseQuery.contains("->") || lowercaseQuery.startsWith("(") || lowercaseQuery.endsWith(")")
        val isSuspendQuery = lowercaseQuery == "suspend"
        val isGenericQuery = lowercaseQuery.contains("<") || lowercaseQuery.contains(">")

        for (pkg in index.packages) {
            // 1. Check package name (only for plain queries)
            if (!isAnnotationQuery && !isSignatureQuery && !isSuspendQuery && !isGenericQuery) {
                if (pkg.name.lowercase().contains(lowercaseQuery)) {
                    results.add(
                        SearchResult(
                            type = MatchType.PACKAGE,
                            packageName = pkg.name,
                            name = pkg.name,
                            details = "package ${pkg.name}"
                        )
                    )
                }
            }

            // 2. Check type aliases
            for (alias in pkg.typeAliases) {
                val matches = when {
                    isAnnotationQuery -> alias.annotations.any { it.name.lowercase().contains(targetAnnotation) }
                    isSignatureQuery -> alias.expandedType.lowercase().contains(lowercaseQuery)
                    isGenericQuery -> alias.typeParameters.isNotEmpty() && alias.typeParameters.any { it.name.lowercase().contains(lowercaseQuery) }
                    else -> alias.name.lowercase().contains(lowercaseQuery) || alias.expandedType.lowercase().contains(lowercaseQuery)
                }

                if (matches) {
                    results.add(
                        SearchResult(
                            type = MatchType.TYPE_ALIAS,
                            packageName = pkg.name,
                            name = alias.name,
                            details = "typealias ${alias.name} = ${alias.expandedType}"
                        )
                    )
                }
            }

            for (clazz in pkg.classes) {
                // 3. Check class match
                val classMatches = when {
                    isAnnotationQuery -> clazz.annotations.any { it.name.lowercase().contains(targetAnnotation) } ||
                                         clazz.dslMarkerAnnotations.any { it.lowercase().contains(targetAnnotation) }
                    isSignatureQuery -> clazz.superTypes.any { it.lowercase().contains(lowercaseQuery) }
                    isSuspendQuery -> false
                    isGenericQuery -> clazz.typeParameters.isNotEmpty() && clazz.typeParameters.any { it.name.lowercase().contains(lowercaseQuery.replace("<", "").replace(">", "")) }
                    else -> clazz.name.lowercase().contains(lowercaseQuery) || clazz.simpleName.lowercase().contains(lowercaseQuery)
                }

                if (classMatches) {
                    val modStr = clazz.modifiers.joinToString(" ")
                    val kindStr = clazz.kind.name.lowercase()
                    val dslMarkerBadge = if (clazz.dslMarkerAnnotations.isNotEmpty()) " [DSL: ${clazz.dslMarkerAnnotations.joinToString { "@$it" }}]" else ""
                    results.add(
                        SearchResult(
                            type = MatchType.CLASS,
                            packageName = pkg.name,
                            className = clazz.name,
                            name = clazz.simpleName,
                            details = "$modStr $kindStr ${clazz.name}$dslMarkerBadge"
                        )
                    )
                }

                // 4. Check methods
                for (method in clazz.methods) {
                    val methodSigStr = buildString {
                        val receiver = method.extensionReceiverType
                        if (receiver != null) {
                            append(receiver.lowercase())
                            append(".")
                        }
                        append("(")
                        append(method.parameters.joinToString { it.type.lowercase() })
                        append(") -> ")
                        append(method.returnType.lowercase())
                    }

                    val methodMatches = when {
                        isAnnotationQuery -> method.annotations.any { it.name.lowercase().contains(targetAnnotation) }
                        isSignatureQuery -> methodSigStr.contains(lowercaseQuery)
                        isSuspendQuery -> method.flags.isSuspend
                        isGenericQuery -> method.typeParameters.isNotEmpty() && method.typeParameters.any { it.name.lowercase().contains(lowercaseQuery.replace("<", "").replace(">", "")) }
                        else -> method.name.lowercase().contains(lowercaseQuery)
                    }

                    if (methodMatches) {
                        val modStr = buildString {
                            if (method.flags.isSuspend) append("suspend ")
                            if (method.flags.isInline) append("inline ")
                            if (method.flags.isStatic) append("static ")
                        }
                        val receiverPrefix = method.extensionReceiverType?.let { "$it." } ?: ""
                        results.add(
                            SearchResult(
                                type = MatchType.METHOD,
                                packageName = pkg.name,
                                className = clazz.name,
                                name = method.name,
                                details = "${method.visibility.name.lowercase()} ${modStr}fun $receiverPrefix${method.name}(${method.parameters.joinToString { it.type }}): ${method.returnType}"
                            )
                        )
                    }
                }

                // 5. Check properties
                for (prop in clazz.properties) {
                    val propMatches = when {
                        isAnnotationQuery -> prop.annotations.any { it.name.lowercase().contains(targetAnnotation) }
                        isSignatureQuery -> prop.type.lowercase().contains(lowercaseQuery)
                        isSuspendQuery -> false
                        isGenericQuery -> false
                        else -> prop.name.lowercase().contains(lowercaseQuery)
                    }

                    if (propMatches) {
                        val mutableStr = if (prop.isMutable) "var" else "val"
                        results.add(
                            SearchResult(
                                type = MatchType.PROPERTY,
                                packageName = pkg.name,
                                className = clazz.name,
                                name = prop.name,
                                details = "${prop.visibility.name.lowercase()} $mutableStr ${prop.name}: ${prop.type}"
                            )
                        )
                    }
                }
            }
        }

        return results
    }
}

package com.meet.libraryinsight.search

import com.meet.libraryinsight.model.*

object SearchEngine {

    enum class MatchType {
        PACKAGE, CLASS, METHOD, PROPERTY
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
     */
    fun search(index: LibraryApiIndex, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val lowercaseQuery = query.lowercase()

        for (pkg in index.packages) {
            // Check package name
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

            for (clazz in pkg.classes) {
                // Check class name
                val classMatches = clazz.name.lowercase().contains(lowercaseQuery) || clazz.simpleName.lowercase().contains(lowercaseQuery)
                if (classMatches) {
                    val modStr = clazz.modifiers.joinToString(" ")
                    val kindStr = clazz.kind.name.lowercase()
                    results.add(
                        SearchResult(
                            type = MatchType.CLASS,
                            packageName = pkg.name,
                            className = clazz.name,
                            name = clazz.simpleName,
                            details = "$modStr $kindStr ${clazz.name}"
                        )
                    )
                }

                // Check methods
                for (method in clazz.methods) {
                    if (method.name.lowercase().contains(lowercaseQuery)) {
                        val modStr = if (method.flags.isStatic) "static " else ""
                        results.add(
                            SearchResult(
                                type = MatchType.METHOD,
                                packageName = pkg.name,
                                className = clazz.name,
                                name = method.name,
                                details = "${method.visibility.name.lowercase()} ${modStr}fun ${method.name}(${method.parameters.joinToString { it.type }}): ${method.returnType}"
                            )
                        )
                    }
                }

                // Check properties
                for (prop in clazz.properties) {
                    if (prop.name.lowercase().contains(lowercaseQuery)) {
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

package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.common.MavenResolver
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.jar.JarFile

class CallGraphCommand : CliktCommand(
    name = "callgraph",
    help = "Generate a visual call graph tree showing internal method invocations for a target method."
) {
    val methodTarget by argument(
        help = "Target method to analyze in format 'ClassName.methodName' (e.g. Calculator.plus)"
    )

    val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File("build/library-insight-index.json"))

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        // 1. Locate the JAR/AAR file associated with this library index
        val targetJar = findJarFile(index.libraryName, index.version)
        if (targetJar == null) {
            echo("Error: Could not locate binary JAR file for cached library ${index.libraryName}.", err = true)
            return
        }

        echo("Analyzing binary JAR: ${targetJar.absolutePath}")

        // 2. Parse all classes from the JAR into classNodes Map
        val classNodes = mutableMapOf<String, ClassNode>()
        try {
            JarFile(targetJar).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".class")) {
                        jar.getInputStream(entry).use { stream ->
                            val classNode = ClassNode()
                            ClassReader(stream.readBytes()).accept(classNode, 0)
                            classNodes[classNode.name] = classNode
                        }
                    }
                }
            }
        } catch (e: Exception) {
            echo("Error reading JAR file: ${e.message}", err = true)
            return
        }

        // 3. Resolve the target class and method name from argument
        val dotIndex = methodTarget.lastIndexOf('.')
        if (dotIndex == -1 || dotIndex == 0 || dotIndex == methodTarget.length - 1) {
            echo("Error: Target method must be in 'ClassName.methodName' format.", err = true)
            return
        }

        val targetClassName = methodTarget.substring(0, dotIndex)
        val targetMethodName = methodTarget.substring(dotIndex + 1)

        // Find the matched ClassNode (supports FQCN or simple class name suffix match)
        val matchedClassNode = classNodes.values.firstOrNull { node ->
            val cleanName = node.name.replace('/', '.').replace('$', '.')
            cleanName == targetClassName || cleanName.endsWith(".$targetClassName")
        }

        if (matchedClassNode == null) {
            echo("Error: Class '$targetClassName' not found in JAR classes.", err = true)
            return
        }

        // Find matched MethodNode(s) by name
        val matchedMethods = matchedClassNode.methods.filter { it.name == targetMethodName }
        if (matchedMethods.isEmpty()) {
            echo("Error: Method '$targetMethodName' not found in class ${matchedClassNode.name.replace('/', '.')}.", err = true)
            return
        }

        // Print Call Graph header
        val classDisplay = matchedClassNode.name.replace('/', '.')
        echo("==================================================")
        echo("  METHOD INVOCATION CALL GRAPH  —  $targetMethodName")
        echo("==================================================")

        // 4. Trace and render call graphs for matching methods
        val visited = mutableSetOf<String>()
        for (methodNode in matchedMethods) {
            val descriptor = methodNode.desc
            echo("\n▶ Starting entrypoint: $classDisplay.${methodNode.name}$descriptor")
            traceCallGraph(
                classNodes = classNodes,
                className = matchedClassNode.name,
                methodNode = methodNode,
                visited = visited,
                depth = 0,
                prefix = ""
            )
        }
        echo("==================================================")
    }

    private fun traceCallGraph(
        classNodes: Map<String, ClassNode>,
        className: String,
        methodNode: MethodNode,
        visited: MutableSet<String>,
        depth: Int,
        prefix: String
    ) {
        if (depth >= 4) return // Cap tracing at 4 levels deep to keep CLI output clean and readable

        val sigKey = "$className.${methodNode.name}${methodNode.desc}"
        if (sigKey in visited) {
            echo("$prefix└── (cycle) ${formatMethodName(className, methodNode.name, methodNode.desc)}")
            return
        }
        visited.add(sigKey)

        // Scan instructions for method invocations
        val calls = mutableListOf<CallTarget>()
        methodNode.instructions?.iterator()?.forEachRemaining { insn ->
            if (insn is MethodInsnNode) {
                // We only trace internal calls (where the target class is defined within our scanned JAR)
                if (insn.owner in classNodes) {
                    calls.add(CallTarget(owner = insn.owner, name = insn.name, desc = insn.desc))
                }
            }
        }

        val distinctCalls = calls.distinctBy { "${it.owner}.${it.name}${it.desc}" }
        distinctCalls.forEachIndexed { index, call ->
            val isLast = index == distinctCalls.size - 1
            val indent = if (isLast) "└── " else "├── "
            val childPrefix = prefix + if (isLast) "    " else "│   "

            val targetClass = classNodes[call.owner]
            val targetMethod = targetClass?.methods?.firstOrNull { it.name == call.name && it.desc == call.desc }

            echo("$prefix$indent${formatMethodName(call.owner, call.name, call.desc)}")

            if (targetMethod != null) {
                traceCallGraph(
                    classNodes = classNodes,
                    className = call.owner,
                    methodNode = targetMethod,
                    visited = visited,
                    depth = depth + 1,
                    prefix = childPrefix
                )
            }
        }

        visited.remove(sigKey)
    }

    private data class CallTarget(
        val owner: String,
        val name: String,
        val desc: String
    )

    private fun formatMethodName(owner: String, name: String, desc: String): String {
        val cleanClass = owner.replace('/', '.')
        // Format descriptor parameters into clean types
        val params = desc.substringAfter('(').substringBefore(')')
        return "$cleanClass.$name()"
    }

    private fun findJarFile(libraryName: String, version: String): File? {
        val cleanName = if (libraryName.contains(':')) {
            val parts = libraryName.split(':')
            parts[1]
        } else {
            libraryName
        }

        // 1. Try searching MavenResolver cache directory first
        val cached = MavenResolver.cacheDir.walkBottomUp()
            .filter { it.isFile && it.extension == "jar" && !it.name.contains("-sources") && !it.name.contains("-javadoc") }
            .find { it.name == "$cleanName-$version.jar" || it.name == "$cleanName.jar" || it.name.contains(cleanName) }

        if (cached != null) return cached

        // 2. Try searching the local Gradle cache modules folder
        val userHome = System.getProperty("user.home")
        if (userHome != null) {
            val gradleCache = File(userHome, ".gradle/caches/modules-2/files-2.1")
            if (gradleCache.exists()) {
                val resolved = gradleCache.walkTopDown()
                    .maxDepth(6)
                    .filter { it.isFile && it.extension == "jar" && !it.name.contains("-sources") && !it.name.contains("-javadoc") }
                    .find { it.name == "$cleanName-$version.jar" || it.name == "$cleanName.jar" }
                if (resolved != null) return resolved

                // Loose match fallback inside Gradle cache
                val looseResolved = gradleCache.walkTopDown()
                    .maxDepth(6)
                    .filter { it.isFile && it.extension == "jar" && !it.name.contains("-sources") && !it.name.contains("-javadoc") }
                    .find { it.name.contains(cleanName) }
                if (looseResolved != null) return looseResolved
            }
        }

        // 3. Slower fallback: search local project structure for target output jar files
        return File(".").walkTopDown()
            .filter { it.isFile && it.extension == "jar" && !it.name.contains("-sources") && !it.name.contains("-javadoc") }
            .find { it.name == "$cleanName-$version.jar" || it.name == "$cleanName.jar" || it.name.contains(cleanName) }
    }
}

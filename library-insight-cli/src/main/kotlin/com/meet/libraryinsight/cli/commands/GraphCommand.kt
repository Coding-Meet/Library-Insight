package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class GraphCommand : CliktCommand(
    name = "graph",
    help = "Download and print a visual dependency graph of a coordinate."
) {
    val coordinate by argument(help = "Maven coordinate (group:artifact:version)")

    override fun run() {
        val parts = coordinate.split(':')
        if (parts.size != 3) {
            echo("Error: Coordinate must be in group:artifact:version format.", err = true)
            return
        }

        echo("Resolving dependency tree for $coordinate...")
        val client = HttpClient(Java) {
            followRedirects = true
        }

        try {
            val visited = mutableSetOf<String>()
            printDependencyNode(client, coordinate, 0, visited)
        } catch (e: Exception) {
            echo("Error generating dependency graph: ${e.message}", err = true)
        } finally {
            client.close()
        }
    }

    private fun printDependencyNode(client: HttpClient, coord: String, indent: Int, visited: MutableSet<String>) {
        val prefix = "│   ".repeat(indent) + if (indent > 0) "├── " else ""
        
        if (coord in visited) {
            echo("$prefix$coord (cycle)")
            return
        }
        visited.add(coord)

        echo("$prefix$coord")

        // Max depth check to avoid massive token usage or hanging
        if (indent >= 3) return

        val parts = coord.split(':')
        if (parts.size != 3) return

        val groupId = parts[0]
        val artifactId = parts[1]
        val version = parts[2]

        val groupPath = groupId.replace('.', '/')
        val pomUrl = "https://repo1.maven.org/maven2/$groupPath/$artifactId/$version/$artifactId-$version.pom"

        try {
            val pomContent = runBlocking {
                client.get(pomUrl).bodyAsText()
            }
            val dependencies = parseDependenciesFromPom(pomContent)
            for (dep in dependencies) {
                printDependencyNode(client, dep, indent + 1, visited)
            }
        } catch (e: Exception) {
            // Ignore resolution errors for optional/non-existent transitive dependencies
        }
    }

    private fun parseDependenciesFromPom(pomXml: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val xmlInput = ByteArrayInputStream(pomXml.toByteArray(Charsets.UTF_8))
            val doc = dBuilder.parse(xmlInput)
            doc.documentElement.normalize()

            val dependencyNodes = doc.getElementsByTagName("dependency")
            for (i in 0 until dependencyNodes.length) {
                val node = dependencyNodes.item(i)
                if (node is Element) {
                    val scope = node.getElementsByTagName("scope").item(0)?.textContent ?: "compile"
                    // Only traverse compile dependencies to keep graph simple and relevant
                    if (scope == "compile") {
                        val g = node.getElementsByTagName("groupId").item(0)?.textContent ?: ""
                        val a = node.getElementsByTagName("artifactId").item(0)?.textContent ?: ""
                        val v = node.getElementsByTagName("version").item(0)?.textContent ?: ""
                        
                        // Filter out dependencies containing property placeholders like ${project.version}
                        if (g.isNotEmpty() && a.isNotEmpty() && v.isNotEmpty() && !v.contains('$')) {
                            list.add("$g:$a:$v")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to empty if POM parsing fails
        }
        return list
    }
}

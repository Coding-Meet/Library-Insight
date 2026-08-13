package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateCommand : CliktCommand(
    name = "update",
    help = "Check for updates and update Library Insight to the latest version."
) {
    override fun run() {
        echo("Checking for updates...")
        val currentVersion = try {
            val properties = java.util.Properties()
            val stream = UpdateCommand::class.java.getResourceAsStream("/version.properties")
            if (stream != null) {
                properties.load(stream)
                properties.getProperty("version") ?: error("version not found")
            } else {
                error("version not found")
            }
        } catch (e: Exception) {
            echo("Error: Current version not found.", err = true)
            return
        }
        
        try {
            val url = URL("https://api.github.com/repos/Coding-Meet/Library-Insight/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Library-Insight")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                // Simple regex to parse "tag_name"
                val regex = "\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"".toRegex()
                val matchResult = regex.find(response)
                val latestVersion = matchResult?.groupValues?.get(1)
                
                if (latestVersion != null) {
                    if (latestVersion != currentVersion) {
                        echo("A new version is available: v$latestVersion (Current: v$currentVersion)")
                        echo("Updating Library Insight...")
                        
                        val process = ProcessBuilder("bash", "-c", "curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash")
                            .inheritIO()
                            .start()
                        val exitCode = process.waitFor()
                        if (exitCode == 0) {
                            echo("Library Insight updated successfully to v$latestVersion!")
                        } else {
                            echo("Error: Update failed with exit code $exitCode")
                        }
                    } else {
                        echo("Library Insight is already up to date (v$currentVersion).")
                    }
                } else {
                    echo("Error: Could not retrieve latest version tag.")
                }
            } else {
                echo("Error: Failed to connect to GitHub API (HTTP ${connection.responseCode})")
            }
        } catch (e: Exception) {
            echo("Error checking for updates: ${e.message}")
        }
    }
}

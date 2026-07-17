package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.meet.libraryinsight.common.MavenResolver

class ClearCacheCommand : CliktCommand(
    name = "clear-cache",
    help = "Clear all cached Maven artifacts from local storage."
) {
    override fun run() {
        echo("Clearing local cache at: ${MavenResolver.cacheDir.absolutePath}...")
        val bytesDeleted = MavenResolver.clearCache()
        val mbDeleted = bytesDeleted / 1024.0 / 1024.0
        val formatted = String.format(java.util.Locale.US, "%.2f", mbDeleted)
        echo("Cache cleared successfully. Deleted $formatted MB.")
    }
}

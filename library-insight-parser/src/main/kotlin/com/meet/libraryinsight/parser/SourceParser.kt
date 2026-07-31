package com.meet.libraryinsight.parser

import com.meet.libraryinsight.model.ClassApi
import com.meet.libraryinsight.model.LibraryApiIndex
import com.meet.libraryinsight.model.PackageApi
import com.meet.libraryinsight.model.ScanMode
import java.io.File

object SourceParser {
    fun parseDirectory(
        directory: File,
        libraryName: String = directory.name,
        version: String = "1.0.0"
    ): LibraryApiIndex {
        val allClassApis = mutableListOf<ClassApi>()

        if (directory.exists() && directory.isDirectory) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    when {
                        file.name.endsWith(".kt") -> {
                            allClassApis.addAll(KotlinSourceParser.parse(file))
                        }
                        file.name.endsWith(".java") -> {
                            allClassApis.addAll(JavaSourceParser.parse(file))
                        }
                    }
                }
            }
        }

        // Group ClassApis by package
        val packageGroups = allClassApis.groupBy { classApi ->
            classApi.name.substringBeforeLast('.', "")
        }

        val packages = packageGroups.map { (pkgName, classes) ->
            PackageApi(
                name = pkgName,
                classes = classes
            )
        }

        return LibraryApiIndex(
            libraryName = libraryName,
            version = version,
            packages = packages,
            scanMode = ScanMode.SOURCE
        )
    }
}

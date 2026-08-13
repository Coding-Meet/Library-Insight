plugins {
    kotlin("jvm") apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
}

allprojects {
    group = "com.meet.libraryinsight"
    version = "1.4.0"

    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "buildsrc.convention.kotlin-jvm")

    dependencies {
        // Kotest testing framework
        "testImplementation"("io.kotest:kotest-runner-junit5:5.8.0")
        "testImplementation"("io.kotest:kotest-assertions-core:5.8.0")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}

tasks.register("setupGitHooks") {
    group = "git"
    description = "Installs Git pre-commit hooks to keep SKILL.md in sync automatically."

    val hooksDir = file(".git/hooks")
    val preCommitHook = file(".git/hooks/pre-commit")

    doLast {
        if (!hooksDir.exists()) {
            logger.warn(".git/hooks directory not found. Skipping git hooks installation.")
            return@doLast
        }

        val hookScript = """
            #!/bin/sh
            echo "Running Git pre-commit hook: generating SKILL.md..."
            ./gradlew generateAgentSkill
            
            # If SKILL.md was modified, stage it
            git add .agents/skills/library-insight/SKILL.md
        """.trimIndent()

        preCommitHook.writeText(hookScript)
        preCommitHook.setExecutable(true, false)
        logger.lifecycle("Successfully installed Git pre-commit hook at ${preCommitHook.path}")
    }
}

tasks.register("generateAgentSkill") {
    dependsOn("setupGitHooks")
    group = "documentation"
    description = "Generates .agents/skills/library-insight/SKILL.md from split docs/cli/*.md and a template."

    val cliDir = file("docs/cli")
    val templateFile = file(".agents/skills/library-insight/SKILL.template.md")
    val outputFile = file(".agents/skills/library-insight/SKILL.md")

    val scannersFile = file("docs/cli/scanners.md")
    val explorerFile = file("docs/cli/explorer.md")
    val analysisFile = file("docs/cli/analysis.md")
    val versioningFile = file("docs/cli/versioning.md")
    val dependenciesFile = file("docs/cli/dependencies.md")
    val aiIntegrationFile = file("docs/cli/ai-integration.md")

    inputs.dir(cliDir)
    inputs.file(templateFile)
    outputs.file(outputFile)

    doLast {
        if (!cliDir.exists() || !cliDir.isDirectory) {
            throw GradleException("docs/cli directory does not exist")
        }
        if (!templateFile.exists()) {
            throw GradleException("SKILL.template.md does not exist")
        }

        // Ordered categories/files
        val categoryFiles = listOf(
            "Scanners & Indexing" to scannersFile,
            "API Explorer & Lookup" to explorerFile,
            "Analysis & Diagnostics" to analysisFile,
            "Versioning & Migration" to versioningFile,
            "Maven & Dependency Resolution" to dependenciesFile,
            "AI Context & Integration" to aiIntegrationFile
        )

        val commandsSection = buildString {
            categoryFiles.forEachIndexed { index, (catTitle, file) ->
                if (!file.exists()) {
                    throw GradleException("${file.name} does not exist")
                }
                file.readLines().forEach { line ->
                    append(line + "\n")
                }
            }
        }.trim()

        val templateContent = templateFile.readText()
        val finalContent = templateContent.replace("{{COMMAND_REFERENCE}}", commandsSection)
        outputFile.writeText(finalContent)
        logger.lifecycle("Successfully generated ${outputFile.path} from split CLI docs")
    }
}

subprojects {
    tasks.configureEach {
        if (name == "processResources") {
            dependsOn(rootProject.tasks.named("generateAgentSkill"))
        }
    }
}


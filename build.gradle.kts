plugins {
    kotlin("jvm") apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
}

allprojects {
    group = "com.meet.libraryinsight"
    version = "1.3.0"

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
    description = "Generates .agents/skills/library-insight/SKILL.md from docs/cli.md and a template."

    val cliFile = file("docs/cli.md")
    val templateFile = file(".agents/skills/library-insight/SKILL.template.md")
    val outputFile = file(".agents/skills/library-insight/SKILL.md")

    inputs.file(cliFile)
    inputs.file(templateFile)
    outputs.file(outputFile)

    doLast {
        if (!cliFile.exists()) {
            throw GradleException("docs/cli.md does not exist")
        }
        if (!templateFile.exists()) {
            throw GradleException("SKILL.template.md does not exist")
        }

        val cliContent = cliFile.readText()
        val startMarker = "<!-- --8<-- [start:commands] -->"
        val endMarker = "<!-- --8<-- [end:commands] -->"
        
        val startIndex = cliContent.indexOf(startMarker)
        val endIndex = cliContent.indexOf(endMarker)

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            throw GradleException("Could not find start/end command markers in docs/cli.md")
        }

        val commandsSection = cliContent.substring(startIndex + startMarker.length, endIndex).trim()

        // Shift headers: ## to ### (to fit in SKILL.md under ## Command Reference)
        val shiftedCommands = commandsSection.lines().joinToString("\n") { line ->
            if (line.startsWith("## ")) {
                "### " + line.substring(3)
            } else if (line.startsWith("### ")) {
                "#### " + line.substring(4)
            } else {
                line
            }
        }

        val templateContent = templateFile.readText()
        val finalContent = templateContent.replace("{{COMMAND_REFERENCE}}", shiftedCommands)
        outputFile.writeText(finalContent)
        logger.lifecycle("Successfully generated ${outputFile.path} from docs/cli.md")
    }
}

subprojects {
    tasks.configureEach {
        if (name == "processResources") {
            dependsOn(rootProject.tasks.named("generateAgentSkill"))
        }
    }
}


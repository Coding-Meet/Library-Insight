plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.meet.libraryinsight.cli.MainKt")
    applicationName = "library-insight"
}

dependencies {
    implementation(project(":library-insight-core"))
    implementation(project(":library-insight-common"))
    implementation("com.github.ajalt.clikt:clikt-jvm:4.4.0")
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxSerialization)
    runtimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

val projectVersion = rootProject.version.toString()
val skillFile = rootProject.file(".agents/skills/library-insight/SKILL.md")
val scriptsDir = rootProject.file(".agents/skills/library-insight/scripts")

tasks.processResources {
    inputs.property("version", projectVersion)
    filter(mapOf("tokens" to mapOf("version" to projectVersion)), org.apache.tools.ant.filters.ReplaceTokens::class.java)
    from(skillFile)
    from(scriptsDir).into("scripts")
}

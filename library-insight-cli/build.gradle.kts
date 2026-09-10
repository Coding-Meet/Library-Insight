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
    implementation(libs.clikt)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxSerialization)
    runtimeOnly(libs.slf4j.simple)
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

val createLiStartScripts = tasks.register<CreateStartScripts>("createLiStartScripts") {
    mainClass.set("com.meet.libraryinsight.cli.MainKt")
    applicationName = "li"
    outputDir = layout.buildDirectory.dir("scripts-li").get().asFile
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.named<Sync>("installDist") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(createLiStartScripts) {
        into("bin")
    }
}

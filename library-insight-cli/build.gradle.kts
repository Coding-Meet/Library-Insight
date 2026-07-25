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

tasks.processResources {
    from(rootProject.file(".agents/skills/library-insight/SKILL.md"))
}

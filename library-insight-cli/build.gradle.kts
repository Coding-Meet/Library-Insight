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
    implementation("com.github.ajalt.clikt:clikt-jvm:4.4.0")
}

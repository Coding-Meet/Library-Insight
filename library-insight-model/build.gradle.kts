plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":library-insight-common"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

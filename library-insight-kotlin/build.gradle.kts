plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":library-insight-parser"))
    api("org.jetbrains.kotlin:kotlin-metadata-jvm:2.0.0")
}

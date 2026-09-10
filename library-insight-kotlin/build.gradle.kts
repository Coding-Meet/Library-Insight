plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":library-insight-parser"))
    api(libs.kotlin.metadata.jvm)
}

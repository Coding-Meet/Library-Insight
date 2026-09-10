plugins {
    kotlin("jvm")
}

dependencies {
    api(libs.kotlinxCoroutines)
    api(project(":library-insight-parser"))
    api(project(":library-insight-kotlin"))
    api(project(":library-insight-search"))
    api(project(":library-insight-export"))
}

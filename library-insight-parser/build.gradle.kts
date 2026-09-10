plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":library-insight-model"))
    api(libs.asm)
    api(libs.asm.tree)
    api(libs.javaparser.core)
    api(libs.kotlin.compiler.embeddable)
}

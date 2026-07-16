plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":library-insight-model"))
    api("org.ow2.asm:asm:9.7")
    api("org.ow2.asm:asm-tree:9.7")
}

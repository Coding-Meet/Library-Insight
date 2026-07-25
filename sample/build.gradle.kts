plugins {
    kotlin("jvm")
}

java {
    withSourcesJar()
}

dependencies {
    // Standard library only, as this is a sample target library
    implementation("org.ow2.asm:asm:9.7")
}

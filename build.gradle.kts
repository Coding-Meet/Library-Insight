plugins {
    kotlin("jvm") apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
}

allprojects {
    group = "com.meet.libraryinsight"
    version = "1.0.0"

    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "buildsrc.convention.kotlin-jvm")

    dependencies {
        // Kotest testing framework
        "testImplementation"("io.kotest:kotest-runner-junit5:5.8.0")
        "testImplementation"("io.kotest:kotest-assertions-core:5.8.0")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}

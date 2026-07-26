package com.meet.sample

// ============================================================
// DSL Marker annotations — demonstrates @DslMarker detection
// ============================================================

@DslMarker
annotation class HtmlDsl

@DslMarker
annotation class ConfigDsl

// ============================================================
// Type aliases — demonstrates type alias extraction
// ============================================================

typealias BuilderBlock<T> = T.() -> Unit
typealias HtmlContent = HtmlBuilder.() -> Unit
typealias ConfigBlock = AppConfigBuilder.() -> Unit

// ============================================================
// DSL Scope Classes — @HtmlDsl scoped builders
// ============================================================

@HtmlDsl
class HtmlBuilder {
    private val children = mutableListOf<String>()

    /** Adds a paragraph element to the HTML output. */
    fun p(text: String) { children.add("<p>$text</p>") }

    /** Adds a heading element. */
    fun h1(text: String) { children.add("<h1>$text</h1>") }

    /** Adds a nested div block. */
    fun div(block: HtmlBuilder.() -> Unit) {
        val inner = HtmlBuilder().apply(block)
        children.add("<div>${inner.build()}</div>")
    }

    fun build(): String = children.joinToString("\n")
}

@HtmlDsl
class HeadBuilder {
    var title: String = ""
    var charset: String = "UTF-8"

    fun meta(name: String, content: String) {}
}

// ============================================================
// Config DSL — @ConfigDsl scoped builder
// ============================================================

@ConfigDsl
class AppConfigBuilder {
    var host: String = "localhost"
    var port: Int = 8080
    var debug: Boolean = false
    var maxConnections: Int = 100

    /** Sets up database configuration block. */
    fun database(block: DatabaseConfigBuilder.() -> Unit) {
        DatabaseConfigBuilder().apply(block)
    }
}

@ConfigDsl
class DatabaseConfigBuilder {
    var url: String = ""
    var username: String = ""
    var password: String = ""
    var poolSize: Int = 10
}

// ============================================================
// Top-level DSL builder functions with lambda receivers
// ============================================================

/** Builds an HTML document using a DSL block. */
fun html(block: HtmlBuilder.() -> Unit): String {
    return HtmlBuilder().apply(block).build()
}

/** Builds an application configuration using a DSL block. */
fun appConfig(block: AppConfigBuilder.() -> Unit): AppConfigBuilder {
    return AppConfigBuilder().apply(block)
}

/** Configures and runs a block with a typed receiver. */
fun <T> T.configure(block: T.() -> Unit): T = apply(block)

// ============================================================
// Inline reified functions — demonstrates reified detection
// ============================================================

/** Finds items of a specific type from a list. */
inline fun <reified T> List<*>.filterIsType(): List<T> {
    return filterIsInstance<T>()
}

/** Creates an instance of a reified type from a JSON-like string. */
inline fun <reified T : Any> deserialize(json: String): T? {
    return null // simplified for demo
}

// ============================================================
// Extension functions — demonstrates extension function surface
// ============================================================

/** Converts a String to a URL-safe slug. */
fun String.toSlug(): String = this.lowercase().replace(" ", "-")

/** Wraps this string in an HTML paragraph tag. */
fun String.asHtmlParagraph(): String = "<p>$this</p>"

/** Retries a suspendable block up to [times] times. */
suspend fun <T> retry(times: Int = 3, block: suspend () -> T): T {
    repeat(times - 1) {
        try { return block() } catch (_: Exception) {}
    }
    return block()
}

// ============================================================
// Original sample classes (preserved)
// ============================================================

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class CustomMarker(val description: String)

@CustomMarker("This is a sample Kotlin data class")
data class User(
    val id: Int,
    var name: String,
    val email: String? = null
) {
    class Builder {
        private var id: Int = 0
        private var name: String = ""
        private var email: String? = null
        fun id(id: Int) = apply { this.id = id }
        fun name(name: String) = apply { this.name = name }
        fun email(email: String) = apply { this.email = email }
        fun build(): User = User(id, name, email)
    }
}

/**
 * Core interface for greeting users.
 */
interface Greeter {
    /**
     * Greets a person by name.
     */
    fun greet(name: String): String
}

object AppConfig {
    const val VERSION: String = "1.0.0"
    var debugMode: Boolean = false
    fun printConfig() = println("Version: $VERSION, Debug: $debugMode")
}

/**
 * A sample calculator class that implements [Greeter].
 */
class Calculator : Greeter {

    companion object {
        const val PI = 3.1415926535
        @JvmStatic
        fun create(): Calculator = Calculator()
    }

    /**
     * Implementation of [Greeter.greet] returning a friendly greeting.
     */
    override fun greet(name: String): String = "Hello, $name from Calculator!"

    operator fun plus(other: Calculator): Calculator = this

    /**
     * Multiplies the given input value by 2.
     */
    infix fun calculateWith(value: Int): Int = value * 2

    inline fun runOperation(block: () -> Unit) = block()

    /**
     * Performs a suspendable addition of two integers.
     */
    suspend fun performAsyncCalculation(a: Int, b: Int): Int = a + b
}

// Extension function (simple)
fun String.shout(): String = this.uppercase() + "!"

// Value class (inline class)
@JvmInline
value class Password(val value: String)

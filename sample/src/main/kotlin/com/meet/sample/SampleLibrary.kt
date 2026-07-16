package com.meet.sample

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class CustomMarker(val description: String)

@CustomMarker("This is a sample Kotlin data class")
data class User(
    val id: Int,
    var name: String,
    val email: String? = null
)

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
}

/**
 * A sample calculator class that implements [Greeter].
 */
class Calculator : Greeter {
    
    companion object {
        const val PI = 3.1415926535
        fun create(): Calculator = Calculator()
    }

    /**
     * Implementation of [Greeter.greet] returning a friendly greeting.
     */
    override fun greet(name: String): String {
        return "Hello, $name from Calculator!"
    }

    operator fun plus(other: Calculator): Calculator {
        return this
    }

    /**
     * Multiplies the given input value by 2.
     */
    infix fun calculateWith(value: Int): Int {
        return value * 2
    }

    inline fun runOperation(block: () -> Unit) {
        block()
    }

    /**
     * Performs a suspendable addition of two integers.
     */
    suspend fun performAsyncCalculation(a: Int, b: Int): Int {
        return a + b
    }
}

// Extension function
fun String.shout(): String {
    return this.uppercase() + "!"
}

// Value class (inline class)
@JvmInline
value class Password(val value: String)

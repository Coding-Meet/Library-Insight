# API Explorer & Lookup

## `search` — Search Symbols

Search for packages, classes, methods, or properties in the saved index.

```bash
library-insight search Retrofit
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight search "anno:Keep" --db custom-index.json
```

**Example output:**

```
Found 2 matches for 'Retrofit':
--------------------------------------------------
[CLASS]     class retrofit2.Retrofit
           Source: src/main/java/retrofit2/Retrofit.java:18
[CLASS]     class retrofit2.Retrofit$Builder
           Source: src/main/java/retrofit2/Retrofit.java:82
--------------------------------------------------
```

---

## `explain` — Explain a Class or Symbol

Print detailed structural information (modifiers, superclass, constructors, properties, methods, Javadoc/KDoc, and nested usage guide examples extracted from README/Dokka markdown files) for a specific class or top-level symbol.

```bash
library-insight explain HtmlBuilder
```

**Smart Resolution Capabilities:**

- **Kotlin Top-Level Function Resolution (`${name}Kt`)**: Querying a top-level Kotlin function (e.g. `library-insight explain Grid`) automatically resolves to its Kotlin facade class (`GridKt`).
- **Member Method & Property Lookup**: Querying a method or property directly (e.g. `library-insight explain gridItem`) automatically locates its declaring class (e.g. `GridScope`).
- **Fuzzy Typo Suggestions**: If a symbol is not found or has a typo, `explain` displays "Did you mean one of these?" with close matches.

**Optional Parameters:**

- `-d, --deep`: Recursively explain referenced parameter types, DSL receiver scopes, and return types in a single output.
- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
# Deep recursive DSL scope resolution
library-insight explain Grid --deep

# Custom database index
library-insight explain HtmlBuilder --db custom-index.json
```

**Example output:**

```
==================================================
 CLASS EXPLAIN REPORT
==================================================
Class:       com.meet.sample.HtmlBuilder
Package:     com.meet.sample
Kind:        class
Visibility:  public
Source:      sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:25
Annotations: @HtmlDsl
==================================================

Imports:
  - kotlin.text.*
  - retrofit2.Retrofit

Properties:
  - private val children: Any (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:27)

Methods:
  // /** Adds a paragraph element to the HTML output. */
  - public fun p(text: String): Unit (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:29)
  // /** Adds a heading element. */
  - public fun h1(text: String): Unit (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:32)
  // /** Adds a nested div block. */
  - public fun div(block: HtmlBuilder.() -> Unit): Unit (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:35)
  - public fun build(): String (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:41)
```

---

## `dsl-report` — Kotlin DSL Surface Report

Generate a dedicated Kotlin DSL surface report for DSL-heavy libraries. Shows:

- **Type aliases** extracted from package metadata
- **DSL scopes** — classes annotated with `@DslMarker` markers, grouped by marker
- **Extension functions** — all `ReceiverType.function()` signatures
- **Lambda-with-receiver parameters** — DSL builder functions (`block: Builder.() -> Unit`)
- **Inline reified functions** — functions with `reified` type parameters

```bash
library-insight dsl-report
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)
- `-p, --package <pkg>`: Filter results to a specific package name prefix

**Example with options:**

```bash
library-insight dsl-report --package io.ktor.client --db custom-index.json
```

**Example output:**

```
==================================================
  DSL SURFACE REPORT  —  ktor-client-core-jvm 2.3.12
==================================================

▶ Type Aliases (3)
  typealias HttpClientConfig<T> = T.() -> Unit
  typealias ResponseValidator = suspend (response: HttpResponse) -> Unit
  typealias HeadersBuilder = StringValuesBuilder

▶ DSL Scopes — @DslMarker annotated classes (5)
  @KtorDsl → HttpClientConfig, HttpRequestBuilder, HeadersBuilder

▶ Extension Functions (24)
  fun HttpClient.get(urlString: String, block: (String) -> Unit): HttpResponse
  fun HttpRequestBuilder.contentType(contentType: ContentType): Unit
  ...

▶ Lambda-with-Receiver Parameters — DSL builder functions (12)
  fun httpClient([config: HttpClientConfig<*>.() -> Unit]): HttpClient
  fun HttpRequestBuilder.headers([block: HeadersBuilder.() -> Unit]): Unit
  ...

▶ Inline Reified Functions (3)
  inline fun <reified T> HttpClient.get(url: String): T
  inline fun <reified T> HttpResponse.body(): T
  ...

==================================================
  Tip: run 'explain <ClassName>' for full API details on any class above.
==================================================
```

> **Note for DSL library authors:** If your library uses `@DslMarker` and the annotation is bundled in the same JAR, Library Insight will group all DSL builder scopes by their marker annotation — making it easy for AI agents and developers to understand which builders can safely nest.

---

## `examples` — API Usage Examples Generator

Generate idiomatic Kotlin code examples showing typical usage patterns for a specific class. Automatically scans bytecode signatures to determine target design patterns (Constructor, Builder, Factory, Singleton) and extracts nested guide examples from README/Dokka markdown files.

```bash
library-insight examples HtmlBuilder
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight examples HtmlBuilder --db custom-index.json
```

**Example output:**

```
==================================================
  API USAGE EXAMPLES GENERATOR  —  HtmlBuilder
==================================================
// Target API: com.meet.sample.HtmlBuilder

Detected Usage Patterns:
  ✓ Constructor
  ✗ Builder
  ✗ Factory
  ✗ Singleton
==================================================

// Pattern: Guide Examples (from README/Dokka)
val result = html {
    head {
        title("My Page")
    }
    div {
        p("Welcome to Library Insight!")
    }
}

// Pattern: Constructor Instantiation
val htmlbuilder = com.meet.sample.HtmlBuilder()

// API Invocation Examples
  htmlbuilder.p(text = "example") // returns: kotlin.Unit
  htmlbuilder.h1(text = "example") // returns: kotlin.Unit
  htmlbuilder.div(block = { }) // returns: kotlin.Unit
  htmlbuilder.build() // returns: kotlin.String
==================================================
```

---

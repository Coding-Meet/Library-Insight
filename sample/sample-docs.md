# Sample DSL Guide

Here is an example showing how to combine `HtmlBuilder` and `HeadBuilder` in a nested DSL structure:

```kotlin
// Setup and run HtmlBuilder configuration
val result = html {
    head {
        title("My Page")
    }
    div {
        p("Welcome to Library Insight!")
    }
}
```

And configure `AppConfig` like this:

```kotlin
// Exposing AppConfig database builder
val config = appConfig {
    database {
        url = "jdbc:mysql://localhost:3306/db"
    }
}
```

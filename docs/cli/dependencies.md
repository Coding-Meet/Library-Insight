# Maven & Dependency Resolution

## `search-central` — Search Maven Central

Search Maven Central dynamically for matching coordinates and versions.

**Search for Retrofit on Maven Central:**

```bash
library-insight search-central retrofit
```

**Or search for another library like Clikt:**

```bash
library-insight search-central clikt
```

**Example output:**

```
Searching Maven Central for 'clikt'...

Found 10 matching libraries on Maven Central:

📦 Coordinate: com.github.ajalt.clikt:clikt:5.0.3
   Repository: central
   Group:      com.github.ajalt.clikt
   Artifact:   clikt
--------------------------------------------------
📦 Coordinate: com.github.ajalt:clikt:2.8.0
   Repository: central
   Group:      com.github.ajalt
   Artifact:   clikt
```

---

## `dependency-graph` — Dependency Tree

Print a visual recursive tree of transitive dependencies from POM descriptors.

```bash
library-insight dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0
```

**Example output:**

```
com.github.ajalt.clikt:clikt-jvm:4.4.0
│   ├── com.github.ajalt.mordant:mordant-jvm:2.5.0
│   │   ├── com.github.ajalt.colormath:colormath-jvm:3.5.0
│   │   │   ├── org.jetbrains.kotlin:kotlin-stdlib:1.9.21
```

---

# Versioning & Migration

## `diff` — Compare Versions

Compare two library archives to check for added, removed, and changed APIs including breaking changes.

```bash
library-insight diff retrofit-2.9.0.jar retrofit-2.11.0.jar
```

**Or via Maven coordinates:**

```bash
library-insight diff com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

**Example output:**

```
==================================================
 LIBRARY INSIGHT API DIFF REPORT
==================================================
Breaking Changes Found: NO
➕ Added Classes:
  - retrofit2.Reflection
📝 Changed Classes:
  Class: retrofit2.Invocation
    Added Methods:
      + fun service(): java.lang.Class<?>
```

---

## `migrate` — Migration Advisor

Compare two versions and get a structured migration report showing removed, deprecated, and replacement APIs.

```bash
library-insight migrate com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

**Optional Parameters:**

- `--repo <url>`: Additional Maven repository URLs to resolve coordinates (multiple allowed)

**Example with options:**

```bash
library-insight migrate com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0 --repo https://repo.maven.apache.org/maven2
```

**Example output:**

```
==================================================
        Library Insight Migration Report
==================================================
Old Version : 2.9.0
New Version : 2.11.0

❌ Removed Classes
  - retrofit2.Platform$Android
❌ Removed Methods
  - fun retrofit2.Platform.defaultCallbackExecutor(): Executor

Binary Compatibility: ❌ BREAKING CHANGES DETECTED
```

---

## `semver` — SemVer Compliance Check

Verify that the version number bump between two releases correctly reflects the bytecode changes (breaking change requires major bump, added APIs require minor bump).

```bash
library-insight semver com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

**Example output:**

```
🚨 SemVer Violation: Version bump does not match API changes!
  ❌ API Breaking Change detected but MAJOR version was not incremented!
```

---

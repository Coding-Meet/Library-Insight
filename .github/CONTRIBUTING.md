# Contributing to Library Insight

Thank you for your interest in contributing to Library Insight! We welcome bug reports, feature suggestions, documentation updates, and pull requests.

---

## How to Contribute

### 1. Reporting Bugs & Requesting Features
- Please search existing issues before opening a new one.
- Use the appropriate **Bug Report** or **Feature Request** templates.

### 2. Local Development Setup
Library Insight is built as a multi-module Kotlin project.
- **Requirement**: JDK 17 or higher.
- Clone the repository:
  ```bash
  git clone https://github.com/Coding-Meet/Library-Insight.git
  cd Library-Insight
  ```
- Build and run unit tests using Gradle wrapper:
  ```bash
  ./gradlew test
  ```
- Compile and run the CLI distribution locally:
  ```bash
  ./gradlew installDist
  ./library-insight-cli/build/install/library-insight/bin/library-insight --help
  ```

### 3. Code Style & Standards
- Follow standard Kotlin coding conventions.
- Make sure all unit tests pass before submitting a pull request.
- Keep modifications clean, modularized, and documented where necessary.

### 4. Submitting a Pull Request (PR)
- Create a feature branch for your edits.
- Ensure your commits have clear, descriptive messages.
- Fill out the provided pull request template when opening your PR.

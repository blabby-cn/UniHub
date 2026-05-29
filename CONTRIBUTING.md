# Contributing to UniHub

Thank you for your interest in contributing to UniHub! We welcome contributions of all kinds.

## How to Contribute

### Reporting Bugs

- Search [existing issues](https://github.com/blabby-cn/UniHub/issues) first to avoid duplicates.
- Provide a clear title and detailed description, including steps to reproduce, expected behavior, and actual behavior.
- Include device model, Android version, and app version if applicable.

### Suggesting Features

- Open a [feature request issue](https://github.com/blabby-cn/UniHub/issues/new) describing the desired functionality and use case.

### Submitting Code Changes

1. Fork the repository.
2. Create a new branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
3. Make your changes, following the existing code style.
4. Test your changes thoroughly.
5. Commit with a clear, descriptive message:
   ```bash
   git commit -m "feat: add xxx feature"
   ```
6. Push to your fork and open a Pull Request against `main`.

### Adding a New Language

1. Create a new `assets/languages/{code}.yaml` file (e.g., `assets/languages/vi.yaml` for Vietnamese).
2. Copy the key-value structure from `assets/languages/en.yaml`.
3. Add your language entry to `Localization.java` in both `getDisplayName()` and `getSupportedLanguages()` methods.

## Code Style

- **Language**: Java (not Kotlin)
- **Indentation**: 4 spaces
- **Naming**: `camelCase` for methods/variables, `PascalCase` for classes
- Follow the existing patterns in the codebase.

## Pull Request Guidelines

- Keep PRs focused on a single concern.
- Provide a clear description of what the PR does and why.
- Ensure the app still compiles and runs correctly.

## Code of Conduct

Be respectful and constructive. Harassment or offensive behavior will not be tolerated.

---

Thank you for helping make UniHub better!

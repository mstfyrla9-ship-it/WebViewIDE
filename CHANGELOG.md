# Changelog

## 0.2.1 - 2026-11-21

### Added
- Added Android/Gradle project analysis for package name, SDK levels, Gradle/AGP versions, languages, dependencies, and missing files.
- Added APK inspection for package metadata, architecture, alignment, signature validity, and artifact size.
- Added a staged build pipeline with cancellation, progress, logs, success verification, and repair suggestions.
- Added project backups and safe independent-clone identity changes for project name, package name, and versions.
- Added local build-error diagnosis and a redacted technical support report for `mstfyrla9@gmail.com`.
- Added Arabic and Russian resources and runtime language switching with RTL support for Arabic.

### Changed
- Replaced mock build success signaling with verification through the real APK packager, Zipalign, apksig, and APK inspection.
- Extended artifact metadata and build history with signed/aligned status and APK package information.
- Improved project detection for Android Gradle, Java, and Kotlin project types.

### Fixed
- Added real cancellation handling across packaging stages.
- Added backup creation before identity changes and clone operations.
- Redacted passwords and bearer tokens from technical support reports.

## 0.2.0 - 2026-11-21

### Added
- Added an in-editor AI assistant with OpenAI-compatible, Anthropic, Gemini, OpenRouter, Groq, DeepSeek, xAI, Mistral, and custom provider support.
- Added encrypted per-provider API key storage.
- Added AI result preview and application to the active file.
- Added ZIP project import from the welcome screen.

### Changed
- Updated the project menu and welcome screen to expose AI and ZIP import actions.

### Fixed
- Added the Compose imports required to compile the AI API key and response UI.

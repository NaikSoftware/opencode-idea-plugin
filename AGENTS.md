# OpenCode IntelliJ Plugin - Agent Guidelines

## Project Overview
IntelliJ IDEA plugin that provides AI-powered code assistance through OpenCode API integration. Features code optimization, explanation, and interactive chat interface.

## Build/Test Commands

- **Build plugin**: `./gradlew buildPlugin` (from plugin/ directory)
- **Run in sandbox**: `./gradlew runIde`
- **Clean build**: `./gradlew clean buildPlugin`
- **Test**: `./gradlew test` (JUnit 4 tests)
- **Single test**: `./gradlew test --tests "*TestClass*"`
- **Verify**: `./gradlew verifyPlugin`

## Code Style Guidelines

- **Language**: Java 17 with IntelliJ Platform SDK 2024.2.4
- **Package**: `ua.naiksoftware.opencodeidea.*` (note: no hyphens in package names)
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **Imports**: IntelliJ annotations (@NotNull, @Nullable), organize imports
- **Error handling**: LOG.error() for exceptions, CompletableFuture for async
- **Architecture**: Actions, Services, UI components, Settings
- **Dependencies**: Gson for JSON, HttpClient for API calls

## Architecture

- **Actions**: Menu items and keyboard shortcuts (Ctrl+Alt+O/P)
- **Services**: API communication layer with async handling
- **UI**: Tool window and settings panels
- **Settings**: Plugin configuration with API URL/key
- **Integration**: OpenCode submodule for AI backend

## Development Tools

- **opencode**: AI coding assistant submodule (see `opencode/AGENTS.md`)
- **Gradle**: Build system with IntelliJ plugin
- **Java 17**: Target and source compatibility
- **Gson**: JSON serialization/deserialization
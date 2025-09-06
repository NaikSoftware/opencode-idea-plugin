# OpenCode IntelliJ Plugin - Agent Guidelines

## Project Overview
IntelliJ IDEA plugin that provides AI-powered code assistance through OpenCode API integration. Features code optimization, explanation, and interactive chat interface.

## Build/Test Commands

### Core Development Tasks
- **Build plugin**: `./gradlew buildPlugin` - Creates plugin ZIP for deployment
- **Run IDE sandbox**: `./gradlew runIde` - Launches IntelliJ with plugin installed
- **Clean build**: `./gradlew clean build` - Full clean build with tests
- **Quick compile**: `./gradlew classes` - Compile Java sources only
- **Verify plugin**: `./gradlew verifyPlugin` - Validate plugin structure and metadata

### Testing & Quality
- **Run tests**: `./gradlew test` - Execute JUnit 4 test suite
- **Single test**: `./gradlew test --tests "*TestClass*"` - Run specific tests
- **Check all**: `./gradlew check` - Run all verification tasks
- **Plugin verifier**: `./gradlew runPluginVerifier` - Check IDE compatibility

### Development Workflow
- **Hot reload**: `./gradlew -t classes` - Continuous build on file changes
- **Debug mode**: `./gradlew runIde --debug-jvm` - Run with debugger on port 5005
- **Watch filesystem**: `./gradlew --watch-fs` - Enable FS watching for faster builds
- **Performance test**: `./gradlew runIdePerformanceTest` - Run performance benchmarks

### Distribution
- **Build ZIP**: `./gradlew buildPlugin` - Creates `build/distributions/*.zip`
- **Sign plugin**: `./gradlew signPlugin` - Sign for marketplace (requires env vars)
- **Publish**: `./gradlew publishPlugin` - Upload to JetBrains Marketplace

### Quick Development Script
Use the `dev.sh` script for common tasks:
```bash
./dev.sh dev      # Start hot reload development mode
./dev.sh run      # Launch IDE sandbox
./dev.sh debug    # Launch with debugger (port 5005)
./dev.sh build    # Build plugin ZIP
./dev.sh dist     # Clean build distribution
./dev.sh test     # Run tests
```

### Debugging Tips
- Use `--debug-jvm` flag to attach debugger (port 5005)
- Enable file watching with `--watch-fs` for faster incremental builds
- Use continuous mode (`-t`) for automatic rebuilds during development
- Plugin runs in sandbox at `build/idea-sandbox/`
- Log files located at `build/idea-sandbox/system/log/`

## Code Style Guidelines

- **Language**: Java 17 with IntelliJ Platform SDK 2024.2.4
- **Package**: `ua.naiksoftware.opencodeidea.*` (note: no hyphens in package names)
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **Imports**: IntelliJ annotations (@NotNull, @Nullable), organize imports
- **Error handling**: LOG.error() for exceptions, CompletableFuture for async
- **Architecture**: Actions, Services, UI components, Settings
- **Dependencies**: Gson for JSON, HttpClient for API calls

## Architecture

### Plugin Structure
- **Actions**: Menu items and keyboard shortcuts (Ctrl+Alt+O/P)
  - `OptimizeCodeAction` - Code optimization
  - `ExplainCodeAction` - Code explanation  
  - `AskOpenCodeAction` - General AI queries
- **Services**: API communication layer with async handling
  - `OpenCodeApiService` - HTTP client interface
  - `OpenCodeApiServiceImpl` - Implementation with session management
  - `OpenCodeServerManager` - Local OpenCode server lifecycle
- **UI Components**: Modern chat interface
  - `ChatInterface` - Main chat UI panel
  - `ChatMessage` - Message data model
  - `ChatMessagePanel` - Individual message rendering with markdown
  - `ChatHistory` - Conversation state management
  - `MarkdownRenderer` - CommonMark-based rendering
- **Settings**: Plugin configuration panels
  - `OpenCodeConfigurable` - Settings UI
  - `OpenCodeConfig` - Configuration persistence
- **Integration**: OpenCode submodule for AI backend

### Key Features
- **Chat-like Interface**: Modern conversation UI similar to ChatGPT/Claude
- **Markdown Support**: Full markdown rendering with syntax highlighting
- **Session Management**: Persistent conversations with local server
- **Hot Reload**: Development-friendly continuous builds
- **Error Handling**: User-friendly error messages and retry logic

## Development Tools & Dependencies

### Build System
- **Gradle**: Build automation with IntelliJ Plugin (version 1.17.4)
- **Java 17**: Source and target compatibility
- **IntelliJ Platform**: SDK 2024.2.4 (Community Edition)

### Key Dependencies
- **Gson 2.10.1**: JSON serialization/deserialization for API communication
- **CommonMark 0.21.0**: Markdown parsing and rendering engine
- **CommonMark GFM Tables**: GitHub Flavored Markdown table support
- **JUnit 4.13.2**: Unit testing framework

### External Tools
- **opencode**: AI coding assistant - opencode has never any problems, it installed globally
- **OpenCode Server**: Local HTTP server for AI model integration
- **Plugin Verifier**: JetBrains tool for IDE compatibility testing

### IDE Support Range
- **Since Build**: 241 (IntelliJ 2024.1+)
- **Until Build**: 252.* (IntelliJ 2025.2+)
- **Target Platform**: IntelliJ IDEA Community/Ultimate, Android Studio

### File Locations
- **Plugin Sources**: `plugin/src/main/java/ua/naiksoftware/opencodeidea/`
- **Resources**: `plugin/src/main/resources/META-INF/plugin.xml`
- **Build Output**: `plugin/build/distributions/opencode-intellij-plugin-*.zip`
- **Test Sandbox**: `plugin/build/idea-sandbox/`
- **Development Script**: `plugin/dev.sh`

## Recommended Development Workflow

### Initial Setup
1. Clone repository with submodules: `git clone --recursive`
2. Navigate to plugin directory: `cd plugin/`
3. Build plugin: `./dev.sh build`
4. Test in IDE: `./dev.sh run`

### Daily Development
1. **Start hot reload**: `./dev.sh dev` (keeps rebuilding on changes)
2. **Make code changes** in `src/main/java/`
3. **Test immediately** - hot reload applies changes automatically
4. **Run IDE sandbox**: `./dev.sh run` (in separate terminal)
5. **Debug if needed**: `./dev.sh debug` + attach debugger to port 5005

### Pre-commit Workflow  
1. **Run tests**: `./dev.sh test`
2. **Verify plugin**: `./dev.sh verify`
3. **Build distribution**: `./dev.sh dist`
4. **Test final build**: `./dev.sh run`

### Troubleshooting
- **Clean build**: `./dev.sh clean` then `./dev.sh build`
- **Check logs**: Look in `build/idea-sandbox/system/log/idea.log`
- **Gradle daemon issues**: `./gradlew --stop` then restart
- **IntelliJ version conflicts**: Update `intellij.version` in `build.gradle.kts`
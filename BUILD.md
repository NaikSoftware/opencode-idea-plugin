# Building the OpenCode IntelliJ Plugin

## Quick Build

Use the development script in the plugin directory:

```bash
cd plugin/
./dev.sh build
```

## Build Options

### Development Commands
```bash
./dev.sh build    # Build plugin ZIP
./dev.sh run      # Test in IDE sandbox
./dev.sh dev      # Start hot reload development
./dev.sh clean    # Clean build directory
./dev.sh dist     # Clean build distribution
```

### Manual Build
If you prefer to use Gradle directly:
```bash
cd plugin/
./gradlew buildPlugin
```

## Build Output

The built plugin will be available at:
```
plugin/build/distributions/opencode-intellij-plugin-1.0.0.zip
```

## Installation

1. Open IntelliJ IDEA
2. Go to `File → Settings → Plugins`
3. Click the gear icon and select "Install Plugin from Disk..."
4. Navigate to and select the generated ZIP file
5. Restart IntelliJ IDEA when prompted

## Development Workflow

### Quick Development
```bash
cd plugin/
./dev.sh dev      # Start hot reload development
./dev.sh run      # Test in IDE sandbox
./dev.sh debug    # Debug with IDE (port 5005)
```

### Testing & Verification
```bash
./dev.sh test     # Run all tests
./dev.sh verify   # Verify plugin compatibility
./dev.sh check    # Run all checks
```

For comprehensive development guidelines, see [AGENTS.md](AGENTS.md).

## Build Requirements

- Java 17+
- Gradle 8.4+ (via wrapper)
- IntelliJ Platform 2024.2+ compatibility

## Features

The plugin includes:
- **Modern Chat Interface** - ChatGPT/Claude-style conversation UI
- **Markdown Support** - Full rendering with syntax highlighting
- **Hot Reload Development** - Continuous builds for efficient development
- **Automatic Server Management** - Local OpenCode server lifecycle
- **Session-based API** - Persistent conversations with proper error handling

## Known Issues

The current build configuration shows warnings about:
- Gradle IntelliJ Plugin 1.x compatibility with IntelliJ Platform 2024.2+
- Consider upgrading to IntelliJ Platform Gradle Plugin 2.0.0+ for full compatibility

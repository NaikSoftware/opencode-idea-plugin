# Building the OpenCode IntelliJ Plugin

## Quick Build

Use the provided build script in the root directory:

```bash
./build-plugin.sh
```

## Build Options

### Clean Build
To clean previous builds and rebuild from scratch:
```bash
./build-plugin.sh --clean
# or
./build-plugin.sh -c
```

### Manual Build
If you prefer to build manually:
```bash
cd plugin
./gradlew buildPlugin
```

### Clean Manual Build
```bash
cd plugin
./gradlew clean buildPlugin
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

## Development Commands

### Run Plugin in Development Mode
```bash
cd plugin
./gradlew runIde
```

### Run Tests
```bash
cd plugin
./gradlew test
```

### Verify Plugin Compatibility
```bash
cd plugin
./gradlew verifyPlugin
```

## Build Requirements

- Java 17+
- Gradle 8.4+ (via wrapper)
- IntelliJ Platform 2024.2+ compatibility

## Known Issues

The current build configuration shows warnings about:
- Gradle IntelliJ Plugin 1.x compatibility with IntelliJ Platform 2024.2+
- Consider upgrading to IntelliJ Platform Gradle Plugin 2.0.0+

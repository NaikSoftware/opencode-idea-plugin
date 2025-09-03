# OpenCode IntelliJ IDEA Plugin

An AI-powered assistant plugin for IntelliJ IDEA that integrates with OpenCode API to provide intelligent code suggestions, analysis, and assistance.

## Features

- **AI-powered code assistance**: Get intelligent suggestions and help with your code
- **Code optimization**: Ask AI to optimize selected code snippets
- **Code explanation**: Get detailed explanations of what your code does
- **Interactive tool window**: Chat interface for general AI assistance
- **Context menu integration**: Right-click on selected code for quick actions
- **Keyboard shortcuts**: Quick access to AI features

## Installation

### Prerequisites
- **IntelliJ IDEA**: 2025.2.1 or later (Community or Ultimate Edition)
- **Java**: JDK 17 or higher
- **Gradle**: 8.4 or higher (included via Gradle Wrapper)
- **Operating System**: Windows, macOS, or Linux

### Compatibility
- **IntelliJ IDEA**: 2024.2+ (build 241+)
- **Java**: 17+ (compiled with Java 17 target)
- **Plugin SDK**: Compatible with IntelliJ Platform 2024.2.4

### Building the Plugin

1. **Clone this repository:**
   ```bash
   git clone <repository-url>
   cd opencode-intellij-plugin
   ```

2. **Build the plugin:**
   ```bash
   ./gradlew buildPlugin
   ```

   The built plugin will be available at:
   ```
   build/distributions/opencode-intellij-plugin-1.0.0.zip
   ```

### Installing in IntelliJ IDEA 2025.2.1

#### Method 1: Install from Disk (Recommended)

1. **Open IntelliJ IDEA 2025.2.1**

2. **Go to Settings:**
   - On macOS: `IntelliJ IDEA > Settings`
   - On Windows/Linux: `File > Settings`

3. **Navigate to Plugins:**
   - In the Settings window, select `Plugins` from the left sidebar

4. **Install from Disk:**
   - Click the gear icon (⚙️) at the top of the Plugins window
   - Select `Install Plugin from Disk...`
   - Navigate to and select: `build/distributions/opencode-intellij-plugin-1.0.0.zip`
   - Click `OK`

5. **Restart IntelliJ IDEA:**
   - You'll be prompted to restart the IDE
   - Click `Restart` to complete the installation

#### Method 2: Manual Installation

If the above method doesn't work, you can manually extract and install:

1. **Extract the plugin:**
   ```bash
   cd build/distributions
   unzip opencode-intellij-plugin-1.0.0.zip -d opencode-plugin
   ```

2. **Copy to plugins directory:**
   - On macOS: `~/Library/Application Support/JetBrains/IdeaIC2025.2/plugins/`
   - On Windows: `%APPDATA%\JetBrains\IdeaIC2025.2\plugins\`
   - On Linux: `~/.config/JetBrains/IdeaIC2025.2/plugins/`

3. **Restart IntelliJ IDEA**

## Configuration

### API Setup

Before using the plugin, you need to configure the OpenCode API connection:

1. **Open Settings:**
   - Go to `File > Settings > Tools > OpenCode AI Assistant`

2. **Configure API:**
   - **API URL**: Enter your OpenCode API endpoint
     - Default: `http://localhost:8080/api/v1/chat`
     - Example: `https://your-opencode-server.com/api/v1/chat`
   - **API Key**: Enter your authentication key
     - Required for API access
     - Keep this secure and don't share it

3. **Apply Settings:**
   - Click `Apply` and `OK`
   - The plugin will show connection status in the tool window

### API Requirements

Your OpenCode API server should:
- Accept POST requests to `/api/v1/chat`
- Support JSON request/response format
- Use Bearer token authentication
- Handle the following request format:
  ```json
  {
    "message": "Your prompt here"
  }
  ```
- Return responses in this format:
  ```json
  {
    "response": "AI response here"
  }
  ```

## Usage

### Keyboard Shortcuts
- `Ctrl+Alt+O`: Ask OpenCode AI about selected code
- `Ctrl+Alt+P`: Optimize selected code

### Context Menu
1. Select code in the editor
2. Right-click and choose from OpenCode AI options:
   - Ask OpenCode AI
   - Optimize Code
   - Explain Code

### Tool Window
1. Open the OpenCode Assistant tool window (View > Tool Windows > OpenCode Assistant)
2. Type your question in the input area
3. Click "Send" to get AI assistance

## Building

### Build the Plugin

To build the plugin:

```bash
./gradlew buildPlugin
```

The build process will:
1. Compile the Java source code
2. Process resources and plugin.xml
3. Create the plugin JAR file
4. Package everything into a ZIP file at `build/distributions/opencode-intellij-plugin-1.0.0.zip`

### Development Build

To run the plugin in a development IntelliJ instance:

```bash
./gradlew runIde
```

This will start a sandboxed IntelliJ IDEA instance with your plugin loaded for testing.

### Clean Build

To clean and rebuild:

```bash
./gradlew clean buildPlugin
```

### Troubleshooting Build Issues

If you encounter build issues:

1. **Gradle IntelliJ Plugin Warning:**
   ```
   Gradle IntelliJ Plugin 1.x does not support building plugins against the IntelliJ Platform 2024.2+ (242+).
   ```
   This is a warning, not an error. The plugin will still build and work correctly.

2. **Java Version Issues:**
   - Ensure you're using Java 17 or higher
   - Check with: `java -version`

3. **Gradle Issues:**
   - Clear Gradle cache: `./gradlew clean`
   - Use wrapper: Always use `./gradlew` instead of system Gradle

## Development

### Project Structure
- `src/main/java/com/opencode/intellij/` - Main plugin source code
  - `actions/` - IntelliJ actions for menu items and shortcuts
  - `services/` - API service for OpenCode integration
  - `ui/` - User interface components
  - `settings/` - Plugin configuration
- `src/main/resources/META-INF/plugin.xml` - Plugin configuration
- `build.gradle.kts` - Build configuration

### API Integration

The plugin communicates with OpenCode API using HTTP requests. The `OpenCodeApiService` handles:
- Authentication with API key
- Request/response formatting
- Async request handling
- Error handling and timeouts

### Testing the Plugin

After installation and configuration:

1. **Verify Installation:**
   - Check that "OpenCode AI Assistant" appears in `File > Settings > Plugins`
   - Look for the OpenCode Assistant tool window in `View > Tool Windows`

2. **Test Connection:**
   - Open the OpenCode Assistant tool window
   - Check the status indicator (should show "✓ Connected" if API is configured)

3. **Test Features:**
   - Select some code in an editor
   - Try the keyboard shortcuts: `Ctrl+Alt+O` (ask) or `Ctrl+Alt+P` (optimize)
   - Test the context menu by right-clicking on selected code
   - Use the tool window for general questions

4. **Debug Issues:**
   - Check IntelliJ IDEA logs for errors
   - Verify API server is running and accessible
   - Ensure API key and URL are correct in settings

## License

This project is licensed under the MIT License.
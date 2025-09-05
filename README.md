# OpenCode IntelliJ IDEA Plugin

An IntelliJ IDEA plugin that integrates with [OpenCode](https://opencode.ai) - an AI-powered coding assistant that runs in your terminal.

## Features

- 🤖 **AI-powered code assistance** - Get intelligent code suggestions and explanations
- ⚡ **Code optimization** - Optimize your code with AI recommendations  
- 📖 **Code explanation** - Understand what your code does with detailed explanations
- 🎛️ **Configurable settings** - Customize server URL, AI model, and timeout settings
- 🔌 **Session-based API** - Efficient communication with OpenCode server
- ⌨️ **Keyboard shortcuts** - Quick access via `Ctrl+Alt+O` and `Ctrl+Alt+P`

## Prerequisites

- IntelliJ IDEA 2024.1 or later
- Java 17+

## Installation

### 1. Install Plugin

1. Download the latest plugin from [Releases](https://github.com/NaikSoftware/opencode-idea-plugin/releases)
2. In IntelliJ IDEA: `File` → `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
3. Select the downloaded `.zip` file
4. Restart IntelliJ IDEA

## Configuration

Configure the plugin in `File` → `Settings` → `Tools` → `OpenCode AI Assistant`:

- **Server Base URL**: `http://localhost:1993` (default)
- **Provider ID**: `anthropic` (default)  
- **Model ID**: `claude-3-5-sonnet-20241022` (default)
- **Timeout**: `120` seconds (default)

## Usage

### Keyboard Shortcuts

- `Ctrl+Alt+O` - Ask OpenCode AI about selected code
- `Ctrl+Alt+P` - Optimize selected code

### Menu Actions

Access via `Tools` → `OpenCode` menu:

- **Ask OpenCode AI** - Get assistance for selected code
- **Optimize Code** - Get optimization suggestions  
- **Explain Code** - Get detailed code explanations

### Tool Window

Access the OpenCode Assistant tool window from the right sidebar for interactive chat.

## API Integration

The plugin uses OpenCode's session-based API:

1. **Session Management** - Automatically creates and manages sessions
2. **Structured Requests** - Sends properly formatted requests with parts array
3. **Response Handling** - Extracts text content from structured responses
4. **Error Handling** - Robust error handling with user-friendly messages

### API Endpoints

- `POST /session` - Create new session
- `POST /session/{id}/message` - Send messages to session
- `GET /config` - Get server configuration

## Development

### Building the Plugin

```bash
cd plugin
./gradlew buildPlugin
```

The built plugin will be in `build/distributions/`.

### Running in Development

```bash
./gradlew runIde
```

### Testing

```bash
./gradlew test
```

## Project Structure

```
plugin/src/main/java/ua/naiksoftware/opencodeidea/
├── actions/           # IntelliJ actions (menu items, shortcuts)
├── config/            # Centralized configuration management  
├── model/             # Data models for OpenCode API
├── services/          # API communication services
├── settings/          # Plugin settings UI
└── ui/                # Tool window and UI components
```

## Key Components

- **OpenCodeConfig** - Centralized configuration with persistent storage
- **OpenCodeApiServiceImpl** - Session-based API integration  
- **Data Models** - OpenCodeSession, OpenCodeMessage, OpenCodeRequest
- **Settings Panel** - Configurable server URL, model, and timeout
- **Action Classes** - Code optimization, explanation, and general assistance

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- 🌐 [OpenCode Website](https://opencode.ai)
- 📚 [OpenCode Documentation](https://opencode.ai/docs)  
- 💬 [OpenCode Discord](https://opencode.ai/discord)
- 🐛 [Report Issues](https://github.com/NaikSoftware/opencode-idea-plugin/issues)

## Changelog

### v1.0.0 (Latest)

- ✅ Automatic OpenCode server startup and management
- ✅ Proper OpenCode session-based API integration
- ✅ Centralized configuration management
- ✅ Enhanced settings panel with multiple options
- ✅ Session management with automatic creation/caching
- ✅ Fixed package naming and build issues
- ✅ Comprehensive error handling and logging
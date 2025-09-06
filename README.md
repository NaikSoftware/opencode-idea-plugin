# OpenCode IntelliJ IDEA Plugin

An IntelliJ IDEA plugin that integrates with [OpenCode](https://opencode.ai) - an AI-powered coding assistant that runs in your terminal.

## Features

- 💬 **Chat-like Interface** - Modern conversational UI similar to ChatGPT/Claude
- 📝 **Markdown Support** - Full markdown rendering with syntax highlighting  
- 🤖 **AI Code Assistance** - Get intelligent code suggestions and explanations
- ⚡ **Code Optimization** - Optimize your code with AI recommendations  
- 📖 **Code Explanation** - Understand what your code does with detailed explanations
- 🔄 **Session Management** - Persistent conversations with automatic server management
- ⌨️ **Keyboard Shortcuts** - Quick access via `Ctrl+Alt+O` and `Ctrl+Alt+P`

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

Access the OpenCode Assistant tool window from the right sidebar for an interactive chat interface with markdown support and conversation history.

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

### Quick Start

```bash
cd plugin/
./dev.sh build    # Build plugin ZIP
./dev.sh run      # Test in IDE sandbox
./dev.sh dev      # Start hot reload development
```

### Development Commands

- `./dev.sh build` - Build plugin ZIP for distribution
- `./dev.sh run` - Launch IDE sandbox with plugin installed
- `./dev.sh dev` - Start continuous build with hot reload
- `./dev.sh debug` - Launch IDE with debugger (port 5005)
- `./dev.sh test` - Run all tests
- `./dev.sh clean` - Clean build directory

See [AGENTS.md](AGENTS.md) for comprehensive development guidelines.

## Architecture

```
plugin/src/main/java/ua/naiksoftware/opencodeidea/
├── actions/           # Menu items and keyboard shortcuts
├── config/            # Configuration management  
├── model/             # API data models
├── services/          # API communication and server management
├── settings/          # Plugin settings UI
└── ui/                # Chat interface and UI components
    ├── ChatInterface.java      # Main chat UI panel
    ├── ChatMessage.java        # Message data model  
    ├── ChatHistory.java        # Conversation management
    ├── ChatMessagePanel.java   # Message rendering with markdown
    └── MarkdownRenderer.java   # CommonMark-based rendering
```

## Key Features

- **Modern Chat UI** - Conversational interface with message bubbles
- **Markdown Rendering** - Full CommonMark support with syntax highlighting
- **Hot Reload Development** - Continuous builds for efficient development
- **Automatic Server Management** - Local OpenCode server lifecycle handling
- **Session-based API** - Persistent conversations with proper error handling

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

- ✅ **Modern Chat Interface** - ChatGPT/Claude-style conversation UI
- ✅ **Markdown Support** - Full rendering with syntax highlighting
- ✅ **Hot Reload Development** - Continuous builds for efficient development
- ✅ **Automatic Server Management** - Local OpenCode server lifecycle
- ✅ **Session-based API** - Persistent conversations with proper error handling
- ✅ **Development Tools** - Comprehensive build and testing scripts
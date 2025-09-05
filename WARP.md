# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is an IntelliJ IDEA plugin that integrates with OpenCode - an AI-powered coding assistant. The plugin provides code optimization, explanation, and general AI assistance through a session-based API connection to a local OpenCode server.

## Common Commands

### Plugin Development

```bash
# Build the plugin (creates ZIP in build/distributions/)
cd plugin
./gradlew buildPlugin

# Run IntelliJ IDEA with the plugin loaded for testing
./gradlew runIde

# Clean and rebuild
./gradlew clean buildPlugin

# Run tests
./gradlew test

# Run a specific test class
./gradlew test --tests "*TestClassName*"

# Verify plugin compatibility
./gradlew verifyPlugin
```

### OpenCode Server Setup

```bash
# Start OpenCode server using provided script
./start-opencode-server.sh

# Or start manually on specific port
./start-opencode-server.sh --port 8080 --hostname 0.0.0.0

### Development Testing

```bash
# Check if OpenCode server is running
curl http://localhost:1993/config

# Test session creation
curl -X POST http://localhost:1993/session -H "Content-Type: application/json" -d '{}'

# View API documentation
# Open http://localhost:1993/doc in browser
```

## Architecture

### High-Level Structure

The plugin follows a layered architecture:

```
UI Layer (Actions, Tool Windows, Settings)
    ↓
Service Layer (OpenCodeApiServiceImpl)
    ↓
Configuration Layer (OpenCodeConfig)
    ↓
HTTP API Client
    ↓
OpenCode Server (session-based REST API)
```

### Key Components

- **Actions** (`actions/`): Menu items and keyboard shortcuts for AI operations
  - `AskOpenCodeAction`: General AI assistance (Ctrl+Alt+O)
  - `ExplainCodeAction`: Code explanation

- **Services** (`services/`): Core API communication
  - `OpenCodeApiServiceImpl`: Manages HTTP sessions and async requests
  - Session lifecycle: create → send messages → handle responses

- **Configuration** (`config/`): Persistent settings management
  - `OpenCodeConfig`: Centralized configuration with XML persistence
  - Default server: `http://localhost:1993`
  - Configurable provider/model selection

- **UI Components** (`ui/`, `settings/`):
  - `OpenCodeToolWindowFactory`: Interactive chat interface
  - `OpenCodeConfigurable`: Settings panel integration

- **Data Models** (`model/`):
  - `OpenCodeSession`: Session state management
  - `OpenCodeRequest`/`OpenCodeMessage`: API request/response structures

### Session-Based Communication Flow

1. Plugin action triggered → Extract selected code/context
2. `OpenCodeApiServiceImpl` creates or reuses session
3. Sends structured request to `/session/{id}/message` endpoint
4. OpenCode server processes with AI provider (Claude, etc.)
5. Response streamed back and displayed in UI

### OpenCode Server Integration

The `opencode/` submodule contains the AI server implementation

## Configuration

### Plugin Settings

Configure via `File → Settings → Tools → OpenCode AI Assistant`:

- **Server Base URL**: `http://localhost:1993` (default)
- **Provider ID**: `anthropic` (default)
- **Model ID**: `claude-3-5-sonnet-20241022` (default)  
- **Timeout**: 120 seconds (default)

### Key Files

- `plugin.xml`: Plugin metadata, actions, and extensions
- `start-opencode-server.sh`: Server startup script with auto-detection
- `gradle.properties`: Build configuration and version settings

### API Integration Notes

The plugin uses OpenCode's session-based API (not the simplified `/api/v1/chat` endpoint mentioned in older docs):

- Sessions: `POST /session` → `POST /session/{id}/message` 
- Event streaming: `GET /event` (Server-Sent Events)
- Configuration: `GET /config` for provider/model info

### Development Requirements

- **Java**: 17+ (plugin target/source compatibility)
- **IntelliJ Platform**: 2024.2+ (build 241+)
- **Gradle**: 8.4+ (via wrapper)


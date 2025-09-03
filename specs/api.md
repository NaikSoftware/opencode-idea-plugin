# OpenCode API Specification

## Overview

OpenCode implements a client/server architecture where the server provides a REST API for AI-assisted coding operations. The server is built using Hono framework with Bun runtime and includes comprehensive OpenAPI documentation.

## Server Setup

### Starting the Server

```bash
# Start server on default port (auto-assigned)
opencode serve

# Start server on specific port and hostname
opencode serve --port 8080 --hostname 127.0.0.1
```

The server will output: `opencode server listening on http://127.0.0.1:<port>`

### OpenAPI Documentation

Access the OpenAPI specification at: `http://localhost:<port>/doc`

## Core API Endpoints

### Sessions

#### Create Session
```http
POST /session
Content-Type: application/json

{
  "parentID": "optional-parent-session-id",
  "title": "optional-session-title"
}
```

#### List Sessions
```http
GET /session
```

#### Get Session
```http
GET /session/{id}
```

#### Update Session
```http
PATCH /session/{id}
Content-Type: application/json

{
  "title": "new title"
}
```

#### Delete Session
```http
DELETE /session/{id}
```

### Messages

#### Send Prompt
```http
POST /session/{id}/message
Content-Type: application/json

{
  "message": "Your prompt here",
  "providerID": "anthropic",
  "modelID": "claude-3-5-sonnet-20241022"
}
```

#### Get Messages
```http
GET /session/{id}/message
```

#### Get Specific Message
```http
GET /session/{id}/message/{messageID}
```

### Files

#### List Files/Directories
```http
GET /file?path=/path/to/directory
```

#### Read File
```http
GET /file/content?path=/path/to/file
```

#### Get File Status
```http
GET /file/status
```

### Search

#### Find Text in Files
```http
GET /find?pattern=search-text
```

#### Find Files
```http
GET /find/file?query=file-pattern
```

#### Find Symbols
```http
GET /find/symbol?query=symbol-name
```

### Configuration

#### Get Config
```http
GET /config
```

#### List Providers
```http
GET /config/providers
```

### Commands

#### List Commands
```http
GET /command
```

### Events

#### Subscribe to Events (SSE)
```http
GET /event
```

## Client Integration

### Java/Kotlin Client Example

```kotlin
// Create HTTP client
val client = HttpClient.newBuilder().build()

// Create session
val createSessionRequest = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/session"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("{}"))
    .build()

val sessionResponse = client.send(createSessionRequest, HttpResponse.BodyHandlers.ofString())
val sessionId = // extract from response

// Send message
val messageRequest = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/session/$sessionId/message"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("""
        {
            "message": "Explain this code",
            "providerID": "anthropic",
            "modelID": "claude-3-5-sonnet-20241022"
        }
    """.trimIndent()))
    .build()

val messageResponse = client.send(messageRequest, HttpResponse.BodyHandlers.ofString())
```

## Current Plugin Integration Notes

The IntelliJ plugin currently uses a simplified API endpoint:
- URL: `http://localhost:8080/api/v1/chat`
- Method: POST
- Body: `{"message": "prompt + code"}`

However, the current server implementation uses session-based messaging. The plugin should be updated to:

1. Create/manage sessions via `/session` endpoints
2. Send messages via `/session/{id}/message`
3. Handle responses with proper session context

## Authentication

The API supports authentication via `/auth/{id}` endpoint for setting provider credentials.

## Error Handling

All endpoints return structured error responses:
```json
{
  "data": {
    "message": "error description",
    "code": "error_code"
  }
}
```

## WebSocket/Event Support

The server supports Server-Sent Events (SSE) for real-time updates via `/event` endpoint.

## Development Notes

- API uses Zod for schema validation
- OpenAPI specs are auto-generated using hono-openapi
- Server runs on Bun with zero idle timeout for long-running connections
- All endpoints support CORS and include proper OpenAPI documentation
#!/bin/bash

# OpenCode Server Startup Script
# Starts OpenCode as a headless HTTP server on port 1993

set -e

# Configuration
OPENCODE_PORT=${OPENCODE_PORT:-1993}
OPENCODE_HOSTNAME=${OPENCODE_HOSTNAME:-localhost}
OPENCODE_DIR="/Users/naik/IdeaProjects/opencode-intellij/opencode"
LOG_FILE="${HOME}/.opencode/server.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

echo_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

echo_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if OpenCode is installed
check_opencode_installation() {
    echo_info "Checking OpenCode installation..."
    
    if command -v opencode >/dev/null 2>&1; then
        echo_success "OpenCode CLI found in PATH"
        return 0
    fi
    
    echo_warning "OpenCode CLI not found in PATH, checking local installation..."
    
    if [ -d "$OPENCODE_DIR" ]; then
        echo_info "Found OpenCode source directory at $OPENCODE_DIR"
        
        # Check if bun is available
        if command -v bun >/dev/null 2>&1; then
            echo_success "Bun runtime found"
            return 1
        fi
        
        # Check if npm is available
        if command -v npm >/dev/null 2>&1; then
            echo_success "Node.js/npm found"
            return 2
        fi
        
        echo_error "Neither bun nor npm found. Please install one of them."
        return 3
    fi
    
    echo_error "OpenCode not found. Please install it first."
    echo_info "Install with: curl -fsSL https://opencode.ai/install | bash"
    return 3
}

# Function to check if port is available
check_port() {
    if lsof -Pi :$OPENCODE_PORT -sTCP:LISTEN -t >/dev/null; then
        echo_error "Port $OPENCODE_PORT is already in use"
        echo_info "Process using port $OPENCODE_PORT:"
        lsof -Pi :$OPENCODE_PORT -sTCP:LISTEN
        return 1
    fi
    echo_success "Port $OPENCODE_PORT is available"
    return 0
}

# Function to start OpenCode server with global installation
start_global_server() {
    echo_info "Starting OpenCode server (global installation)..."
    echo_info "Server will be available at: http://$OPENCODE_HOSTNAME:$OPENCODE_PORT"
    
    # Create log directory if it doesn't exist
    mkdir -p "$(dirname "$LOG_FILE")"
    
    # Start the server
    opencode serve --port "$OPENCODE_PORT" --hostname "$OPENCODE_HOSTNAME" 2>&1 | tee "$LOG_FILE"
}

# Function to start OpenCode server with bun (local development)
start_bun_server() {
    echo_info "Starting OpenCode server with bun..."
    echo_info "Server will be available at: http://$OPENCODE_HOSTNAME:$OPENCODE_PORT"
    
    cd "$OPENCODE_DIR"
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        echo_info "Installing dependencies..."
        bun install
    fi
    
    # Create log directory if it doesn't exist
    mkdir -p "$(dirname "$LOG_FILE")"
    
    # Set environment variables for server mode
    export OPENCODE_SERVER_PORT="$OPENCODE_PORT"
    export OPENCODE_SERVER_HOSTNAME="$OPENCODE_HOSTNAME"
    
    # Start the server with bun
    bun run dev serve --port "$OPENCODE_PORT" --hostname "$OPENCODE_HOSTNAME" 2>&1 | tee "$LOG_FILE"
}

# Function to start OpenCode server with npm (local development)
start_npm_server() {
    echo_info "Starting OpenCode server with npm..."
    echo_info "Server will be available at: http://$OPENCODE_HOSTNAME:$OPENCODE_PORT"
    
    cd "$OPENCODE_DIR"
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        echo_info "Installing dependencies..."
        npm install
    fi
    
    # Create log directory if it doesn't exist
    mkdir -p "$(dirname "$LOG_FILE")"
    
    # Set environment variables for server mode
    export OPENCODE_SERVER_PORT="$OPENCODE_PORT"
    export OPENCODE_SERVER_HOSTNAME="$OPENCODE_HOSTNAME"
    
    # Start the server with npm
    npm run dev serve --port "$OPENCODE_PORT" --hostname "$OPENCODE_HOSTNAME" 2>&1 | tee "$LOG_FILE"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -p, --port PORT       Port to run the server on (default: 1993)"
    echo "  -h, --hostname HOST   Hostname to bind to (default: localhost)"
    echo "  --help               Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  OPENCODE_PORT         Override default port"
    echo "  OPENCODE_HOSTNAME     Override default hostname"
    echo ""
    echo "Examples:"
    echo "  $0                    # Start server on localhost:1993"
    echo "  $0 -p 8080           # Start server on localhost:8080"
    echo "  $0 -h 0.0.0.0 -p 3000 # Start server on 0.0.0.0:3000"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--port)
            OPENCODE_PORT="$2"
            shift 2
            ;;
        -h|--hostname)
            OPENCODE_HOSTNAME="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Trap to handle cleanup on exit
cleanup() {
    echo_info "Shutting down OpenCode server..."
    # Kill any background processes
    jobs -p | xargs -r kill
    exit 0
}
trap cleanup SIGINT SIGTERM

# Main execution
main() {
    echo_info "Starting OpenCode Server"
    echo_info "Port: $OPENCODE_PORT"
    echo_info "Hostname: $OPENCODE_HOSTNAME"
    echo_info "Log file: $LOG_FILE"
    echo ""
    
    # Check if port is available
    if ! check_port; then
        exit 1
    fi
    
    # Check OpenCode installation and determine how to start
    check_opencode_installation
    installation_type=$?
    
    case $installation_type in
        0)
            # Global installation found
            start_global_server
            ;;
        1)
            # Local with bun
            start_bun_server
            ;;
        2)
            # Local with npm
            start_npm_server
            ;;
        3)
            # Not found
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
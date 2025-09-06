#!/bin/bash
# OpenCode IntelliJ Plugin Development Script

set -e

PLUGIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$PLUGIN_DIR"

case "${1:-help}" in
    "build")
        echo "🔨 Building plugin..."
        ./gradlew buildPlugin
        echo "✅ Plugin built: build/distributions/*.zip"
        ;;
    "run")
        echo "🚀 Starting IDE sandbox..."
        ./gradlew runIde
        ;;
    "debug")
        echo "🐛 Starting IDE with debugger (port 5005)..."
        echo "Attach your debugger to localhost:5005"
        ./gradlew runIde --debug-jvm
        ;;
    "test")
        echo "🧪 Running tests..."
        ./gradlew test
        ;;
    "check")
        echo "✅ Running all checks..."
        ./gradlew check
        ;;
    "clean")
        echo "🧹 Cleaning build..."
        ./gradlew clean
        ;;
    "dev"|"watch")
        echo "🔄 Starting continuous build (Ctrl+C to stop)..."
        echo "Watching for changes in src/..."
        ./gradlew -t classes --watch-fs
        ;;
    "verify")
        echo "🔍 Verifying plugin..."
        ./gradlew verifyPlugin
        ;;
    "dist")
        echo "📦 Building distribution..."
        ./gradlew clean buildPlugin
        echo "✅ Distribution ready: build/distributions/"
        ls -la build/distributions/
        ;;
    "help"|*)
        echo "OpenCode IntelliJ Plugin Development Commands"
        echo ""
        echo "Usage: $0 <command>"
        echo ""
        echo "Commands:"
        echo "  build    - Build the plugin ZIP"
        echo "  run      - Launch IDE sandbox with plugin"
        echo "  debug    - Launch IDE with debugger (port 5005)"
        echo "  test     - Run all tests"
        echo "  check    - Run all verification tasks"
        echo "  clean    - Clean build directory"
        echo "  dev      - Start continuous build (hot reload)"
        echo "  watch    - Alias for dev"
        echo "  verify   - Verify plugin structure"
        echo "  dist     - Build clean distribution"
        echo "  help     - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 dev     # Start development mode with hot reload"
        echo "  $0 run     # Test plugin in IDE"
        echo "  $0 debug   # Debug plugin (attach to port 5005)"
        echo "  $0 dist    # Build final distribution"
        ;;
esac
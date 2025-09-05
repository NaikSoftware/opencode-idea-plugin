#!/bin/bash

# OpenCode IntelliJ Plugin Builder
# This script builds the IntelliJ plugin and provides status information

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN_DIR="$SCRIPT_DIR/plugin"
BUILD_DIR="$PLUGIN_DIR/build/distributions"

echo -e "${BLUE}🔨 OpenCode IntelliJ Plugin Builder${NC}"
echo "======================================"

# Check if plugin directory exists
if [ ! -d "$PLUGIN_DIR" ]; then
    echo -e "${RED}❌ Error: Plugin directory not found at $PLUGIN_DIR${NC}"
    exit 1
fi

# Check if gradlew exists
if [ ! -f "$PLUGIN_DIR/gradlew" ]; then
    echo -e "${RED}❌ Error: Gradle wrapper not found at $PLUGIN_DIR/gradlew${NC}"
    exit 1
fi

echo -e "${BLUE}📁 Project Directory:${NC} $SCRIPT_DIR"
echo -e "${BLUE}🔧 Plugin Directory:${NC} $PLUGIN_DIR"
echo ""

# Clean previous builds if requested
if [ "$1" == "--clean" ] || [ "$1" == "-c" ]; then
    echo -e "${YELLOW}🧹 Cleaning previous build...${NC}"
    cd "$PLUGIN_DIR"
    ./gradlew clean
    echo ""
fi

echo -e "${BLUE}🏗️  Building plugin...${NC}"
cd "$PLUGIN_DIR"

# Build the plugin
if ./gradlew buildPlugin; then
    echo ""
    echo -e "${GREEN}✅ Build completed successfully!${NC}"
    
    # Check if build output exists
    if [ -d "$BUILD_DIR" ]; then
        echo ""
        echo -e "${BLUE}📦 Build Output:${NC}"
        ls -la "$BUILD_DIR"
        
        # Find the plugin ZIP file
        PLUGIN_ZIP=$(find "$BUILD_DIR" -name "*.zip" -type f | head -1)
        if [ -n "$PLUGIN_ZIP" ]; then
            PLUGIN_SIZE=$(du -h "$PLUGIN_ZIP" | cut -f1)
            PLUGIN_NAME=$(basename "$PLUGIN_ZIP")
            echo ""
            echo -e "${GREEN}🎉 Plugin ready for installation:${NC}"
            echo -e "${BLUE}   File:${NC} $PLUGIN_NAME"
            echo -e "${BLUE}   Size:${NC} $PLUGIN_SIZE"
            echo -e "${BLUE}   Path:${NC} $PLUGIN_ZIP"
            echo ""
            echo -e "${YELLOW}💡 To install in IntelliJ IDEA:${NC}"
            echo "   1. Go to File → Settings → Plugins"
            echo "   2. Click on the gear icon and select 'Install Plugin from Disk...'"
            echo "   3. Select: $PLUGIN_ZIP"
        fi
    else
        echo -e "${YELLOW}⚠️  Build completed but no output directory found${NC}"
    fi
else
    echo ""
    echo -e "${RED}❌ Build failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}🚀 Build process completed!${NC}"

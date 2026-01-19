#!/bin/bash

# Define image name
IMAGE_NAME="amazeing-builder"

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}🔍 Checking for Docker...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed or not in your PATH.${NC}"
    echo "Please install Docker Desktop: https://www.docker.com/products/docker-desktop/"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo -e "${RED}❌ Docker daemon is not running.${NC}"
    echo "Please start the Docker Desktop application."
    exit 1
fi

echo -e "${GREEN}🐳 Building Docker image '${IMAGE_NAME}'...${NC}"
docker build -t $IMAGE_NAME .

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Docker build failed.${NC}"
    exit 1
fi

echo -e "${GREEN}🏃 Running build inside Docker container...${NC}"

# Create output directory if it doesn't exist
mkdir -p docker-build-out

# Run container
# -rm: Remove container after exit
# -v: Volume mount to get the jar file out
docker run --rm -v "$(pwd)/docker-build-out:/app/desktop/build/libs" $IMAGE_NAME

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Build complete!${NC}"
    echo "Artifacts are available in: ./docker-build-out/"
    open ./docker-build-out 2>/dev/null || xdg-open ./docker-build-out 2>/dev/null || echo ""
else
    echo -e "${RED}❌ Build failed inside the container.${NC}"
    exit 1
fi

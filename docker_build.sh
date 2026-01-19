#!/bin/bash

# Define image name
IMAGE_NAME="amazeing-builder"

echo "🐳 Building Docker image..."
docker build -t $IMAGE_NAME .

echo "🏃 Running build inside Docker container..."
# We map the build/libs folder out so we can inspect artifacts if needed, 
# although mostly we just want to see if it succeeds.
# We also mount the source code again if we want to develop live, but for "Clean Build"
# we just run the container's CMD.
docker run --rm -v "$(pwd)/docker-build-out:/app/desktop/build/libs" $IMAGE_NAME

echo "✅ Build complete. Check console output for status."
echo "If the build succeeded, artifacts (if any) are in docker-build-out/"

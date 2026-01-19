@echo off
set IMAGE_NAME=amazeing-builder

echo 🔍 Checking for Docker...

where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker is not installed or not in your PATH.
    echo Please install Docker Desktop: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

docker info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker daemon is not running.
    echo Please start the Docker Desktop application.
    pause
    exit /b 1
)

echo 🐳 Building Docker image '%IMAGE_NAME%'...
docker build -t %IMAGE_NAME% .

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker build failed.
    pause
    exit /b 1
)

echo 🏃 Running build inside Docker container...

if not exist "docker-build-out" mkdir "docker-build-out"

REM Get absolute path for volume mount
set CURRENT_DIR=%cd%

docker run --rm -v "%CURRENT_DIR%/docker-build-out:/app/desktop/build/libs" %IMAGE_NAME%

if %ERRORLEVEL% EQU 0 (
    echo ✅ Build complete!
    echo Artifacts are available in: .\docker-build-out\
    start "" "docker-build-out"
) else (
    echo ❌ Build failed inside the container.
    pause
    exit /b 1
)

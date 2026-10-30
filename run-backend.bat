@echo off
setlocal enabledelayedexpansion
title Fixloop Backend Server

echo ========================================================
echo             FIXLOOP BACKEND AUTO-STARTUP
echo ========================================================
echo.

:: 1. Auto-detect Java if not already in environment
if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
        echo [INFO] Detected Java at: !JAVA_HOME!
    ) else (
        echo [WARN] JAVA_HOME is not set and default JBR path not found.
    )
) else (
    echo [INFO] Using JAVA_HOME: %JAVA_HOME%
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

:: 2. Ensure PostgreSQL Docker container is running
echo [INFO] Checking PostgreSQL database...
docker compose -f "%~dp0docker\docker-compose.yml" up -d
if %ERRORLEVEL% neq 0 (
    echo [WARN] Failed to start Docker container. Ensure Docker Desktop is running!
)

:: 3. Launch Spring Boot
echo.
echo [INFO] Starting Spring Boot on http://localhost:8080...
echo [INFO] Press Ctrl+C anytime to stop.
echo.
cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Backend stopped with an error code: %ERRORLEVEL%
    pause
)

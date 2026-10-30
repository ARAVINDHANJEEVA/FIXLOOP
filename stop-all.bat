@echo off
setlocal enabledelayedexpansion
title Fixloop Stop Services

echo ========================================================
echo             STOPPING ALL FIXLOOP SERVICES
echo ========================================================
echo.

:: 1. Stop Backend on port 8080
echo [1/3] Stopping Backend on port 8080...
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr /r "\<8080\>" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

:: 2. Stop Frontend on port 3000
echo [2/3] Stopping Frontend on port 3000...
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr /r "\<3000\>" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

:: 3. Stop PostgreSQL Docker Container
echo [3/3] Stopping PostgreSQL Docker container...
docker compose -f "%~dp0docker\docker-compose.yml" stop >nul 2>&1

echo.
echo ========================================================
echo  All Fixloop services (Backend, Frontend, DB) stopped!
echo ========================================================
timeout /t 3 >nul

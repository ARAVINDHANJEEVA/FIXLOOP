@echo off
setlocal enabledelayedexpansion
title Fixloop Full Stack Launcher

echo ========================================================
echo          FIXLOOP FULL-STACK AUTO LAUNCHER
echo ========================================================
echo.

:: 1. Auto-detect Java if not set
if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    )
)

:: 2. Ensure PostgreSQL Docker container is running
echo [1/3] Ensuring PostgreSQL container is running...
docker compose -f "%~dp0docker\docker-compose.yml" up -d
if %ERRORLEVEL% neq 0 (
    echo [WARN] Failed to start Docker container. Ensure Docker Desktop is running!
)

:: 3. Start Backend in a dedicated window
echo [2/3] Launching Spring Boot Backend in a separate window...
start "Fixloop Backend (Port 8080)" cmd /k "cd /d "%~dp0" && call run-backend.bat"

:: 4. Start Frontend in a dedicated window
echo [3/3] Launching React Frontend (Port 3000) in a separate window...
start "Fixloop Frontend (Port 3000)" cmd /k "cd /d "%~dp0web" && echo Starting Fixloop Web Portal... && npm start"

echo.
echo ========================================================
echo  All services have been initiated!
echo    - Backend:  http://localhost:8080
echo    - Frontend: http://localhost:3000
echo    - Database: localhost:5432 (fixloop_db)
echo.
echo  Default Credentials:
echo    - Admin: admin@fixloop.edu / admin123
echo    - Staff: staff@fixloop.edu / staff123
echo ========================================================
echo.
echo This launcher window will now close. Use the service windows to view logs.
timeout /t 5 >nul

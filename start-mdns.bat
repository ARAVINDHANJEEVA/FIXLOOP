@echo off
title FixLoop mDNS Publisher
color 0A

echo.
echo  ================================================
echo    FixLoop - IP-Resilient mDNS Setup (Version 2)
echo  ================================================
echo.
echo  This publishes your laptop as "fixloop.local"
echo  on the phone's hotspot network.
echo.
echo  The Android app will find this laptop even when
echo  the IP address changes — no reconfiguration needed.
echo.
echo  ================================================
echo.

:: Check if Python is available
python --version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] Python not found. Please install Python 3.x from:
    echo          https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
)

:: Install zeroconf if not already installed
echo  [STEP 1] Checking zeroconf library...
python -c "import zeroconf" >nul 2>&1
if errorlevel 1 (
    echo  [STEP 1] Installing zeroconf...
    pip install zeroconf
    if errorlevel 1 (
        echo.
        echo  [ERROR] Failed to install zeroconf. Try manually:
        echo          pip install zeroconf
        echo.
        pause
        exit /b 1
    )
    echo  [STEP 1] zeroconf installed successfully.
) else (
    echo  [STEP 1] zeroconf already installed. OK.
)

echo.
echo  [STEP 2] Starting mDNS publisher...
echo.

:: Run the publisher from the same directory as this batch file
cd /d "%~dp0"
python mdns_publisher.py

:: If we get here, the user pressed Ctrl+C
echo.
echo  mDNS publisher stopped.
pause

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "          FIXLOOP FULL-STACK AUTO LAUNCHER              " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Ensure JAVA_HOME is configured
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\Android\Android Studio\jbr\bin\java.exe") {
        $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    }
}
if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

# 2. Ensure PostgreSQL container is running
Write-Host "[1/3] Ensuring PostgreSQL container is running..." -ForegroundColor Cyan
docker compose -f "$PSScriptRoot\docker\docker-compose.yml" up -d

# 3. Launch Backend in separate window
Write-Host "[2/3] Launching Backend on http://localhost:8080..." -ForegroundColor Green
Start-Process cmd.exe -ArgumentList "/k cd /d `"$PSScriptRoot`" && call run-backend.bat"

# 4. Launch Frontend in separate window
Write-Host "[3/3] Launching Frontend on http://localhost:3000..." -ForegroundColor Green
Start-Process cmd.exe -ArgumentList "/k cd /d `"$PSScriptRoot\web`" && npm start"

Write-Host "`nAll services have been initiated!" -ForegroundColor Green
Write-Host "  - Frontend Portal: http://localhost:3000" -ForegroundColor White
Write-Host "  - Backend API:     http://localhost:8080" -ForegroundColor White
Write-Host "  - Database:        localhost:5432 (fixloop_db)" -ForegroundColor White
Write-Host "`nDefault Credentials:" -ForegroundColor Yellow
Write-Host "  - Admin: admin@fixloop.edu / admin123" -ForegroundColor Gray
Write-Host "  - Staff: staff@fixloop.edu / staff123" -ForegroundColor Gray

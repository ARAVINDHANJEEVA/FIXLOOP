Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "             FIXLOOP BACKEND AUTO-STARTUP               " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Ensure JAVA_HOME is configured
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\Android\Android Studio\jbr\bin\java.exe") {
        $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
        Write-Host "[INFO] Detected Java at: $env:JAVA_HOME" -ForegroundColor Green
    } else {
        Write-Host "[WARN] JAVA_HOME is not set." -ForegroundColor Yellow
    }
}
if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

# 2. Ensure PostgreSQL is up
Write-Host "[INFO] Ensuring PostgreSQL container is running..." -ForegroundColor Cyan
docker compose -f "$PSScriptRoot\docker\docker-compose.yml" up -d

# 3. Launch Spring Boot
Write-Host "[INFO] Starting Spring Boot on http://localhost:8080..." -ForegroundColor Green
Write-Host "[INFO] Press Ctrl+C anytime to stop.`n" -ForegroundColor DarkGray
Push-Location "$PSScriptRoot\backend"
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}

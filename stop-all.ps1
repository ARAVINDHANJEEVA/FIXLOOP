Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "             STOPPING ALL FIXLOOP SERVICES              " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Stop processes on port 8080 and 3000
Write-Host "[1/2] Terminating servers on ports 8080 and 3000..." -ForegroundColor Cyan
try {
    $connections = Get-NetTCPConnection -LocalPort 8080, 3000 -State Listen -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($pidToKill in $pids) {
            Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
        }
    }
} catch {
    # ignore if already down
}

# 2. Stop Docker container
Write-Host "[2/2] Stopping PostgreSQL container..." -ForegroundColor Cyan
docker compose -f "$PSScriptRoot\docker\docker-compose.yml" stop

Write-Host "`nAll Fixloop services have been stopped successfully!" -ForegroundColor Green

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$services = @(
    'config-service',
    'discovery-service',
    'gateway-service',
    'user-service',
    'complaint-service',
    'assignment-service',
    'notification-service',
    'statistics-service'
)

foreach ($service in $services) {
    Write-Host "`n===== Unit tests: $service =====" -ForegroundColor Cyan
    $servicePath = Join-Path $repositoryRoot $service
    Push-Location $servicePath
    try {
        & '.\gradlew.bat' test --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "$service unit tests failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

foreach ($service in @('notification-service', 'statistics-service')) {
    Write-Host "`n===== Kafka integration tests: $service =====" -ForegroundColor Cyan
    $servicePath = Join-Path $repositoryRoot $service
    Push-Location $servicePath
    try {
        & '.\gradlew.bat' integrationTest --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "$service Kafka integration tests failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host "`nAll unit and Kafka integration tests passed." -ForegroundColor Green

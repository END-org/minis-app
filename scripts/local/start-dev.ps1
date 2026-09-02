[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$modulePath = Join-Path $PSScriptRoot 'MinisLocal.psm1'
Import-Module $modulePath -Force

$repositoryRoot = Assert-MinisRepositoryRoot -RepositoryRoot (Get-Location).Path
Import-MinisEnvironment -RepositoryRoot $repositoryRoot -Required | Out-Null

$composeFile = Join-Path $repositoryRoot 'compose.local.yml'
$runtimeDirectory = Get-MinisRuntimeDirectory -RepositoryRoot $repositoryRoot
$backendDirectory = Join-Path $repositoryRoot 'backend\minis-backend'
$frontendDirectory = Join-Path $repositoryRoot 'web\event-app'
$backendPort = ConvertTo-MinisPort -Name 'SERVER_PORT' -Value (Get-MinisSetting -Name 'SERVER_PORT' -Fallback '8080')
$frontendPort = ConvertTo-MinisPort -Name 'MINIS_EVENT_APP_DEV_PORT' -Value (Get-MinisSetting -Name 'MINIS_EVENT_APP_DEV_PORT' -Fallback '5173')
$frontendHost = Get-MinisSetting -Name 'MINIS_EVENT_APP_DEV_HOST' -Fallback 'localhost'
$minioConsolePort = ConvertTo-MinisPort -Name 'MINIO_CONSOLE_PORT' -Value (Get-MinisSetting -Name 'MINIO_CONSOLE_PORT' -Fallback '9001')

if ($frontendHost -notmatch '^[A-Za-z0-9.-]+$') {
  throw 'MINIS_EVENT_APP_DEV_HOST must be a host name or IP address without URL punctuation.'
}
if (-not (Test-MinisDocker)) {
  throw 'Docker Desktop is not available. Start Docker and rerun this helper.'
}

Invoke-MinisExternal -Description 'Compose configuration validation' -Command {
  & docker compose -f $composeFile config --quiet
}
Invoke-MinisExternal -Description 'Local PostgreSQL and MinIO startup' -Command {
  & docker compose -f $composeFile up -d postgres minio
}
Wait-MinisCondition -Description 'PostgreSQL health' -Condition {
  $health = & docker inspect --format '{{.State.Health.Status}}' minis-postgres 2>$null
  return $LASTEXITCODE -eq 0 -and $health -eq 'healthy'
}
Wait-MinisCondition -Description 'MinIO health' -Condition {
  $health = & docker inspect --format '{{.State.Health.Status}}' minis-minio 2>$null
  return $LASTEXITCODE -eq 0 -and $health -eq 'healthy'
}
Invoke-MinisExternal -Description 'MinIO bucket initialization' -Command {
  & docker compose -f $composeFile run --rm minio-init
}

$backendRecord = Join-Path $runtimeDirectory 'backend.json'
$frontendRecord = Join-Path $runtimeDirectory 'frontend.json'
$trackedBackend = Get-MinisTrackedProcess -RecordPath $backendRecord
$trackedFrontend = Get-MinisTrackedProcess -RecordPath $frontendRecord
$startedBackend = $false
$startedFrontend = $false

if ($null -eq $trackedBackend) {
  if (-not (Test-MinisPortAvailable -Port $backendPort)) {
    throw "Port $backendPort is already in use by a process not tracked by MINIS. Resolve the collision without terminating unrelated processes."
  }
  $backendProcess = Start-Process -FilePath (Join-Path $backendDirectory 'mvnw.cmd') `
    -ArgumentList @('spring-boot:run', '-Dspring-boot.run.profiles=local') `
    -WorkingDirectory $backendDirectory `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $runtimeDirectory 'backend.log') `
    -RedirectStandardError (Join-Path $runtimeDirectory 'backend-error.log') `
    -PassThru
  Save-MinisProcessRecord -Process $backendProcess -Profile 'local' -RecordPath $backendRecord
  $startedBackend = $true
}

try {
  Wait-MinisCondition -Description 'backend readiness' -Condition {
    Test-MinisHttp -Url "http://localhost:$backendPort/actuator/health/readiness"
  }
} catch {
  if ($startedBackend) {
    Stop-MinisTrackedProcess -RecordPath $backendRecord | Out-Null
  }
  throw "Backend did not become ready. Inspect $runtimeDirectory\\backend.log and $runtimeDirectory\\backend-error.log."
}

if ($null -eq $trackedFrontend) {
  if (-not (Test-MinisPortAvailable -Port $frontendPort)) {
    throw "Port $frontendPort is already in use by a process not tracked by MINIS. Resolve the collision without terminating unrelated processes."
  }
  $frontendCommand = "npm.cmd run dev -- --host $frontendHost --port $frontendPort"
  $frontendProcess = Start-Process -FilePath 'cmd.exe' `
    -ArgumentList @('/d', '/c', $frontendCommand) `
    -WorkingDirectory $frontendDirectory `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $runtimeDirectory 'frontend.log') `
    -RedirectStandardError (Join-Path $runtimeDirectory 'frontend-error.log') `
    -PassThru
  Save-MinisProcessRecord -Process $frontendProcess -Profile 'local' -RecordPath $frontendRecord
  $startedFrontend = $true
}

try {
  Wait-MinisCondition -Description 'React event app response' -Condition {
    Test-MinisHttp -Url "http://${frontendHost}:$frontendPort/"
  }
} catch {
  if ($startedFrontend) {
    Stop-MinisTrackedProcess -RecordPath $frontendRecord | Out-Null
  }
  throw "React did not respond. Inspect $runtimeDirectory\\frontend.log and $runtimeDirectory\\frontend-error.log."
}

Write-Host ''
Write-Host 'MINIS local development is ready:'
Write-Host "  React:   http://${frontendHost}:$frontendPort/"
Write-Host "  API:     http://localhost:$backendPort/actuator/health/readiness"
Write-Host "  Swagger: http://localhost:$backendPort/swagger-ui/index.html"
Write-Host "  MinIO:   http://localhost:$minioConsolePort/"
Write-Host 'Stop with .\scripts\local\stop-dev.ps1 from the repository root.'

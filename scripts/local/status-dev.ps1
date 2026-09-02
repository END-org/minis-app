[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$modulePath = Join-Path $PSScriptRoot 'MinisLocal.psm1'
Import-Module $modulePath -Force

$repositoryRoot = Assert-MinisRepositoryRoot -RepositoryRoot (Get-Location).Path
Import-MinisEnvironment -RepositoryRoot $repositoryRoot | Out-Null

$composeFile = Join-Path $repositoryRoot 'compose.local.yml'
$runtimeDirectory = Get-MinisRuntimeDirectory -RepositoryRoot $repositoryRoot
$backendPort = ConvertTo-MinisPort -Name 'SERVER_PORT' -Value (Get-MinisSetting -Name 'SERVER_PORT' -Fallback '8080')
$frontendPort = ConvertTo-MinisPort -Name 'MINIS_EVENT_APP_DEV_PORT' -Value (Get-MinisSetting -Name 'MINIS_EVENT_APP_DEV_PORT' -Fallback '5173')
$frontendHost = Get-MinisSetting -Name 'MINIS_EVENT_APP_DEV_HOST' -Fallback 'localhost'
$backendProcess = Get-MinisTrackedProcess -RecordPath (Join-Path $runtimeDirectory 'backend.json')
$frontendProcess = Get-MinisTrackedProcess -RecordPath (Join-Path $runtimeDirectory 'frontend.json')

Write-Host "Repository: $repositoryRoot"
Write-Host "Docker: $(if (Test-MinisDocker) { 'available' } else { 'unavailable' })"
if (Test-MinisDocker) {
  & docker compose -f $composeFile ps
}

if ($null -ne $backendProcess) {
  Write-Host "Backend process: tracked PID $($backendProcess.Process.Id), profile $($backendProcess.Record.profile)"
} else {
  Write-Host 'Backend process: not tracked by the MINIS helper'
}
if ($null -ne $frontendProcess) {
  Write-Host "React process: tracked PID $($frontendProcess.Process.Id), profile $($frontendProcess.Record.profile)"
} else {
  Write-Host 'React process: not tracked by the MINIS helper'
}

Write-Host "Backend readiness: $(if (Test-MinisHttp -Url "http://localhost:$backendPort/actuator/health/readiness") { 'reachable' } else { 'unreachable' })"
Write-Host "React response: $(if (Test-MinisHttp -Url "http://${frontendHost}:$frontendPort/") { 'reachable' } else { 'unreachable' })"
Write-Host "Feed response: $(if (Test-MinisHttp -Url "http://localhost:$backendPort/api/v1/feed?limit=1") { 'reachable' } else { 'unreachable' })"
Write-Host "Swagger UI: $(if (Test-MinisHttp -Url "http://localhost:$backendPort/swagger-ui/index.html") { 'reachable' } else { 'unreachable' })"

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$modulePath = Join-Path $PSScriptRoot 'MinisLocal.psm1'
Import-Module $modulePath -Force

$repositoryRoot = Assert-MinisRepositoryRoot -RepositoryRoot (Get-Location).Path
$runtimeDirectory = Get-MinisRuntimeDirectory -RepositoryRoot $repositoryRoot
$frontendStopped = Stop-MinisTrackedProcess -RecordPath (Join-Path $runtimeDirectory 'frontend.json')
$backendStopped = Stop-MinisTrackedProcess -RecordPath (Join-Path $runtimeDirectory 'backend.json')

Write-Host "React helper process stopped: $frontendStopped"
Write-Host "Backend helper process stopped: $backendStopped"

if (Test-MinisDocker) {
  $composeFile = Join-Path $repositoryRoot 'compose.local.yml'
  Invoke-MinisExternal -Description 'Local Compose shutdown' -Command {
    & docker compose -f $composeFile down
  }
  Write-Host 'Local PostgreSQL and MinIO containers stopped. Named data volumes were preserved.'
} else {
  Write-Host 'Docker is unavailable, so local Compose services were left untouched.'
}

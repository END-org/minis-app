[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$modulePath = Join-Path $PSScriptRoot 'MinisLocal.psm1'
Import-Module $modulePath -Force

$repositoryRoot = Assert-MinisRepositoryRoot -RepositoryRoot (Get-Location).Path
$composeFile = Join-Path $repositoryRoot 'compose.local.yml'
$backendDirectory = Join-Path $repositoryRoot 'backend\minis-backend'
$frontendDirectory = Join-Path $repositoryRoot 'web\event-app'

Invoke-MinisExternal -Description 'Compose configuration validation' -Command {
  & docker compose -f $composeFile config --quiet
}

Push-Location $backendDirectory
try {
  Invoke-MinisExternal -Description 'Backend Spotless formatting' -Command {
    & .\mvnw.cmd spotless:apply
  }
  Invoke-MinisExternal -Description 'Backend verification' -Command {
    & .\mvnw.cmd --batch-mode --no-transfer-progress verify
  }
} finally {
  Pop-Location
}

Push-Location $frontendDirectory
try {
  Invoke-MinisExternal -Description 'Frontend dependency install' -Command {
    & cmd.exe /d /c npm.cmd ci
  }
  Invoke-MinisExternal -Description 'Frontend lint' -Command {
    & cmd.exe /d /c npm.cmd run lint
  }
  Invoke-MinisExternal -Description 'Frontend typecheck' -Command {
    & cmd.exe /d /c npm.cmd run typecheck
  }
  Invoke-MinisExternal -Description 'Frontend tests' -Command {
    & cmd.exe /d /c npm.cmd run test
  }
  Invoke-MinisExternal -Description 'Frontend build' -Command {
    & cmd.exe /d /c npm.cmd run build
  }
} finally {
  Pop-Location
}

Invoke-MinisExternal -Description 'Git whitespace validation' -Command {
  & git diff --check
}

Write-Host 'All MINIS verification commands completed successfully.'

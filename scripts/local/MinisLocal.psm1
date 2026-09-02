Set-StrictMode -Version Latest

function Assert-MinisRepositoryRoot {
  param(
    [Parameter(Mandatory)]
    [string]$RepositoryRoot
  )

  $expectedRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
  $currentRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
  if ($currentRoot -ne $expectedRoot) {
    throw "Run this helper from $expectedRoot."
  }
  return $expectedRoot
}

function Import-MinisEnvironment {
  param(
    [Parameter(Mandatory)]
    [string]$RepositoryRoot,
    [switch]$Required
  )

  $environmentFile = Join-Path $RepositoryRoot '.env'
  if (-not (Test-Path -LiteralPath $environmentFile -PathType Leaf)) {
    if ($Required) {
      throw "Missing $environmentFile. Copy .env.example to .env and fill the required local values."
    }
    return $false
  }

  foreach ($rawLine in Get-Content -LiteralPath $environmentFile) {
    $line = $rawLine.Trim()
    if (-not $line -or $line.StartsWith('#')) {
      continue
    }

    $separator = $line.IndexOf('=')
    if ($separator -lt 1) {
      throw "Invalid .env entry. Expected NAME=VALUE."
    }

    $name = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()
    if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
      throw "Invalid environment variable name in .env."
    }

    if ($value.Length -ge 2) {
      $first = $value[0]
      $last = $value[$value.Length - 1]
      if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }

    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  }
  return $true
}

function Get-MinisSetting {
  param(
    [Parameter(Mandatory)]
    [string]$Name,
    [Parameter(Mandatory)]
    [string]$Fallback
  )

  $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
  if ([string]::IsNullOrWhiteSpace($value)) {
    return $Fallback
  }
  return $value.Trim()
}

function ConvertTo-MinisPort {
  param(
    [Parameter(Mandatory)]
    [string]$Value,
    [Parameter(Mandatory)]
    [string]$Name
  )

  $port = 0
  if (-not [int]::TryParse($Value, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
    throw "$Name must be a valid TCP port."
  }
  return $port
}

function Get-MinisRuntimeDirectory {
  param(
    [Parameter(Mandatory)]
    [string]$RepositoryRoot
  )

  $runtimeDirectory = Join-Path $RepositoryRoot '.minis-local'
  if (-not (Test-Path -LiteralPath $runtimeDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
  }
  return $runtimeDirectory
}

function Get-MinisTrackedProcess {
  param(
    [Parameter(Mandatory)]
    [string]$RecordPath
  )

  if (-not (Test-Path -LiteralPath $RecordPath -PathType Leaf)) {
    return $null
  }

  try {
    $record = Get-Content -LiteralPath $RecordPath -Raw | ConvertFrom-Json
    $process = Get-Process -Id ([int]$record.pid) -ErrorAction Stop
    $actualStartTime = $process.StartTime.ToUniversalTime().ToFileTimeUtc()
    if ([int64]$record.startedAtFileTime -ne $actualStartTime) {
      Remove-Item -LiteralPath $RecordPath -Force
      return $null
    }
    return [PSCustomObject]@{
      Process = $process
      Record = $record
    }
  } catch {
    Remove-Item -LiteralPath $RecordPath -Force -ErrorAction SilentlyContinue
    return $null
  }
}

function Save-MinisProcessRecord {
  param(
    [Parameter(Mandatory)]
    [System.Diagnostics.Process]$Process,
    [Parameter(Mandatory)]
    [string]$RecordPath,
    [Parameter(Mandatory)]
    [string]$Profile
  )

  $record = [PSCustomObject]@{
    pid = $Process.Id
    profile = $Profile
    startedAtFileTime = $Process.StartTime.ToUniversalTime().ToFileTimeUtc()
  }
  $record | ConvertTo-Json | Set-Content -LiteralPath $RecordPath -Encoding utf8
}

function Stop-MinisTrackedProcess {
  param(
    [Parameter(Mandatory)]
    [string]$RecordPath
  )

  $trackedProcess = Get-MinisTrackedProcess -RecordPath $RecordPath
  if ($null -eq $trackedProcess) {
    return $false
  }

  $descendantIds = Get-MinisDescendantProcessIds -ParentProcessId $trackedProcess.Process.Id
  foreach ($descendantId in $descendantIds) {
    Stop-Process -Id $descendantId -ErrorAction SilentlyContinue
  }
  Stop-Process -Id $trackedProcess.Process.Id -ErrorAction Stop
  Remove-Item -LiteralPath $RecordPath -Force -ErrorAction SilentlyContinue
  return $true
}

function Get-MinisDescendantProcessIds {
  param(
    [Parameter(Mandatory)]
    [int]$ParentProcessId
  )

  $descendantIds = [System.Collections.Generic.List[int]]::new()
  $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ParentProcessId" -ErrorAction SilentlyContinue
  foreach ($child in $children) {
    foreach ($descendantId in Get-MinisDescendantProcessIds -ParentProcessId ([int]$child.ProcessId)) {
      $descendantIds.Add($descendantId)
    }
    $descendantIds.Add([int]$child.ProcessId)
  }
  return $descendantIds
}

function Test-MinisPortAvailable {
  param(
    [Parameter(Mandatory)]
    [int]$Port
  )

  $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
    Select-Object -First 1
  return $null -eq $listener
}

function Wait-MinisCondition {
  param(
    [Parameter(Mandatory)]
    [string]$Description,
    [Parameter(Mandatory)]
    [scriptblock]$Condition,
    [int]$TimeoutSeconds = 90
  )

  $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
  do {
    if (& $Condition) {
      return
    }
    Start-Sleep -Seconds 2
  } while ([DateTime]::UtcNow -lt $deadline)

  throw "Timed out waiting for $Description."
}

function Test-MinisHttp {
  param(
    [Parameter(Mandatory)]
    [string]$Url
  )

  try {
    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    return $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
  } catch {
    return $false
  }
}

function Test-MinisDocker {
  & docker info --format '{{.ServerVersion}}' *> $null
  return $LASTEXITCODE -eq 0
}

function Invoke-MinisExternal {
  param(
    [Parameter(Mandatory)]
    [string]$Description,
    [Parameter(Mandatory)]
    [scriptblock]$Command
  )

  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "$Description failed with exit code $LASTEXITCODE."
  }
}

Export-ModuleMember -Function @(
  'Assert-MinisRepositoryRoot',
  'ConvertTo-MinisPort',
  'Get-MinisRuntimeDirectory',
  'Get-MinisSetting',
  'Get-MinisTrackedProcess',
  'Import-MinisEnvironment',
  'Invoke-MinisExternal',
  'Save-MinisProcessRecord',
  'Stop-MinisTrackedProcess',
  'Test-MinisDocker',
  'Test-MinisHttp',
  'Test-MinisPortAvailable',
  'Wait-MinisCondition'
)

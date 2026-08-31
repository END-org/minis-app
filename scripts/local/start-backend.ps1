[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$envFile = Join-Path $repoRoot '.env'
$backendDirectory = Join-Path $repoRoot 'backend\minis-backend'

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
  throw "Missing $envFile. Copy .env.example to .env and fill the required local values."
}

foreach ($rawLine in Get-Content -LiteralPath $envFile) {
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

Push-Location $backendDirectory
try {
  & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
  $exitCode = $LASTEXITCODE
} finally {
  Pop-Location
}

exit $exitCode

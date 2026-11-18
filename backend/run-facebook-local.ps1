$ErrorActionPreference = "Stop"

function Test-TcpPort {
  param([string]$HostName, [int]$Port)
  $client = [System.Net.Sockets.TcpClient]::new()
  try {
    $result = $client.BeginConnect($HostName, $Port, $null, $null)
    if (-not $result.AsyncWaitHandle.WaitOne(1000)) { return $false }
    $client.EndConnect($result)
    return $true
  }
  catch { return $false }
  finally { $client.Dispose() }
}

if (-not (Test-TcpPort -HostName "127.0.0.1" -Port 5432)) {
  Write-Host "PostgreSQL is not running on port 5432. Starting Docker service postgres..." -ForegroundColor Yellow
  $composeFile = Join-Path $PSScriptRoot "..\docker-compose.yml"
  & docker compose -f $composeFile up -d postgres
  if ($LASTEXITCODE -ne 0) {
    throw "Cannot start PostgreSQL. Open Docker Desktop, then run this script again."
  }
  $databaseReady = $false
  for ($attempt = 1; $attempt -le 30; $attempt++) {
    Start-Sleep -Seconds 2
    if (Test-TcpPort -HostName "127.0.0.1" -Port 5432) {
      $databaseReady = $true
      break
    }
  }
  if (-not $databaseReady) { throw "PostgreSQL did not open port 5432 within 60 seconds." }
}

$env:DB_URL = "jdbc:postgresql://127.0.0.1:5432/core_platform"
$env:DB_USER = "core_app"
$env:DB_PASSWORD = "core_app_dev"
$env:DB_MIGRATION_USER = "core_admin"
$env:DB_MIGRATION_PASSWORD = "core_admin_dev"

function Read-ValidatedSecret {
  param(
    [Parameter(Mandatory = $true)][string]$Prompt,
    [Parameter(Mandatory = $true)][string]$Pattern,
    [Parameter(Mandatory = $true)][string]$ErrorMessage
  )
  $secureValue = Read-Host $Prompt -AsSecureString
  $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
  try {
    $value = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer).Trim()
    if ($value -notmatch $Pattern) { throw $ErrorMessage }
    return $value
  }
  finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
  }
}

$pageToken = Read-ValidatedSecret `
  -Prompt "Dan PAGE ACCESS TOKEN moi cua Fanpage roi nhan Enter" `
  -Pattern '^EAA[A-Za-z0-9_-]{40,}$' `
  -ErrorMessage "Page Access Token khong dung dinh dang. Token phai bat dau bang EAA va khong chua khoang trang."

$env:FACEBOOK_PAGE_ID = "101327182988385"
$env:FACEBOOK_PAGE_NAME = "HarmonyAquamid"
$env:FACEBOOK_PAGE_ACCESS_TOKEN = $pageToken
$env:FACEBOOK_WEBHOOK_VERIFY_TOKEN = [guid]::NewGuid().ToString("N")

Write-Host ""
Write-Host "Facebook local configuration is ready:" -ForegroundColor Green
Write-Host "  Page: HarmonyAquamid (101327182988385)"
Write-Host "  Page token format: OK"
Write-Host "  Mode: Graph API polling (webhook disabled)"
Write-Host "  Starting Spring Boot on port 8080..."
Write-Host ""

Push-Location $PSScriptRoot
try { & .\mvnw.cmd spring-boot:run }
finally {
  Pop-Location
  Remove-Item Env:FACEBOOK_PAGE_ACCESS_TOKEN -ErrorAction SilentlyContinue
  Remove-Item Env:FACEBOOK_WEBHOOK_VERIFY_TOKEN -ErrorAction SilentlyContinue
}

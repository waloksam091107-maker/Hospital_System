[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$TargetDatabase,
    [Parameter(Mandatory = $true)][string]$ConfirmTarget,
    [Parameter(Mandatory = $true)][string]$InputFile,
    [Parameter(Mandatory = $true)][string]$ServerHost,
    [Parameter(Mandatory = $true)][ValidateRange(1,65535)][int]$Port,
    [Parameter(Mandatory = $true)][string]$User,
    [string]$ClientPath = 'obclient.exe',
    [switch]$ValidateOnly
)
. (Join-Path $PSScriptRoot 'operations-common.ps1')
Assert-RecoveryTarget $TargetDatabase $ConfirmTarget
$resolved = (Resolve-Path -LiteralPath $InputFile).Path
if ([IO.Path]::GetExtension($resolved) -ne '.sql') { throw 'Restore input must be a SQL dump.' }
$checksumFile = $resolved + '.sha256'
if (-not (Test-Path -LiteralPath $checksumFile)) { throw 'The backup SHA256 sidecar is required.' }
$expectedHash = [IO.File]::ReadAllText($checksumFile).Trim()
if ($expectedHash -notmatch '^[A-Fa-f0-9]{64}$' -or (Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash -ine $expectedHash) { throw 'Backup checksum mismatch.' }
Assert-SingleDatabaseDump ([IO.File]::ReadAllText($resolved, [Text.Encoding]::UTF8))
if ($ValidateOnly) { Write-Output 'Restore target, checksum and SQL valid; no connection made.'; return }
$password = Get-OperationsPassword
try {
    $connection = @(('--host=' + $ServerHost), ('--port=' + $Port), ('--user=' + $User), '--default-character-set=utf8mb4', "--init-command=SET SESSION sql_mode='NO_AUTO_VALUE_ON_ZERO'", '--batch', '--skip-column-names', ('--database=' + $TargetDatabase))
    $actual = Invoke-OperationsClient $ClientPath ($connection + @('--execute=SELECT DATABASE()')) $password
    if ($actual.Trim() -cne $TargetDatabase) { throw 'Connected database does not match the explicit restore target.' }
    $tables = Invoke-OperationsClient $ClientPath ($connection + @('--execute=SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()')) $password
    if ($tables.Trim() -ne '0') { throw 'Restore requires an existing empty recovery database. No existing tables are overwritten.' }
    Invoke-OperationsClient $ClientPath $connection $password $resolved | Out-Null
    Write-Output ('Restored into ' + $TargetDatabase + '. Validate counts and business invariants before any separate cutover.')
} finally { $password = $null }

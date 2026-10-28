[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$TargetDatabase,
    [Parameter(Mandatory = $true)][string]$ConfirmTarget,
    [switch]$MaintenanceMode,
    [string]$JavaPath = 'java.exe',
    [string]$RuntimeClasspath = '',
    [switch]$ValidateOnly
)
. (Join-Path $PSScriptRoot 'operations-common.ps1')
Assert-DatabaseName $TargetDatabase
if ($TargetDatabase -cne $ConfirmTarget) { throw 'The upgrade target and exact confirmation must match.' }
if ($TargetDatabase -in @('mysql','information_schema','oceanbase','sys','__recyclebin')) { throw 'System databases cannot be upgraded.' }
if (-not $MaintenanceMode) { throw 'MaintenanceMode is required: stop all old and new application writers and scheduled jobs before upgrading.' }
$upgradeUrl = $env:HOSPITAL_UPGRADE_DB_URL
$upgradeUser = $env:HOSPITAL_UPGRADE_DB_USERNAME
$targetPattern = '^jdbc:oceanbase://[^/]+/' + [regex]::Escape($TargetDatabase) + '(?:\?.*)?$'
if ([string]::IsNullOrWhiteSpace($upgradeUrl) -or $upgradeUrl -cnotmatch $targetPattern) { throw 'HOSPITAL_UPGRADE_DB_URL must select the explicitly confirmed OceanBase database.' }
if ([string]::IsNullOrWhiteSpace($upgradeUser)) { throw 'Set HOSPITAL_UPGRADE_DB_USERNAME in the environment.' }
if ($ValidateOnly) { Write-Output 'Upgrade maintenance confirmation, target and URL valid; no connection made.'; return }
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $RuntimeClasspath) {
    $classes = Join-Path $projectRoot 'target/classes'
    $upgradeClass = Join-Path $classes 'com/ustb/hospital/database/ExtensionSchemaUpgrade.class'
    $upgradeTables = Join-Path $classes 'db/five-extensions-tables.sql'
    $libraries = Join-Path $projectRoot 'target/ustbhospital/WEB-INF/lib'
    if (-not (Test-Path -LiteralPath $upgradeClass) -or -not (Test-Path -LiteralPath $upgradeTables) -or -not (Test-Path -LiteralPath $libraries)) { throw 'Build and verify the new WAR first; required upgrade classes/resources/runtime libraries are missing.' }
    $driver = @(Get-ChildItem -LiteralPath $libraries -File -Filter 'oceanbase-client-*.jar')
    if ($driver.Count -ne 1) { throw 'Exactly one packaged OceanBase JDBC runtime driver is required.' }
    $RuntimeClasspath = $classes + [IO.Path]::PathSeparator + $driver[0].FullName
}
$upgradePassword = $env:HOSPITAL_UPGRADE_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($upgradePassword)) { $upgradePassword = Get-OperationsPassword }
$process = $null
try {
    $java = Get-Command $JavaPath -ErrorAction Stop
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $java.Source
    $start.Arguments = '-cp ' + (ConvertTo-NativeArgument $RuntimeClasspath) + ' com.ustb.hospital.database.ExtensionSchemaUpgrade'
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.StandardOutputEncoding = New-Object Text.UTF8Encoding($false)
    $start.StandardErrorEncoding = New-Object Text.UTF8Encoding($false)
    $start.EnvironmentVariables['HOSPITAL_UPGRADE_TARGET'] = $TargetDatabase
    $start.EnvironmentVariables['HOSPITAL_UPGRADE_CONFIRM_TARGET'] = $ConfirmTarget
    $start.EnvironmentVariables['HOSPITAL_UPGRADE_MAINTENANCE'] = 'true'
    $start.EnvironmentVariables['HOSPITAL_UPGRADE_DB_URL'] = $upgradeUrl
    $start.EnvironmentVariables['HOSPITAL_UPGRADE_DB_USERNAME'] = $upgradeUser
    $start.EnvironmentVariables['HOSPITAL_UPGRADE_DB_PASSWORD'] = $upgradePassword
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $start
    [void]$process.Start()
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    if ($stdout.Result.Trim()) { Write-Output $stdout.Result.Trim() }
    if ($stderr.Result.Trim()) { Write-Output $stderr.Result.Trim() }
    if ($process.ExitCode -ne 0) { throw 'Upgrade stopped. Keep every application writer stopped, inspect the preflight diagnostics and persisted phase, then retry this same target after correcting the cause.' }
} finally {
    if ($null -ne $process) { $process.Dispose() }
    $upgradePassword = $null
}

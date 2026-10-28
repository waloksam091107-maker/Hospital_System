$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$wrapper = Join-Path $repo 'scripts/oceanbase/upgrade-five-extensions.ps1'
if (-not (Test-Path -LiteralPath $wrapper)) { throw 'The guarded maintenance upgrade wrapper must exist.' }
$previousUrl = $env:HOSPITAL_UPGRADE_DB_URL
$previousUser = $env:HOSPITAL_UPGRADE_DB_USERNAME
$previousPassword = $env:HOSPITAL_UPGRADE_DB_PASSWORD
$powershell5 = Join-Path $env:WINDIR 'System32/WindowsPowerShell/v1.0/powershell.exe'
try {
    $env:HOSPITAL_UPGRADE_DB_URL = 'jdbc:oceanbase://127.0.0.1:12881/hospital_test_upgrade?serverTimezone=Asia/Shanghai'
    $env:HOSPITAL_UPGRADE_DB_USERNAME = 'upgrade-test-user'
    $env:HOSPITAL_UPGRADE_DB_PASSWORD = 'never-display-this-test-password'
    $common = @('-NoProfile','-ExecutionPolicy','Bypass','-File',$wrapper,'-TargetDatabase','hospital_test_upgrade','-ConfirmTarget','hospital_test_upgrade','-ValidateOnly')
    $output = (& $powershell5 @common -MaintenanceMode 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $output -notmatch 'no connection made') { throw ('Explicit safe validation failed: ' + $output) }
    if ($output.Contains($env:HOSPITAL_UPGRADE_DB_PASSWORD) -or $output.Contains($env:HOSPITAL_UPGRADE_DB_USERNAME)) { throw 'Upgrade validation exposed credentials.' }
    $ErrorActionPreference = 'Continue'
    & $powershell5 @common 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { throw 'Upgrade accepted missing maintenance confirmation.' }
    $badConfirm = @('-NoProfile','-ExecutionPolicy','Bypass','-File',$wrapper,'-TargetDatabase','hospital_test_upgrade','-ConfirmTarget','different','-MaintenanceMode','-ValidateOnly')
    & $powershell5 @badConfirm 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { throw 'Upgrade accepted mismatched target confirmation.' }
    $env:HOSPITAL_UPGRADE_DB_URL = 'jdbc:oceanbase://127.0.0.1:12881/hospt'
    & $powershell5 @common -MaintenanceMode 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { throw 'Upgrade accepted a URL selecting a different database.' }
    $ErrorActionPreference = 'Stop'
    Write-Output 'PASS Windows PowerShell 5.1 maintenance, target confirmation, URL scope, credential redaction; no connection made.'
} finally {
    $env:HOSPITAL_UPGRADE_DB_URL = $previousUrl
    $env:HOSPITAL_UPGRADE_DB_USERNAME = $previousUser
    $env:HOSPITAL_UPGRADE_DB_PASSWORD = $previousPassword
}

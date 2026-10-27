[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Database,
    [Parameter(Mandatory = $true)][string]$ServerHost,
    [Parameter(Mandatory = $true)][ValidateRange(1,65535)][int]$Port,
    [Parameter(Mandatory = $true)][string]$User,
    [string]$DumpPath = 'mysqldump.exe',
    [string]$ClientPath = 'obclient.exe',
    [switch]$ValidateOnly
)
. (Join-Path $PSScriptRoot 'operations-common.ps1')
Assert-DatabaseName $Database
if ($ValidateOnly) { Write-Output 'Backup arguments valid; no connection made.'; return }
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$directory = Join-Path $projectRoot 'database\oceanbase\export\backups'
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$name = '{0}-{1}-{2}.sql' -f $Database, (Get-Date -Format 'yyyyMMdd-HHmmss'), ([guid]::NewGuid().ToString('N'))
$dump = Join-Path $directory $name
$partial = $dump + '.partial'
$password = Get-OperationsPassword
try {
    $connection = @(( '--host=' + $ServerHost), ('--port=' + $Port), ('--user=' + $User), '--default-character-set=utf8mb4')
    $actual = Invoke-OperationsClient $ClientPath ($connection + @('--batch','--skip-column-names',('--database=' + $Database),'--execute=SELECT DATABASE()')) $password
    if ($actual.Trim() -cne $Database) { throw 'Connected database does not match the explicit backup source.' }
    Invoke-OperationsClient $DumpPath ($connection + @('--single-transaction','--skip-lock-tables','--skip-add-locks','--skip-add-drop-table','--skip-triggers','--no-tablespaces','--column-statistics=0','--set-gtid-purged=OFF',('--result-file=' + $partial),$Database)) $password | Out-Null
    if (-not (Test-Path -LiteralPath $partial) -or (Get-Item -LiteralPath $partial).Length -eq 0) { throw 'Backup output is missing or empty.' }
    $portable = ConvertTo-PortableDump ([IO.File]::ReadAllText($partial, [Text.Encoding]::UTF8)) $Database
    [IO.File]::WriteAllText($partial, $portable, (New-Object Text.UTF8Encoding($false)))
    Move-Item -LiteralPath $partial -Destination $dump
    $hash = (Get-FileHash -LiteralPath $dump -Algorithm SHA256).Hash
    [IO.File]::WriteAllText($dump + '.sha256', $hash, (New-Object Text.UTF8Encoding($false)))
    Write-Output ('Backup: ' + $dump)
} finally { $password = $null }

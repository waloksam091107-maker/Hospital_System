[CmdletBinding()]
param(
    [string]$MySqlHost = "127.0.0.1",
    [int]$MySqlPort = 3306,
    [string]$MySqlDatabase = "hospt",
    [string]$MySqlUser = "root",
    [string]$MySqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin",
    [string]$ComposeFile = "docker-compose.oceanbase.yml"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
# Windows PowerShell 5.1 otherwise uses US-ASCII when a string is piped to a
# native process. SQL dumps contain Chinese business data, so force UTF-8
# before passing their contents to `docker compose exec ... obclient`.
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $projectRoot ".env.oceanbase"
$exportDirectory = Join-Path $projectRoot "database\oceanbase\export"
$mysql = Join-Path $MySqlBin "mysql.exe"
$mysqldump = Join-Path $MySqlBin "mysqldump.exe"
$docker = (Get-Command docker -ErrorAction Stop).Source

function Get-LocalEnvironmentValue([string]$Name) {
    if (-not (Test-Path -LiteralPath $envFile)) { throw "未找到 $envFile。请先复制 .env.oceanbase.example 并填写本地 OceanBase 密码。" }
    $match = Get-Content -LiteralPath $envFile | Where-Object { $_ -match "^$([regex]::Escape($Name))=" } | Select-Object -First 1
    if ($null -eq $match) { throw "本地环境文件缺少 $Name。" }
    $value = $match.Substring($Name.Length + 1).Trim()
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq "<replace-locally>") { throw "$Name 尚未填写本地密码。" }
    return $value
}

function ConvertToPlainText([Security.SecureString]$SecureValue) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

$sourcePassword = ConvertToPlainText (Read-Host -AsSecureString "请输入 MySQL 源库密码（不会回显或保存）")

function Invoke-MySqlRead([string]$Sql) {
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $sourcePassword
        $output = & $mysql "-h$MySqlHost" "-P$MySqlPort" "-u$MySqlUser" "-N" "-B" "-e" $Sql $MySqlDatabase
        if ($LASTEXITCODE -ne 0) { throw "MySQL 只读查询失败。" }
        return @($output)
    } finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

function Invoke-OceanBaseSql([string]$Sql) {
    $clientCommand = 'MYSQL_PWD="$OB_TENANT_PASSWORD" obclient -h127.0.0.1 -P2881 -uroot@test -Dhospt'
    $Sql | & $docker compose --env-file $envFile -f $ComposeFile exec -T oceanbase sh -lc $clientCommand
    if ($LASTEXITCODE -ne 0) { throw "OceanBase 导入失败；已停止，MySQL 源库未被修改。" }
}

if (-not (Test-Path -LiteralPath $mysql) -or -not (Test-Path -LiteralPath $mysqldump)) { throw "未找到 MySQL 客户端工具。请检查 -MySqlBin。" }
Push-Location $projectRoot
try {
    Invoke-OceanBaseSql "SELECT 1;"

    $sourceTables = @(Invoke-MySqlRead "SHOW TABLES;") | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*$' }
    $targetTables = @('departments', 'professional_titles', 'doctors', 'patients', 'admins', 'dates', 'schedule_slots', 'appointments', 'medical_records', 'medical_orders', 'audit_logs', 'announcement', 'news', 'menu', 'orders')
    $unsupported = $sourceTables | Where-Object { $_ -notin $targetTables }
    if ($unsupported) { throw "发现当前 OceanBase DDL 未覆盖的源表：$($unsupported -join ', ')。已停止，未导入任何数据。" }

    $orderedTables = $targetTables | Where-Object { $_ -in $sourceTables }
    $primaryKeys = @{ departments='department_id'; professional_titles='id'; doctors='doctor_id'; patients='patient_id'; admins='admin_id'; dates='date_id'; schedule_slots='schedule_slot_id'; appointments='appointment_id'; medical_records='medical_record_id'; medical_orders='medical_order_id'; audit_logs='audit_log_id'; announcement='announcement_id'; news='news_id'; menu='id'; orders='order_id' }
    New-Item -ItemType Directory -Force -Path $exportDirectory | Out-Null
    $manifest = foreach ($table in $orderedTables) {
        $key = $primaryKeys[$table]
        $summary = @(Invoke-MySqlRead "SELECT COUNT(*), MIN($key), MAX($key) FROM ``$table``;")
        $parts = $summary[0] -split "`t", 3
        [pscustomobject]@{ table = $table; primaryKey = $key; sourceCount = [Int64]$parts[0]; sourceMin = $parts[1]; sourceMax = $parts[2] }
    }
    $manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $exportDirectory "source-manifest.json") -Encoding UTF8

    foreach ($table in $orderedTables) {
        $dumpFile = Join-Path $exportDirectory ("{0}.sql" -f $table)
        $previousPassword = $env:MYSQL_PWD
        try {
            $env:MYSQL_PWD = $sourcePassword
            & $mysqldump "-h$MySqlHost" "-P$MySqlPort" "-u$MySqlUser" "--single-transaction" "--no-create-info" "--complete-insert" "--skip-triggers" "--skip-add-locks" "--skip-disable-keys" "--skip-comments" "--no-tablespaces" "--result-file=$dumpFile" $MySqlDatabase $table
            if ($LASTEXITCODE -ne 0) { throw "表 $table 的只读导出失败。" }
        } finally { $env:MYSQL_PWD = $previousPassword }
        Invoke-OceanBaseSql ([IO.File]::ReadAllText($dumpFile, [Text.Encoding]::UTF8))
    }

    $appPassword = Get-LocalEnvironmentValue "HOSPITAL_APP_PASSWORD"
    $escapedPassword = $appPassword.Replace("'", "''")
    Invoke-OceanBaseSql "CREATE USER IF NOT EXISTS hospital_app IDENTIFIED BY '$escapedPassword'; GRANT SELECT, INSERT, UPDATE, DELETE ON hospt.* TO hospital_app; FLUSH PRIVILEGES;"
    Write-Host "迁移导入完成。请立即运行 scripts/oceanbase/verify-migration.ps1 进行对账。"
} finally {
    $sourcePassword = $null
    Pop-Location
}

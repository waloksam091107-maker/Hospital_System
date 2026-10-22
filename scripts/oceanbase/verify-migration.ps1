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
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $projectRoot ".env.oceanbase"
$mysql = Join-Path $MySqlBin "mysql.exe"
$docker = (Get-Command docker -ErrorAction Stop).Source

function ConvertToPlainText([Security.SecureString]$SecureValue) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

if (-not (Test-Path -LiteralPath $envFile)) { throw "未找到 $envFile。" }
if (-not (Test-Path -LiteralPath $mysql)) { throw "未找到 $mysql。" }
$sourcePassword = ConvertToPlainText (Read-Host -AsSecureString "请输入 MySQL 源库密码（不会回显或保存）")

function Invoke-MySqlRead([string]$Sql) {
    $oldPassword = $env:MYSQL_PWD
    try { $env:MYSQL_PWD = $sourcePassword; $result = & $mysql "-h$MySqlHost" "-P$MySqlPort" "-u$MySqlUser" -N -B -e $Sql $MySqlDatabase; if ($LASTEXITCODE -ne 0) { throw "MySQL 查询失败。" }; return @($result) }
    finally { $env:MYSQL_PWD = $oldPassword }
}

function Invoke-OceanBaseRead([string]$Sql) {
    $command = 'MYSQL_PWD="$OB_TENANT_PASSWORD" obclient -h127.0.0.1 -P2881 -uroot@test -Dhospt -N -B'
    $result = $Sql | & $docker compose --env-file $envFile -f $ComposeFile exec -T oceanbase sh -lc $command
    if ($LASTEXITCODE -ne 0) { throw "OceanBase 查询失败。" }
    return @($result)
}

Push-Location $projectRoot
try {
    $tables = @(Invoke-MySqlRead "SHOW TABLES;") | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*$' }
    $primaryKeys = @{ departments='department_id'; professional_titles='id'; doctors='doctor_id'; patients='patient_id'; admins='admin_id'; dates='date_id'; schedule_slots='schedule_slot_id'; appointments='appointment_id'; medical_records='medical_record_id'; medical_orders='medical_order_id'; audit_logs='audit_log_id'; announcement='announcement_id'; news='news_id'; menu='id'; orders='order_id' }
    $failures = @()
    foreach ($table in $tables) {
        if (-not $primaryKeys.ContainsKey($table)) { $failures += "目标 DDL 未声明源表 $table"; continue }
        $key = $primaryKeys[$table]
        $source = (@(Invoke-MySqlRead "SELECT COUNT(*), MIN($key), MAX($key) FROM ``$table``;"))[0]
        $target = (@(Invoke-OceanBaseRead "SELECT COUNT(*), MIN($key), MAX($key) FROM $table;"))[0]
        Write-Host "$table  源=$source  目标=$target"
        if ($source -ne $target) { $failures += "$table 的行数或主键范围不一致" }
    }

    $checks = @{
        'appointments->schedule_slots' = "SELECT COUNT(*) FROM appointments a LEFT JOIN schedule_slots s ON s.schedule_slot_id = a.schedule_slot_id WHERE a.schedule_slot_id IS NOT NULL AND s.schedule_slot_id IS NULL;"
        'medical_records->appointments' = "SELECT COUNT(*) FROM medical_records r LEFT JOIN appointments a ON a.appointment_id = r.appointment_id WHERE a.appointment_id IS NULL;"
        'medical_orders->medical_records' = "SELECT COUNT(*) FROM medical_orders o LEFT JOIN medical_records r ON r.medical_record_id = o.medical_record_id WHERE r.medical_record_id IS NULL;"
        'audit_logs.created_at' = "SELECT COUNT(*) FROM audit_logs WHERE created_at IS NULL;"
    }
    foreach ($name in $checks.Keys) {
        $orphanResult = @(Invoke-OceanBaseRead $checks[$name])
        $orphanCount = [Int64]$orphanResult[0]
        Write-Host "$name 异常记录=$orphanCount"
        if ($orphanCount -ne 0) { $failures += "$name 完整性校验失败：$orphanCount" }
    }
    if ($failures.Count -gt 0) { throw ("迁移对账失败：" + ($failures -join "；")) }
    Write-Host "迁移对账通过：行数、主键范围及预约/病历/医嘱/审计完整性均一致。"
} finally {
    $sourcePassword = $null
    Pop-Location
}

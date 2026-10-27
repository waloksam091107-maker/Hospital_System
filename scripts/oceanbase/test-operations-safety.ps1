[CmdletBinding()]
param()
. (Join-Path $PSScriptRoot 'operations-common.ps1')
$checks = 0
function Assert-Rejected([scriptblock]$Action) {
    $rejected = $false
    try { & $Action } catch { $rejected = $true }
    if (-not $rejected) { throw 'Dangerous input was accepted.' }
    $script:checks++
}
Assert-Rejected { Assert-RecoveryTarget 'hospt' 'hospt' }
Assert-Rejected { Assert-RecoveryTarget 'hospital_restore_example' 'other' }
Assert-Rejected { Assert-DatabaseName 'x; DROP DATABASE hospt' }
Assert-Rejected { Assert-IsolatedTestName 'hospital_test_fixed' }
Assert-Rejected { Assert-IsolatedTestName 'hospt' }
foreach ($unsafe in @('USE hospt;', '/*!50000 USE hospt */;', 'CREATE DATABASE hospt;', 'INSERT INTO hospt.patients VALUES(1);', 'CREATE TABLE `hospt`.`patients` (id INT);', 'SOURCE other.sql', '\! whoami', 'LOAD DATA INFILE ''x'' INTO TABLE t;')) {
    Assert-Rejected { Assert-SingleDatabaseDump $unsafe }
}
foreach ($unsafe in @('USE/**/hospt; DELETE/**/FROM/**/patients;', 'INSERT INTO/**/hospt.patients VALUES(1);', '/*!50000 USE/**/hospt */;', '\u hospt', 'DELIMITER $$', 'INSERT INTO `业务库`.`patients` VALUES(1);', 'SELECT * FROM/**/hospt.patients;', 'SET @q=''DELETE FROM hospt.patients''; PREPARE x FROM @q; EXECUTE x;', 'SET SQL_MODE=''NO_BACKSLASH_ESCAPES'';')) {
    Assert-Rejected { Assert-SingleDatabaseDump $unsafe }
}
Assert-SingleDatabaseDump "INSERT INTO notes VALUES('请 USE medicine; FROM hospt.table /*正文*/', 'it''s safe');"
$checks++
Assert-Rejected { Assert-SingleDatabaseDump 'ALTER TABLE x RENAME TO hospt.patients;' }
Assert-Rejected { Assert-SingleDatabaseDump 'CREATE TABLE x LIKE hospt.patients;' }
foreach ($unsafe in @('INSERT INTO x SELECT p.* FROM (SELECT 1) q, `hospt`.`patients` p;', 'SET @a=(SELECT p.patient_id FROM (SELECT 1) q, hospt.patients p LIMIT 1);', 'CREATE TABLE x(id INT) AS SELECT p.patient_id FROM (SELECT 1) q, hospt.patients p;', 'INSERT INTO x VALUES(hospt.f());', 'INSERT INTO x VALUES(LOAD_FILE(''file''));')) {
    Assert-Rejected { Assert-SingleDatabaseDump $unsafe }
}
Assert-RecoveryTarget 'hospital_restore_20261027' 'hospital_restore_20261027'
Assert-IsolatedTestName 'hospital_test_0123456789abcdef0123456789abcdef'
Assert-SingleDatabaseDump 'CREATE TABLE x(id INT); INSERT INTO x VALUES(1);'
$checks += 3
$qualified = 'CREATE TABLE x(id int,parent_id int,' + "`n" + '  CONSTRAINT `fk_x` FOREIGN KEY (`parent_id`) REFERENCES `hospital_test_example`.`parent` (`id`));'
$portable = ConvertTo-PortableDump $qualified 'hospital_test_example'
if ($portable -cne ('CREATE TABLE x(id int,parent_id int,' + "`n" + '  CONSTRAINT `fk_x` FOREIGN KEY (`parent_id`) REFERENCES `parent` (`id`));')) { throw 'Same-source FK was not made portable.' }
Assert-Rejected { ConvertTo-PortableDump '  CONSTRAINT `fk_x` FOREIGN KEY (`x`) REFERENCES `hospt`.`patients` (`id`)' 'hospital_test_example' }
$checks++
$backupScript = Join-Path $PSScriptRoot 'backup.ps1'
$restoreScript = Join-Path $PSScriptRoot 'restore.ps1'
Assert-Rejected { & $backupScript -Database 'hospt;bad' -ServerHost '127.0.0.1' -Port 12881 -User 'test' -ValidateOnly }
Assert-Rejected { & $restoreScript -TargetDatabase 'hospt' -ConfirmTarget 'hospt' -InputFile 'absent.sql' -ServerHost '127.0.0.1' -Port 12881 -User 'test' -ValidateOnly }
& $backupScript -Database 'hospital_test_example' -ServerHost '127.0.0.1' -Port 12881 -User 'test' -ValidateOnly | Out-Null
$checks++
foreach ($file in @('operations-common.ps1','backup.ps1','restore.ps1','run-isolated-tests.ps1','test-operations-safety.ps1')) {
    $path = Join-Path $PSScriptRoot $file
    $bytes = [IO.File]::ReadAllBytes($path)
    if ($bytes.Length -lt 3 -or $bytes[0] -ne 239 -or $bytes[1] -ne 187 -or $bytes[2] -ne 191) { throw ('PowerShell 5.1 UTF-8 BOM missing: ' + $file) }
    $tokens = $null
    $parseErrors = $null
    [void][Management.Automation.Language.Parser]::ParseFile($path,[ref]$tokens,[ref]$parseErrors)
    if ($parseErrors.Count -ne 0) { throw ('PowerShell syntax invalid: ' + $file + ' ' + $parseErrors[0].Message) }
    $checks++
}

# Exercise native argv quoting with spaces, Unicode, quotes and trailing backslashes.
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('hospital-native-' + [guid]::NewGuid().ToString('N') + '.ps1')
try {
    [IO.File]::WriteAllText($temporary, 'param([string]$Value) if ($env:MYSQL_PWD -ne ''test-password'') { exit 2 }; [Console]::OutputEncoding=[Text.Encoding]::UTF8; [Console]::Write($Value)', (New-Object Text.UTF8Encoding($true)))
    $expected = 'Chinese 中文 with "quotes" and trailing\'
    $actual = Invoke-OperationsClient 'powershell.exe' @('-NoProfile','-ExecutionPolicy','Bypass','-File',$temporary,'-Value',$expected) 'test-password'
    if ($actual -cne $expected) { throw 'Native argv roundtrip failed.' }
    $checks++
} finally { Remove-Item -LiteralPath $temporary -ErrorAction SilentlyContinue }
Write-Output ('Operations safety checks passed: ' + $checks)

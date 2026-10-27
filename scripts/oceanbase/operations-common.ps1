Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-DatabaseName([string]$Name) {
    if ($Name -notmatch '^[A-Za-z][A-Za-z0-9_]{0,63}$') { throw 'Invalid database name.' }
}

function Assert-RecoveryTarget([string]$Name, [string]$Confirmation) {
    Assert-DatabaseName $Name
    if ($Name -notmatch '^hospital_(restore|test)_[a-z0-9_]+$' -or $Name -cne $Confirmation) {
        throw 'Restore requires an explicit hospital_restore_ or hospital_test_ target and an exactly matching confirmation.'
    }
}

function Assert-IsolatedTestName([string]$Name) {
    if ($Name -notmatch '^hospital_test_[a-f0-9]{32}$') { throw 'Only a random hospital_test_ database/project is allowed.' }
}

function ConvertTo-DumpScopeTokens([string]$Sql, [int]$Depth = 0) {
    # Validate the supported mysqldump subset, not arbitrary executable SQL.
    # Literal contents are masked; executable version comments are still examined.
    if ($Depth -gt 4) { throw 'Nested SQL comments are not supported.' }
    $tokens = New-Object Text.StringBuilder
    for ($i = 0; $i -lt $Sql.Length;) {
        $ch = $Sql[$i]
        if ($ch -eq "'") {
            $literalStart = $i
            $isModeValue = $tokens.ToString() -match '(?i)\bSQL_MODE\s*=\s*$'
            $i++; $closed = $false
            while ($i -lt $Sql.Length) {
                if ($Sql[$i] -eq '\') { $i += 2; continue }
                if ($Sql[$i] -eq "'") {
                    if ($i + 1 -lt $Sql.Length -and $Sql[$i + 1] -eq "'") { $i += 2; continue }
                    $i++; $closed = $true; break
                }
                $i++
            }
            if (-not $closed) { throw 'Unterminated SQL literal.' }
            if ($isModeValue -and $Sql.Substring($literalStart, $i - $literalStart).ToUpperInvariant() -cne "'NO_AUTO_VALUE_ON_ZERO'") {
                throw 'Only the standard mysqldump SQL mode header is supported.'
            }
            [void]$tokens.Append(' __SQL_LITERAL__ '); continue
        }
        if ($ch -eq '"') { throw 'Double-quoted SQL syntax is not supported; use a standard mysqldump.' }
        if ($ch -eq '`') {
            [void]$tokens.Append($ch); $i++; $closed = $false
            while ($i -lt $Sql.Length) {
                $next = $Sql[$i]; [void]$tokens.Append($next); $i++
                if ($next -eq '`') {
                    if ($i -lt $Sql.Length -and $Sql[$i] -eq '`') { [void]$tokens.Append('`'); $i++; continue }
                    $closed = $true; break
                }
            }
            if (-not $closed) { throw 'Unterminated SQL identifier.' }
            continue
        }
        if ($ch -eq '/' -and $i + 1 -lt $Sql.Length -and $Sql[$i + 1] -eq '*') {
            $end = $Sql.IndexOf('*/', $i + 2)
            if ($end -lt 0) { throw 'Unterminated SQL comment.' }
            $body = $Sql.Substring($i + 2, $end - $i - 2)
            if ($body -match '^(?:!|M!)\d*\s*') {
                $body = [regex]::Replace($body, '^(?:!|M!)\d*\s*', '')
                [void]$tokens.Append(' '); [void]$tokens.Append((ConvertTo-DumpScopeTokens $body ($Depth + 1))); [void]$tokens.Append(' ')
            } else { [void]$tokens.Append(' ') }
            $i = $end + 2; continue
        }
        if ($ch -eq '#' -or ($ch -eq '-' -and $i + 2 -lt $Sql.Length -and $Sql[$i + 1] -eq '-' -and [char]::IsWhiteSpace($Sql[$i + 2]))) {
            while ($i -lt $Sql.Length -and $Sql[$i] -ne "`n") { $i++ }
            [void]$tokens.Append("`n"); continue
        }
        if ($ch -eq '\') { throw 'Database client backslash commands are forbidden.' }
        [void]$tokens.Append($ch); $i++
    }
    return $tokens.ToString()
}

function Assert-SingleDatabaseDump([string]$Sql) {
    $tokens = ConvertTo-DumpScopeTokens $Sql
    $identifier = '(?:`(?:[^`]|``)+`|[\p{L}_][\p{L}\p{N}_$]*)'
    if ($tokens -match ($identifier + '\s*\.\s*' + $identifier)) {
        throw 'Qualified SQL identifiers are not supported in a portable single-database dump.'
    }
    $code = [regex]::Replace($tokens, '`(?:[^`]|``)+`', '__SQL_IDENTIFIER__')
    if ($code -match '(?i)\b(SELECT|PREPARE|EXECUTE|CALL|HANDLER|LOAD_FILE)\b') {
        throw 'Queries, dynamic execution and file reads are not supported in a data dump.'
    }
    if ($tokens -match ('(?i)\b(INTO|UPDATE|FROM|JOIN|TABLE|REFERENCES)\s+(IF\s+NOT\s+EXISTS\s+)?' + $identifier + '\s*\.')) {
        throw 'Schema-qualified destinations or reads are forbidden.'
    }
    foreach ($part in $tokens.Split(';')) {
        $statement = $part.Trim()
        if (-not $statement) { continue }
        if ($statement -notmatch '(?is)^(CREATE\s+TABLE\b|ALTER\s+TABLE\b|INSERT\s+INTO\b|SET\s+)') {
            throw 'Only CREATE TABLE, ALTER TABLE, INSERT INTO and standard dump SET statements are supported.'
        }
        if ($statement -match '(?i)^CREATE\s+TABLE\b' -and
            $statement -notmatch ('(?is)^CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?' + $identifier + '\s*\(')) {
            throw 'Only explicit table definitions are supported; CREATE LIKE/AS is forbidden.'
        }
        if ($statement -match '(?i)^ALTER\s+TABLE\b' -and
            $statement -notmatch ('(?is)^ALTER\s+TABLE\s+' + $identifier + '\s+(?:ENABLE|DISABLE)\s+KEYS\s*$')) {
            throw 'Only standard dump ENABLE/DISABLE KEYS statements are supported.'
        }
        if ($statement -match '(?i)^INSERT\s+INTO\b') {
            $insert = [regex]::Match($statement, '(?is)^INSERT\s+INTO\s+' + $identifier + '(?:\s*\([^)]*\))?\s+VALUES\s*(?<rows>.+)$')
            $value = '(?:NULL|[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?|0x[0-9a-f]+|(?:_[a-z0-9]+\s+|[bBxX]\s+)?__SQL_LITERAL__)'
            $row = '\(\s*' + $value + '(?:\s*,\s*' + $value + ')*\s*\)'
            if (-not $insert.Success -or $insert.Groups['rows'].Value -notmatch ('(?is)^' + $row + '(?:\s*,\s*' + $row + ')*\s*$')) {
                throw 'INSERT supports literal VALUES only, not queries or executable expressions.'
            }
        }
        if ($statement -match '(?is)^SET\s+') {
            if ($statement -match '[()]') { throw 'SET expressions with function calls are not supported.' }
            if ($statement -match '(?i)\b(GLOBAL|PERSIST|PASSWORD|ROLE)\b' -or $statement -match '(?i)\b(INTO\s+OUTFILE|LOAD_FILE)\b') {
                throw 'Global settings and administrative SET statements are forbidden.'
            }
            # Standard mysqldump saves/restores SQL_MODE; arbitrary mode expressions
            # could change the meaning of quote/backslash handling during import.
            if ($statement -match '(?i)SQL_MODE' -and
                $statement -notmatch '(?i)^SET\s+@OLD_SQL_MODE\s*=\s*@@SQL_MODE\s*,\s*SQL_MODE\s*=\s*__SQL_LITERAL__\s*$' -and
                $statement -notmatch '(?i)^SET\s+SQL_MODE\s*=\s*@OLD_SQL_MODE\s*$') {
                throw 'Nonstandard SQL_MODE changes are forbidden.'
            }
        }
    }
}

function ConvertTo-PortableDump([string]$Sql, [string]$SourceDatabase) {
    Assert-DatabaseName $SourceDatabase
    # OceanBase SHOW CREATE qualifies same-database FK references. Only rewrite
    # constraint-definition lines; never rewrite INSERT values or other schemas.
    $source = [regex]::Escape($SourceDatabase)
    $pattern = '(?im)^(\s*CONSTRAINT\s+`?[A-Za-z0-9_]+`?\s+FOREIGN\s+KEY\s*\([^\r\n]*?\)\s+REFERENCES\s+)(?:`' + $source + '`|' + $source + ')\s*\.\s*'
    $portable = [regex]::Replace($Sql, $pattern, '$1')
    Assert-SingleDatabaseDump $portable
    return $portable
}

function Get-OperationsPassword {
    if (-not [string]::IsNullOrWhiteSpace($env:HOSPITAL_OB_PASSWORD)) { return $env:HOSPITAL_OB_PASSWORD }
    $secure = Read-Host -AsSecureString 'OceanBase password (not echoed or saved)'
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

function ConvertTo-NativeArgument([string]$Value) {
    # Windows CommandLineToArgvW quoting, including embedded quotes and trailing backslashes.
    return '"' + [regex]::Replace([regex]::Replace($Value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
}

function Invoke-OperationsClient([string]$Executable, [string[]]$Arguments, [string]$Password, [string]$InputFile = '') {
    $command = Get-Command $Executable -ErrorAction Stop
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $command.Source
    $start.Arguments = ($Arguments | ForEach-Object { ConvertTo-NativeArgument $_ }) -join ' '
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.RedirectStandardInput = $true
    $start.StandardOutputEncoding = New-Object Text.UTF8Encoding($false)
    $start.StandardErrorEncoding = New-Object Text.UTF8Encoding($false)
    $start.EnvironmentVariables['MYSQL_PWD'] = $Password
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $start
    try {
        [void]$process.Start()
        $output = $process.StandardOutput.ReadToEndAsync()
        $errorOutput = $process.StandardError.ReadToEndAsync()
        if ($InputFile) {
            $inputStream = [IO.File]::OpenRead($InputFile)
            try { $inputStream.CopyTo($process.StandardInput.BaseStream) }
            finally { $inputStream.Dispose() }
        }
        $process.StandardInput.Close()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw 'Database client failed. The target was not automatically retried; inspect the local client/server logs.' }
        return $output.Result
    } finally { $process.Dispose() }
}

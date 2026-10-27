[CmdletBinding(DefaultParameterSetName = 'Prepare')]
param(
    [Parameter(Mandatory = $true, ParameterSetName = 'Prepare')][switch]$StartIsolatedContainer,
    [Parameter(ParameterSetName = 'Prepare')][ValidateRange(1024,65535)][int]$Port = 12881,
    [Parameter(ParameterSetName = 'Prepare')][switch]$PrepareOnly,
    [Parameter(Mandatory = $true, ParameterSetName = 'Run')][switch]$RunPrepared,
    [string]$SessionFile = '',
    [switch]$KeepRunning
)
. (Join-Path $PSScriptRoot 'operations-common.ps1')
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $projectRoot 'docker-compose.oceanbase-test.yml'
$localDirectory = Join-Path $projectRoot '.hospital-local'
if (-not $SessionFile) { $SessionFile = Join-Path $localDirectory 'oceanbase-test-session.json' }
$utf8 = New-Object Text.UTF8Encoding($false)
$OutputEncoding = $utf8
$previousRootPassword = $env:HOSPITAL_TEST_ROOT_PASSWORD
$previousPort = $env:HOSPITAL_TEST_DB_PORT
$previousUrl = $env:HOSPITAL_TEST_DB_URL
$previousUser = $env:HOSPITAL_TEST_DB_USERNAME
$previousPassword = $env:HOSPITAL_TEST_DB_PASSWORD
$previousDatabase = $env:HOSPITAL_TEST_DB_NAME
$session = $null
Push-Location $projectRoot
try {
    if ($StartIsolatedContainer) {
        if ($Port -in @(2881,3306,8080)) { throw 'Business service ports are forbidden.' }
        $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
        try { $listener.Start() } finally { $listener.Stop() }
        $name = 'hospital_test_' + [guid]::NewGuid().ToString('N')
        Assert-IsolatedTestName $name
        $session = [pscustomobject]@{ project=$name; database=$name; port=$Port; user='hospital_test_runner@test'; password=([guid]::NewGuid().ToString('N')); rootPassword=([guid]::NewGuid().ToString('N')) }
    } else {
        $session = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $SessionFile).Path, [Text.Encoding]::UTF8) | ConvertFrom-Json
    }
    Assert-IsolatedTestName $session.project
    Assert-IsolatedTestName $session.database
    if ([int]$session.port -in @(2881,3306,8080) -or [int]$session.port -lt 1024) { throw 'Invalid isolated test port.' }
    if ($session.user -cne 'hospital_test_runner@test') { throw 'The restricted isolated test account is required.' }
    if ($session.password -notmatch '^[a-f0-9]{32}$' -or $session.rootPassword -notmatch '^[a-f0-9]{32}$') { throw 'Invalid local session password format.' }
    $env:HOSPITAL_TEST_ROOT_PASSWORD = $session.rootPassword
    $env:HOSPITAL_TEST_DB_PORT = [string]$session.port
    $compose = @('compose','--project-name',$session.project,'-f',$composeFile)
    if ($StartIsolatedContainer) {
        New-Item -ItemType Directory -Force -Path $localDirectory | Out-Null
        [IO.File]::WriteAllText([IO.Path]::GetFullPath($SessionFile), ($session | ConvertTo-Json), $utf8)
        & docker @compose up -d
        if ($LASTEXITCODE -ne 0) { throw 'Independent OceanBase container could not start.' }
    }
    $container = (& docker @compose ps -a -q oceanbase | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $container -notmatch '^[a-f0-9]{12,64}$') { throw 'No isolated OceanBase container found.' }
    # Avoid embedded native-command quotes, which Windows PowerShell 5.1 strips.
    $labels = (& docker inspect --format '{{json .Config.Labels}}' $container | Out-String) | ConvertFrom-Json
    $label = $labels.'com.docker.compose.project'
    if ($label -cne $session.project) { throw 'Container project isolation check failed.' }
    $deadline = (Get-Date).AddMinutes(10)
    do {
        $health = (& docker inspect --format '{{.State.Health.Status}}' $container | Out-String).Trim()
        if ($health -eq 'healthy') { break }
        if ((Get-Date) -gt $deadline) { throw 'Independent OceanBase readiness timed out. Inspect only this test project logs.' }
        Write-Host ('Waiting for independent test OceanBase: ' + $health)
        Start-Sleep -Seconds 10
    } while ($true)
    if ($StartIsolatedContainer) {
        # The account can write only this one random database; it cannot use hospt.
        $database = $session.database
        $password = $session.password
        $sql = "CREATE DATABASE ``$database`` CHARACTER SET utf8mb4; CREATE USER hospital_test_runner IDENTIFIED BY '$password'; GRANT ALL PRIVILEGES ON ``$database``.* TO hospital_test_runner;"
        $sql | & docker @compose exec -T oceanbase sh -lc 'MYSQL_PWD="$OB_TENANT_PASSWORD" obclient -h127.0.0.1 -P2881 -uroot@test'
        if ($LASTEXITCODE -ne 0) { throw 'Restricted test database/account setup failed.' }
        New-Item -ItemType Directory -Force -Path $localDirectory | Out-Null
        [IO.File]::WriteAllText([IO.Path]::GetFullPath($SessionFile), ($session | ConvertTo-Json), $utf8)
        Write-Host ('Prepared independent project/database ' + $database + '. Local credentials are under ignored .hospital-local/.')
    }
    if ($PrepareOnly) { return }
    $env:HOSPITAL_TEST_DB_URL = 'jdbc:oceanbase://127.0.0.1:' + $session.port + '/' + $session.database + '?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&connectTimeout=3000&socketTimeout=10000'
    $env:HOSPITAL_TEST_DB_USERNAME = $session.user
    $env:HOSPITAL_TEST_DB_PASSWORD = $session.password
    $env:HOSPITAL_TEST_DB_NAME = $session.database
    & mvn '-Dhospital.test.oceanbase=true' '-Dtest=IsolatedOceanBaseIntegrationTest' test
    if ($LASTEXITCODE -ne 0) { throw 'OceanBase integration tests failed. The isolated schema is retained for inspection.' }
} finally {
    if ($null -ne $session -and -not $PrepareOnly -and -not $KeepRunning) {
        Assert-IsolatedTestName $session.project
        & docker compose --project-name $session.project -f $composeFile stop
        # Intentionally retain the random test database, container and volumes. No down -v / DROP / volume rm.
    }
    $env:HOSPITAL_TEST_ROOT_PASSWORD = $previousRootPassword
    $env:HOSPITAL_TEST_DB_PORT = $previousPort
    $env:HOSPITAL_TEST_DB_URL = $previousUrl
    $env:HOSPITAL_TEST_DB_USERNAME = $previousUser
    $env:HOSPITAL_TEST_DB_PASSWORD = $previousPassword
    $env:HOSPITAL_TEST_DB_NAME = $previousDatabase
    Pop-Location
}

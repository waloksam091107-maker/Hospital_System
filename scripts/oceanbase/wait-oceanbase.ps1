[CmdletBinding()]
param(
    [string]$HostName = "127.0.0.1",
    [int]$Port = 2881,
    [int]$TimeoutSeconds = 360
)

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
Write-Host "等待 OceanBase TCP 端点 $HostName`:$Port ..."
while ((Get-Date) -lt $deadline) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connect = $client.BeginConnect($HostName, $Port, $null, $null)
        if ($connect.AsyncWaitHandle.WaitOne(2000) -and $client.Connected) {
            $client.EndConnect($connect)
            Write-Host "OceanBase TCP 端点已就绪。"
            exit 0
        }
    } catch {
        # The next retry reports no secret and no connection string.
    } finally {
        $client.Close()
    }
    Start-Sleep -Seconds 2
}

throw "OceanBase 在 $TimeoutSeconds 秒内未开放 $HostName`:$Port。"

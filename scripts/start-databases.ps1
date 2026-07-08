param(
    [string]$MySqlService = "MySQL80",
    [string]$MongoService = "MongoDB",
    [int]$MySqlPort = 3306,
    [int]$MongoPort = 27017
)

$ErrorActionPreference = "Stop"

function Test-Admin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Start-DatabaseService {
    param(
        [string]$Name,
        [string]$Label
    )

    $service = Get-Service -Name $Name -ErrorAction Stop
    if ($service.Status -eq "Running") {
        Write-Host "[OK] $Label service is already running: $Name" -ForegroundColor Green
        return
    }

    if (-not (Test-Admin)) {
        throw "$Label service is not running. Please run this script as Administrator to start service: $Name"
    }

    Write-Host "[START] Starting $Label service: $Name" -ForegroundColor Yellow
    Start-Service -Name $Name
    $service.WaitForStatus("Running", "00:00:30")
    Write-Host "[OK] $Label service started: $Name" -ForegroundColor Green
}

function Wait-Port {
    param(
        [string]$Label,
        [int]$Port
    )

    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        try {
            $client = New-Object Net.Sockets.TcpClient
            $connect = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
            if ($connect.AsyncWaitHandle.WaitOne(1000, $false)) {
                $client.EndConnect($connect)
                $client.Close()
                Write-Host "[OK] $Label port is ready: $Port" -ForegroundColor Green
                return
            }
            $client.Close()
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }

    throw "$Label port is not ready after 30 seconds: $Port"
}

Write-Host "Starting local database services for scenic-ticket..." -ForegroundColor Cyan
Start-DatabaseService -Name $MySqlService -Label "MySQL"
Start-DatabaseService -Name $MongoService -Label "MongoDB"
Wait-Port -Label "MySQL" -Port $MySqlPort
Wait-Port -Label "MongoDB" -Port $MongoPort
Write-Host "All database services are ready." -ForegroundColor Cyan

param(
    [ValidateSet("start", "stop", "restart", "status", "all-start", "all-stop", "all-restart")]
    [string]$Action = "status",
    [ValidateSet("portal", "admin", "backend")]
    [string]$Service
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Services = @{
    portal  = @{ Name = "Portal frontend"; Port = 5173; Directory = Join-Path $ProjectRoot "portal"; Command = "npm.cmd run dev -- --host 127.0.0.1" }
    admin   = @{ Name = "RuoYi admin frontend"; Port = 80; Directory = Join-Path $ProjectRoot "admin"; Command = "npm.cmd run dev" }
    backend = @{ Name = "RuoYi backend"; Port = 8081; Directory = Join-Path $ProjectRoot "backend"; Command = "call `"$ProjectRoot\scripts\Start-LostFound.cmd`"" }
}

function Get-ListeningProcessIds {
    param([int]$Port)

    try {
        @(
            Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
                Select-Object -ExpandProperty OwningProcess -Unique
        )
    }
    catch {
        @()
    }
}

function Write-ServiceStatus {
    param([string]$ServiceKey)

    $service = $Services[$ServiceKey]
    $processIds = @(Get-ListeningProcessIds -Port $service.Port)
    if ($processIds.Count -eq 0) {
        Write-Host ("[STOPPED] {0} port {1}" -f $service.Name, $service.Port) -ForegroundColor DarkGray
        return
    }

    Write-Host ("[RUNNING] {0} port {1} PID {2}" -f $service.Name, $service.Port, ($processIds -join ", ")) -ForegroundColor Green
}

function Stop-ServiceByPort {
    param([string]$ServiceKey)

    $service = $Services[$ServiceKey]
    $processIds = @(Get-ListeningProcessIds -Port $service.Port)
    if ($processIds.Count -eq 0) {
        Write-Host ("{0} is not running." -f $service.Name) -ForegroundColor DarkGray
        return
    }

    foreach ($processId in $processIds) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($null -ne $process) {
            Write-Host ("Stopping {0}, PID {1}..." -f $service.Name, $processId)
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        }
    }

    Start-Sleep -Milliseconds 800
    if ((Get-ListeningProcessIds -Port $service.Port).Count -eq 0) {
        Write-Host ("{0} stopped." -f $service.Name) -ForegroundColor Green
    }
    else {
        Write-Host ("{0} still owns port {1}. Check permissions or stop it manually." -f $service.Name, $service.Port) -ForegroundColor Yellow
    }
}

function Start-ServiceProcess {
    param([string]$ServiceKey)

    $service = $Services[$ServiceKey]
    if ((Get-ListeningProcessIds -Port $service.Port).Count -gt 0) {
        Write-Host ("{0} is already running on port {1}." -f $service.Name, $service.Port) -ForegroundColor Yellow
        return
    }

    if (-not (Test-Path -LiteralPath $service.Directory)) {
        Write-Host ("Directory not found: {0}" -f $service.Directory) -ForegroundColor Red
        return
    }

    if ($ServiceKey -eq "backend" -and -not (Test-Path -LiteralPath (Join-Path $service.Directory "ruoyi-admin\target\ruoyi-admin.jar"))) {
        Write-Host "Backend JAR not found. Build the backend first." -ForegroundColor Red
        return
    }

    Write-Host ("Starting {0}..." -f $service.Name) -ForegroundColor Cyan
    $commandLine = "cd /d `"$($service.Directory)`" && $($service.Command)"
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList @("/c", $commandLine) `
        -WorkingDirectory $service.Directory `
        -WindowStyle Normal | Out-Null

    Write-Host "A separate service window was opened. Enter the MySQL password there if prompted." -ForegroundColor DarkGray
}

function Invoke-ServiceAction {
    param(
        [string]$ActionName,
        [string]$ServiceKey
    )

    switch ($ActionName) {
        "start"   { Start-ServiceProcess -ServiceKey $ServiceKey }
        "stop"    { Stop-ServiceByPort -ServiceKey $ServiceKey }
        "restart" {
            Stop-ServiceByPort -ServiceKey $ServiceKey
            Start-ServiceProcess -ServiceKey $ServiceKey
        }
        "status"  { Write-ServiceStatus -ServiceKey $ServiceKey }
    }
}

switch ($Action) {
    "all-start" {
        Start-ServiceProcess -ServiceKey "backend"
        Start-ServiceProcess -ServiceKey "admin"
        Start-ServiceProcess -ServiceKey "portal"
    }
    "all-stop" {
        Stop-ServiceByPort -ServiceKey "portal"
        Stop-ServiceByPort -ServiceKey "admin"
        Stop-ServiceByPort -ServiceKey "backend"
    }
    "all-restart" {
        Stop-ServiceByPort -ServiceKey "portal"
        Stop-ServiceByPort -ServiceKey "admin"
        Stop-ServiceByPort -ServiceKey "backend"
        Start-Sleep -Seconds 1
        Start-ServiceProcess -ServiceKey "backend"
        Start-ServiceProcess -ServiceKey "admin"
        Start-ServiceProcess -ServiceKey "portal"
    }
    default {
        if ($Service) {
            Invoke-ServiceAction -ActionName $Action -ServiceKey $Service
        }
        elseif ($Action -eq "status") {
            Write-ServiceStatus -ServiceKey "portal"
            Write-ServiceStatus -ServiceKey "admin"
            Write-ServiceStatus -ServiceKey "backend"
        }
    }
}

param(
    [string]$DatabaseUser = "root"
)

$ErrorActionPreference = "Stop"
$backend = Join-Path $PSScriptRoot "..\backend"
$jar = Join-Path $backend "ruoyi-admin\target\ruoyi-admin.jar"

if (-not (Test-Path -LiteralPath $jar)) {
    throw "Backend JAR not found: $jar"
}

$secure = Read-Host "Enter Lost-Found MySQL password" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)

try {
    $env:LOSTFOUND_DB_USERNAME = $DatabaseUser
    $env:LOSTFOUND_DB_PASSWORD =
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    Push-Location $backend
    try {
        java -jar $jar --spring.profiles.active=druid,lostfound
    }
    finally {
        Pop-Location
    }
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    Remove-Item Env:LOSTFOUND_DB_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:LOSTFOUND_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Variable secure,ptr -ErrorAction SilentlyContinue
}

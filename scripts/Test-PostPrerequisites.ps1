param(
    [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"
try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/lostfound/category/tree" -TimeoutSec 10
    if ($response.code -ne 200) { throw "Category API did not return code 200." }
    $roots = @($response.data)
    $missing = @()
    foreach ($type in @("ITEM", "COLOR", "CAMPUS")) {
        $count = @($roots | Where-Object {
            $_.categoryType -eq $type -and $_.parentId -eq 0 -and
            $_.status -eq "0" -and $_.delFlag -eq "0"
        }).Count
        Write-Output "$type enabled roots: $count"
        if ($count -eq 0) { $missing += $type }
    }
    if ($missing.Count -gt 0) {
        throw "Publishing prerequisites missing: $($missing -join ', '). Apply backend/sql/lost_found_category_seed.sql in ry-vue-lostfound using Navicat."
    }
    Write-Output "Category prerequisites PASS. This does NOT certify the publish/review/image workflow."
}
catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}

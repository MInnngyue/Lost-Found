param(
    [string]$MySqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin",
    [int]$Port = 13307
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$root = Split-Path -Parent $PSScriptRoot
$run = Join-Path $root ("_data\sql-tests\" + [guid]::NewGuid().ToString("N"))
$data = Join-Path $run "data"
$mysql = Join-Path $MySqlBin "mysql.exe"
$mysqld = Join-Path $MySqlBin "mysqld.exe"
$mysqladmin = Join-Path $MySqlBin "mysqladmin.exe"
$server = $null
$ownedPort = $false

if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "Port $Port is in use. No existing database will be touched."
}
New-Item -ItemType Directory -Path $data -Force | Out-Null
$basedir = Split-Path -Parent $MySqlBin

function Invoke-TestSql([string]$Sql) {
    $result = $Sql | & $mysql --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$Port" `
        --user=root --default-character-set=utf8mb4 --batch --raw --skip-column-names
    if ($LASTEXITCODE -ne 0) { throw "Isolated MySQL SQL failed (exit $LASTEXITCODE)." }
    return $result
}

try {
    $init = Start-Process -FilePath $mysqld -ArgumentList @(
        "--no-defaults", "--initialize-insecure", "--console",
        "--basedir=`"$basedir`"", "--datadir=`"$data`""
    ) -WindowStyle Hidden -Wait -PassThru `
        -RedirectStandardOutput (Join-Path $run "init.out.log") `
        -RedirectStandardError (Join-Path $run "init.err.log")
    if ($init.ExitCode -ne 0) { throw "Isolated MySQL initialization failed. Logs: $run" }

    $server = Start-Process -FilePath $mysqld -ArgumentList @(
        "--no-defaults", "--console", "--basedir=`"$basedir`"", "--datadir=`"$data`"",
        "--bind-address=127.0.0.1", "--port=$Port", "--mysqlx=OFF", "--skip-log-bin",
        "--innodb-buffer-pool-size=64M"
    ) -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $run "server.out.log") `
        -RedirectStandardError (Join-Path $run "server.err.log")
    $deadline = (Get-Date).AddSeconds(45)
    do {
        if ($server.HasExited) { throw "Isolated server exited. Logs: $run" }
        $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($listener) { break }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    $listenerProcess = if ($listener) {
        Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
    }
    if (!$listenerProcess -or ($listenerProcess.ProcessId -ne $server.Id -and
            $listenerProcess.ParentProcessId -ne $server.Id)) {
        throw "Unable to verify ownership of test port. No SQL executed."
    }
    $ownedPort = $true
    $actualData = (Invoke-TestSql "SELECT @@datadir;").Trim().Replace("/", "\").TrimEnd("\")
    if ($actualData -ne $data.TrimEnd("\")) {
        throw "Data directory mismatch. No schema changes executed."
    }
    Invoke-TestSql 'CREATE DATABASE `ry-vue-lostfound` CHARACTER SET utf8mb4;'
    $schemaSql = Get-Content -LiteralPath (Join-Path $root "backend\sql\lost_found_v2_fresh.sql") -Raw -Encoding UTF8
    $seedSql = Get-Content -LiteralPath (Join-Path $root "backend\sql\lost_found_category_seed.sql") -Raw -Encoding UTF8
    Invoke-TestSql "USE ``ry-vue-lostfound``;`n$schemaSql"

    # An existing user-created item and a disabled color must survive unchanged.
    Invoke-TestSql @'
INSERT INTO `ry-vue-lostfound`.lf_category
    (category_type,parent_id,category_code,category_name,status,create_by)
VALUES ('ITEM',0,'USER_ELECTRONICS',CONVERT(0xE794B5E5AD90E8AEBEE5A487 USING utf8mb4),'0','user'),
       ('COLOR',0,'BLUE','Preserve disabled blue','1','user');
'@
    Invoke-TestSql $seedSql | Out-Null
    $snapshotSql = @'
SET SESSION group_concat_max_len=1000000;
SELECT MD5(GROUP_CONCAT(CONCAT_WS('|',category_id,category_type,parent_id,category_code,
       category_name,status,create_by,update_by,create_time,update_time,del_flag)
       ORDER BY category_id SEPARATOR ';'))
FROM `ry-vue-lostfound`.lf_category;
'@
    $before = Invoke-TestSql $snapshotSql
    Invoke-TestSql $seedSql | Out-Null
    $after = Invoke-TestSql $snapshotSql
    if ($before -ne $after) { throw "Idempotency failed: second run changed category records." }

    $checks = Invoke-TestSql @'
SELECT COUNT(*) FROM `ry-vue-lostfound`.lf_category WHERE category_type='ITEM'
 AND category_name=CONVERT(0xE794B5E5AD90E8AEBEE5A487 USING utf8mb4);
SELECT COUNT(*) FROM `ry-vue-lostfound`.lf_category WHERE category_code='BLUE'
 AND status='1' AND category_name='Preserve disabled blue';
SELECT COUNT(*) FROM `ry-vue-lostfound`.lf_category child
 LEFT JOIN `ry-vue-lostfound`.lf_category parent ON parent.category_id=child.parent_id
 WHERE child.category_type IN ('AREA','LOCATION')
 AND (parent.category_id IS NULL OR (child.category_type='AREA' AND parent.category_type<>'CAMPUS')
 OR (child.category_type='LOCATION' AND parent.category_type<>'AREA'));
SELECT COUNT(DISTINCT category_type) FROM `ry-vue-lostfound`.lf_category
 WHERE category_type IN ('ITEM','COLOR','CAMPUS') AND status='0' AND del_flag='0';
'@
    if (($checks -join ",") -ne "1,1,0,3") {
        throw "Seed preservation or hierarchy check failed: $($checks -join ',')."
    }

    # This server and all records belong to this test run, not the user's database.
    Invoke-TestSql @'
DELETE FROM `ry-vue-lostfound`.lf_category WHERE category_type IN ('CAMPUS','AREA','LOCATION');
INSERT INTO `ry-vue-lostfound`.lf_category
    (category_type,parent_id,category_code,category_name,status,create_by)
VALUES ('CAMPUS',0,'USER_CAMPUS','Existing campus','0','user');
'@
    Invoke-TestSql $seedSql | Out-Null
    $testCampusCount = Invoke-TestSql "SELECT COUNT(*) FROM ``ry-vue-lostfound``.lf_category WHERE category_code='TEST_CAMPUS';"
    if ($testCampusCount -ne "0") { throw "Seed introduced a test campus alongside a real campus." }
    Write-Output "PASS: MySQL 8 seed, idempotency, existing item preservation, disabled color preservation, hierarchy, readiness, real-campus preservation."
    Write-Output "Isolated run artifacts: $run"
}
finally {
    if ($server -and !$server.HasExited) {
        if ($ownedPort) {
            & $mysqladmin --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$Port" --user=root shutdown 2>$null
        }
        if (!$server.WaitForExit(10000)) { Stop-Process -Id $server.Id -Force }
    }
    if (Test-Path -LiteralPath $data) {
        $resolvedData = (Resolve-Path -LiteralPath $data).Path
        $testRoot = [System.IO.Path]::GetFullPath((Join-Path $root "_data\sql-tests")) + "\"
        if (!$resolvedData.StartsWith($testRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove a path outside the isolated SQL test directory."
        }
        Remove-Item -LiteralPath $resolvedData -Recurse -Force
    }
}

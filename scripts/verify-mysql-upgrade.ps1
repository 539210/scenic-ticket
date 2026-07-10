param(
    [string]$TestDatabase = "scenic_ticket_test",
    [string]$ConfigPath = "",
    [string]$MysqlExecutable = "mysql",
    [switch]$Reset
)

$ErrorActionPreference = "Stop"
if ($TestDatabase -ne "scenic_ticket_test") {
    throw "Refusing to operate on database '$TestDatabase'. Only scenic_ticket_test is allowed."
}
if (-not $Reset) {
    throw "Pass -Reset to confirm recreation of the isolated scenic_ticket_test database."
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $projectRoot "src/main/resources/db.properties"
}
$properties = ConvertFrom-StringData (Get-Content -Raw -Encoding UTF8 -LiteralPath $ConfigPath)
$jdbcUrl = $properties["mysql.url"]
$username = $properties["mysql.username"]
$password = $properties["mysql.password"]
if ($jdbcUrl -notmatch '^jdbc:mysql://([^:/?]+)(?::([0-9]+))?') {
    throw "mysql.url is not a supported MySQL JDBC URL."
}
$hostName = $Matches[1]
$port = if ([string]::IsNullOrWhiteSpace($Matches[2])) { "3306" } else { $Matches[2] }
$mysqlCommand = Get-Command $MysqlExecutable -ErrorAction Stop

$legacyFixture = Join-Path $projectRoot "src/test/resources/sql/mysql_day08_legacy_fixture.sql"
$sqlDirectory = Join-Path $projectRoot "src/main/resources/sql"
$upgradeScripts = @(
    $legacyFixture,
    (Join-Path $sqlDirectory "mysql_day09_migration_baseline.sql"),
    (Join-Path $sqlDirectory "mysql_day09_ticket_types_inventory.sql"),
    (Join-Path $sqlDirectory "mysql_day09_order_lifecycle_refunds_admissions.sql"),
    (Join-Path $sqlDirectory "mysql_views.sql"),
    (Join-Path $sqlDirectory "mysql_procedures.sql"),
    (Join-Path $sqlDirectory "mysql_triggers.sql")
)

$oldMysqlPassword = $env:MYSQL_PWD
$temporaryFiles = New-Object System.Collections.Generic.List[string]
try {
    $env:MYSQL_PWD = $password
    & $mysqlCommand.Source --default-character-set=utf8mb4 --host=$hostName --port=$port --user=$username `
        --execute="DROP DATABASE IF EXISTS ``$TestDatabase``; CREATE DATABASE ``$TestDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    if ($LASTEXITCODE -ne 0) { throw "Failed to recreate the isolated test database." }

    foreach ($sourcePath in $upgradeScripts) {
        $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $sourcePath
        $testContent = $content -replace '(?i)scenic_ticket', $TestDatabase
        $temporaryPath = [System.IO.Path]::GetTempFileName()
        $temporaryFiles.Add($temporaryPath)
        [System.IO.File]::WriteAllText($temporaryPath, $testContent, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "[UPGRADE] $(Split-Path -Leaf $sourcePath)"
        & $mysqlCommand.Source --default-character-set=utf8mb4 --host=$hostName --port=$port --user=$username `
            --database=$TestDatabase --execute="source $($temporaryPath.Replace('\', '/'))"
        if ($LASTEXITCODE -ne 0) { throw "Upgrade script failed: $sourcePath" }
    }

    $validationSql = @"
SELECT 'tables', COUNT(*) FROM information_schema.tables WHERE table_schema = '$TestDatabase' AND table_type = 'BASE TABLE';
SELECT 'views', COUNT(*) FROM information_schema.views WHERE table_schema = '$TestDatabase';
SELECT 'procedures', COUNT(*) FROM information_schema.routines WHERE routine_schema = '$TestDatabase' AND routine_type = 'PROCEDURE';
SELECT 'triggers', COUNT(*) FROM information_schema.triggers WHERE trigger_schema = '$TestDatabase';
SELECT 'legacy_users', COUNT(*) FROM users WHERE username = 'legacy_admin';
SELECT 'legacy_profiles', COUNT(*) FROM profiles WHERE profile_id = 1 AND user_id = 1;
SELECT 'legacy_orders', COUNT(*) FROM orders WHERE order_id = 1 AND ticket_type_id IS NOT NULL AND ticket_type_name_snapshot IS NOT NULL AND original_unit_price = 100.00 AND discounted_unit_price = 90.00;
SELECT 'ticket_types', COUNT(*) FROM ticket_types;
SELECT 'inventory_rows', COUNT(*) FROM ticket_inventory;
SELECT 'migration_rows', COUNT(*) FROM schema_migrations;
"@
    $results = & $mysqlCommand.Source --default-character-set=utf8mb4 --host=$hostName --port=$port --user=$username `
        --database=$TestDatabase --batch --skip-column-names --execute=$validationSql
    if ($LASTEXITCODE -ne 0) { throw "Upgrade validation queries failed." }

    $metrics = @{}
    foreach ($line in $results) {
        $parts = $line -split "`t"
        if ($parts.Count -eq 2) { $metrics[$parts[0]] = [int]$parts[1] }
    }
    foreach ($key in @("legacy_users", "legacy_profiles", "legacy_orders")) {
        if ($metrics[$key] -ne 1) { throw "Legacy compatibility check failed: $key=$($metrics[$key])" }
    }
    if ($metrics["tables"] -lt 10 -or $metrics["views"] -lt 2 -or $metrics["procedures"] -lt 2 -or $metrics["triggers"] -lt 2) {
        throw "Upgraded database is missing required database objects."
    }
    if ($metrics["ticket_types"] -lt 3 -or $metrics["inventory_rows"] -lt 21 -or $metrics["migration_rows"] -lt 3) {
        throw "Upgraded database is missing ticketing data or migration records."
    }

    Write-Host "[OK] Day08 legacy fixture upgraded without losing required fields or records." -ForegroundColor Green
    $metrics.GetEnumerator() | Sort-Object Name | ForEach-Object { Write-Host ("  {0}={1}" -f $_.Name, $_.Value) }
} finally {
    foreach ($temporaryPath in $temporaryFiles) {
        if (Test-Path -LiteralPath $temporaryPath) { Remove-Item -LiteralPath $temporaryPath -Force }
    }
    $env:MYSQL_PWD = $oldMysqlPassword
}

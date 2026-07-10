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
if (-not (Test-Path -LiteralPath $ConfigPath)) {
    throw "Local db.properties is required but was not found. Copy db.properties.example and keep it uncommitted."
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
if ([string]::IsNullOrWhiteSpace($username) -or [string]::IsNullOrWhiteSpace($password)) {
    throw "MySQL username/password is missing from local db.properties."
}

$mysqlCommand = Get-Command $MysqlExecutable -ErrorAction Stop
$sqlDirectory = Join-Path $projectRoot "src/main/resources/sql"
$sqlFiles = @(
    "mysql_schema.sql",
    "mysql_init_data.sql",
    "mysql_views.sql",
    "mysql_procedures.sql",
    "mysql_triggers.sql",
    "mysql_day07_optimization.sql",
    "mysql_day08_pricing_update.sql",
    "mysql_day09_migration_baseline.sql",
    "mysql_day09_ticket_types_inventory.sql",
    "mysql_day09_order_lifecycle_refunds_admissions.sql"
)

$oldMysqlPassword = $env:MYSQL_PWD
$temporaryFiles = New-Object System.Collections.Generic.List[string]
try {
    $env:MYSQL_PWD = $password
    & $mysqlCommand.Source --default-character-set=utf8mb4 --host=$hostName --port=$port --user=$username `
        --execute="DROP DATABASE IF EXISTS ``$TestDatabase``; CREATE DATABASE ``$TestDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to recreate the isolated MySQL test database."
    }

    foreach ($sqlFile in $sqlFiles) {
        $sourcePath = Join-Path $sqlDirectory $sqlFile
        if (-not (Test-Path -LiteralPath $sourcePath)) {
            throw "Required SQL script is missing: $sqlFile"
        }
        $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $sourcePath
        $testContent = $content -replace '(?i)scenic_ticket', $TestDatabase
        $temporaryPath = [System.IO.Path]::GetTempFileName()
        $temporaryFiles.Add($temporaryPath)
        [System.IO.File]::WriteAllText($temporaryPath, $testContent, (New-Object System.Text.UTF8Encoding($false)))
        $mysqlSourcePath = $temporaryPath.Replace('\', '/')
        Write-Host "[MYSQL] $sqlFile"
        & $mysqlCommand.Source --default-character-set=utf8mb4 --host=$hostName --port=$port --user=$username `
            --database=$TestDatabase --execute="source $mysqlSourcePath"
        if ($LASTEXITCODE -ne 0) {
            throw "SQL script failed: $sqlFile"
        }
    }

    $validationSql = @"
SELECT 'tables', COUNT(*) FROM information_schema.tables WHERE table_schema = '$TestDatabase' AND table_type = 'BASE TABLE';
SELECT 'views', COUNT(*) FROM information_schema.views WHERE table_schema = '$TestDatabase';
SELECT 'procedures', COUNT(*) FROM information_schema.routines WHERE routine_schema = '$TestDatabase' AND routine_type = 'PROCEDURE';
SELECT 'triggers', COUNT(*) FROM information_schema.triggers WHERE trigger_schema = '$TestDatabase';
SELECT 'foreign_keys', COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema = '$TestDatabase';
SELECT 'ticket_types', COUNT(*) FROM ticket_types;
SELECT 'inventory_rows', COUNT(*) FROM ticket_inventory;
SELECT 'invalid_inventory', COUNT(*) FROM ticket_inventory WHERE total_stock < 0 OR available_stock < 0 OR reserved_stock < 0 OR sold_stock < 0 OR available_stock + reserved_stock + sold_stock > total_stock;
SELECT 'migration_rows', COUNT(*) FROM schema_migrations;
"@
    $results = & $mysqlCommand.Source --default-character-set=utf8mb4 --host=$hostName --port=$port --user=$username `
        --database=$TestDatabase --batch --skip-column-names --execute=$validationSql
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL validation queries failed."
    }

    $metrics = @{}
    foreach ($line in $results) {
        $parts = $line -split "`t"
        if ($parts.Count -eq 2) {
            $metrics[$parts[0]] = [int]$parts[1]
        }
    }
    if ($metrics["tables"] -lt 10) { throw "Expected at least 10 tables, found $($metrics['tables'])." }
    if ($metrics["views"] -lt 2) { throw "Expected at least 2 views, found $($metrics['views'])." }
    if ($metrics["procedures"] -lt 2) { throw "Expected at least 2 procedures, found $($metrics['procedures'])." }
    if ($metrics["triggers"] -lt 2) { throw "Expected at least 2 triggers, found $($metrics['triggers'])." }
    if ($metrics["foreign_keys"] -lt 8) { throw "Expected at least 8 foreign keys, found $($metrics['foreign_keys'])." }
    if ($metrics["ticket_types"] -lt 60) { throw "Expected at least 60 ticket types, found $($metrics['ticket_types'])." }
    if ($metrics["inventory_rows"] -lt 420) { throw "Expected at least 420 inventory rows, found $($metrics['inventory_rows'])." }
    if ($metrics["invalid_inventory"] -ne 0) { throw "Inventory invariants are violated." }
    if ($metrics["migration_rows"] -lt 4) { throw "Expected at least 4 migration records, found $($metrics['migration_rows'])." }

    Write-Host "[OK] MySQL scenic_ticket_test initialized and validated." -ForegroundColor Green
    $metrics.GetEnumerator() | Sort-Object Name | ForEach-Object {
        Write-Host ("  {0}={1}" -f $_.Name, $_.Value)
    }
} finally {
    foreach ($temporaryPath in $temporaryFiles) {
        if (Test-Path -LiteralPath $temporaryPath) {
            Remove-Item -LiteralPath $temporaryPath -Force
        }
    }
    $env:MYSQL_PWD = $oldMysqlPassword
}

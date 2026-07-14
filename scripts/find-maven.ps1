$ErrorActionPreference = 'SilentlyContinue'

function Find-MavenInIde {
    param([string]$IdeRoot)

    if ([string]::IsNullOrWhiteSpace($IdeRoot)) {
        return $null
    }
    $candidate = Join-Path $IdeRoot 'plugins\maven\lib\maven3\bin\mvn.cmd'
    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        return [System.IO.Path]::GetFullPath($candidate)
    }
    return $null
}

$uninstallRoots = @(
    'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
)

foreach ($root in $uninstallRoots) {
    foreach ($entry in Get-ItemProperty -Path $root | Where-Object { $_.DisplayName -like 'IntelliJ IDEA*' }) {
        $maven = Find-MavenInIde $entry.InstallLocation
        if ($maven) {
            Write-Output $maven
            exit 0
        }
    }
}

$standardRoots = @(
    (Join-Path $env:ProgramFiles 'JetBrains'),
    (Join-Path $env:LOCALAPPDATA 'Programs')
) | Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Container) }

foreach ($root in $standardRoots) {
    foreach ($ide in Get-ChildItem -LiteralPath $root -Directory -Filter 'IntelliJ IDEA*') {
        $maven = Find-MavenInIde $ide.FullName
        if ($maven) {
            Write-Output $maven
            exit 0
        }
    }
}

exit 1

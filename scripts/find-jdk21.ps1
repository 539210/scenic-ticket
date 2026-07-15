$ErrorActionPreference = 'SilentlyContinue'

function Add-JdkCandidate {
    param(
        [System.Collections.Generic.List[string]]$Candidates,
        [string]$Path
    )

    if (-not [string]::IsNullOrWhiteSpace($Path)) {
        $Candidates.Add($Path.Trim('"'))
    }
}

function Test-Jdk21 {
    param([string]$JdkHome)

    $javac = Join-Path $JdkHome 'bin\javac.exe'
    $java = Join-Path $JdkHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javac -PathType Leaf) -or
            -not (Test-Path -LiteralPath $java -PathType Leaf)) {
        return $false
    }
    # Reading the executable version avoids PowerShell treating `java -version`
    # output on stderr as an error record.
    $version = [string](Get-Item -LiteralPath $javac).VersionInfo.ProductVersion
    return $version -match '^21\.'
}

$candidates = [System.Collections.Generic.List[string]]::new()
Add-JdkCandidate $candidates $env:SCENIC_JAVA_HOME
Add-JdkCandidate $candidates $env:JAVA_HOME

Get-Command javac.exe -ErrorAction SilentlyContinue | ForEach-Object {
    Add-JdkCandidate $candidates (Split-Path (Split-Path $_.Source -Parent) -Parent)
}

foreach ($registryPath in @(
    'HKLM:\SOFTWARE\JavaSoft\JDK',
    'HKLM:\SOFTWARE\WOW6432Node\JavaSoft\JDK',
    'HKLM:\SOFTWARE\Eclipse Adoptium\JDK',
    'HKLM:\SOFTWARE\Azul Systems\Zulu'
)) {
    Get-ItemProperty -Path $registryPath | ForEach-Object {
        Add-JdkCandidate $candidates $_.JavaHome
        Add-JdkCandidate $candidates $_.InstallPath
    }
    Get-ChildItem -Path $registryPath | ForEach-Object {
        $properties = Get-ItemProperty -Path $_.PSPath
        Add-JdkCandidate $candidates $properties.JavaHome
        Add-JdkCandidate $candidates $properties.InstallPath
    }
}

foreach ($root in @(
    'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
)) {
    Get-ItemProperty -Path $root | Where-Object {
        $_.DisplayName -match 'JDK|OpenJDK|Zulu|Temurin|Adoptium'
    } | ForEach-Object {
        Add-JdkCandidate $candidates $_.InstallLocation
    }
}

@(
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:ProgramFiles 'Zulu'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    'D:\'
) | Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Container) } | ForEach-Object {
    Get-ChildItem -LiteralPath $_ -Directory | Where-Object {
        $_.Name -match 'jdk|zulu|temurin|openjdk'
    } | ForEach-Object {
        Add-JdkCandidate $candidates $_.FullName
    }
}

foreach ($candidate in $candidates | Select-Object -Unique) {
    if (Test-Jdk21 $candidate) {
        [Console]::Out.WriteLine([System.IO.Path]::GetFullPath($candidate))
        exit 0
    }
}

exit 1

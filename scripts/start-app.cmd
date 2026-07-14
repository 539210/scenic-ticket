@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%"

if defined SCENIC_JAVA_HOME (
  set "JAVA_HOME=%SCENIC_JAVA_HOME%"
)

set "JDK_READY="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_READY=1"

if not defined JDK_READY (
  set "JAVA_HOME="
  for /f "delims=" %%I in ('where javac.exe 2^>nul') do if not defined JDK_READY (
    for %%J in ("%%~dpI..") do set "JAVA_HOME=%%~fJ"
    if exist "!JAVA_HOME!\bin\javac.exe" set "JDK_READY=1"
  )
)

if not defined JDK_READY (
  echo JDK 21 was not found. Set JAVA_HOME or SCENIC_JAVA_HOME to a JDK 21 installation.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
set "JAVA_VERSION="
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do (
  if not defined JAVA_VERSION set "JAVA_VERSION=%%~V"
)
if not "!JAVA_VERSION:~0,3!"=="21." (
  echo Java 21 is required. Detected version: !JAVA_VERSION!
  exit /b 1
)

set "MAVEN_CMD="
if defined SCENIC_MAVEN_CMD (
  if exist "%SCENIC_MAVEN_CMD%" set "MAVEN_CMD=%SCENIC_MAVEN_CMD%"
)

if not defined MAVEN_CMD if exist "%PROJECT_ROOT%\mvnw.cmd" (
  set "MAVEN_CMD=%PROJECT_ROOT%\mvnw.cmd"
)

if not defined MAVEN_CMD if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" (
  set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
)

if not defined MAVEN_CMD (
  where mvn.cmd >nul 2>nul
  if not errorlevel 1 set "MAVEN_CMD=mvn.cmd"
)

if not defined MAVEN_CMD (
  where mvn >nul 2>nul
  if not errorlevel 1 set "MAVEN_CMD=mvn"
)

if not defined MAVEN_CMD if exist "%SCRIPT_DIR%find-maven.ps1" (
  for /f "delims=" %%I in ('powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%find-maven.ps1" 2^>nul') do if not defined MAVEN_CMD (
    if exist "%%~fI" set "MAVEN_CMD=%%~fI"
  )
)

if not defined MAVEN_CMD (
  echo Maven was not found. Set MAVEN_HOME or SCENIC_MAVEN_CMD, add mvn to PATH, install IntelliJ IDEA, or add Maven Wrapper.
  exit /b 1
)

echo Starting Scenic Ticket Swing application...
echo Project: %PROJECT_ROOT%
if defined JAVA_HOME echo JAVA_HOME: %JAVA_HOME%

if /i "%~1"=="--check" (
  if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" -version >nul 2>nul
  ) else (
    call %MAVEN_CMD% -version >nul 2>nul
  )
  if errorlevel 1 (
    echo Maven was found but could not run with the selected JDK.
    exit /b 1
  )
  echo Environment check passed: JDK !JAVA_VERSION! and Maven are available.
  exit /b 0
)

if exist "%MAVEN_CMD%" (
  call "%MAVEN_CMD%" exec:java -Dexec.mainClass=com.scenicticket.Main
) else (
  call %MAVEN_CMD% exec:java -Dexec.mainClass=com.scenicticket.Main
)

if errorlevel 1 (
  echo.
  echo Swing application failed to start. Check db.properties and database service status.
  exit /b 1
)

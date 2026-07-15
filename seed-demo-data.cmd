@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /i not "%~1"=="--apply" (
  echo This script adds up to 50 demo users, scenic items, paid orders and comments.
  echo It is safe to rerun and does not overwrite later edits.
  echo.
  echo To execute: seed-demo-data.cmd --apply
  exit /b 2
)

set "PROJECT_ROOT=%~dp0"
cd /d "%PROJECT_ROOT%"

if defined SCENIC_JAVA_HOME set "JAVA_HOME=%SCENIC_JAVA_HOME%"
if not defined JAVA_HOME (
  for /f "delims=" %%I in ('where javac.exe 2^>nul') do if not defined JAVA_HOME (
    for %%J in ("%%~dpI..") do set "JAVA_HOME=%%~fJ"
  )
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo JDK 21 was not found. Set JAVA_HOME or SCENIC_JAVA_HOME.
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "MAVEN_CMD="
if defined SCENIC_MAVEN_CMD if exist "%SCENIC_MAVEN_CMD%" set "MAVEN_CMD=%SCENIC_MAVEN_CMD%"
if not defined MAVEN_CMD if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MAVEN_CMD if exist "%PROJECT_ROOT%mvnw.cmd" set "MAVEN_CMD=%PROJECT_ROOT%mvnw.cmd"
if not defined MAVEN_CMD (
  where mvn.cmd >nul 2>nul
  if not errorlevel 1 set "MAVEN_CMD=mvn.cmd"
)
if not defined MAVEN_CMD if exist "%PROJECT_ROOT%scripts\find-maven.ps1" (
  for /f "delims=" %%I in ('powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%scripts\find-maven.ps1" 2^>nul') do if not defined MAVEN_CMD (
    if exist "%%~fI" set "MAVEN_CMD=%%~fI"
  )
)
if not defined MAVEN_CMD (
  echo Maven was not found. Set MAVEN_HOME or SCENIC_MAVEN_CMD.
  exit /b 1
)

echo Seeding 50 demo users, scenic items, paid orders and comments...
if exist "%MAVEN_CMD%" (
  call "%MAVEN_CMD%" -q compile exec:java -Dexec.mainClass=com.scenicticket.tools.DemoDataSeeder -Dexec.args="--apply --count=50"
) else (
  call %MAVEN_CMD% -q compile exec:java -Dexec.mainClass=com.scenicticket.tools.DemoDataSeeder -Dexec.args="--apply --count=50"
)
if errorlevel 1 (
  echo Demo data generation failed. Fix the reported problem and rerun; completed rows are idempotent and will not be duplicated.
  exit /b 1
)

echo Demo data generation completed.

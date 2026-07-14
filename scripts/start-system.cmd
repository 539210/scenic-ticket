@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%"

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%start-databases.ps1"
if errorlevel 1 (
  echo.
  echo Database startup failed. If a service is stopped, right-click this file and run as Administrator.
  pause
  exit /b 1
)

call "%SCRIPT_DIR%start-app.cmd" %*

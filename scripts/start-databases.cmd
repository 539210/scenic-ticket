@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-databases.ps1" %*
if errorlevel 1 (
  echo.
  echo Database startup failed. If a service is stopped, right-click this file and run as Administrator.
)
echo.
pause

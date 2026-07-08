@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%"

set "LOCAL_JAVA_HOME=D:\zulu21.44.17-ca-jdk21.0.8-win_x64\zulu21.44.17-ca-jdk21.0.8-win_x64"
if exist "%LOCAL_JAVA_HOME%\bin\java.exe" (
  set "JAVA_HOME=%LOCAL_JAVA_HOME%"
)

if defined JAVA_HOME (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)

set "MAVEN_CMD="
set "IDEA_MAVEN=D:\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"
if exist "%IDEA_MAVEN%" (
  set "MAVEN_CMD=%IDEA_MAVEN%"
)

if not defined MAVEN_CMD (
  where mvn.cmd >nul 2>nul
  if not errorlevel 1 set "MAVEN_CMD=mvn.cmd"
)

if not defined MAVEN_CMD (
  where mvn >nul 2>nul
  if not errorlevel 1 set "MAVEN_CMD=mvn"
)

if not defined MAVEN_CMD (
  echo Maven was not found. Please install Maven or run this project from IntelliJ IDEA.
  pause
  exit /b 1
)

echo Starting Scenic Ticket Swing application...
echo Project: %PROJECT_ROOT%
if defined JAVA_HOME echo JAVA_HOME: %JAVA_HOME%

if exist "%MAVEN_CMD%" (
  call "%MAVEN_CMD%" exec:java -Dexec.mainClass=com.scenicticket.Main
) else (
  call %MAVEN_CMD% exec:java -Dexec.mainClass=com.scenicticket.Main
)

if errorlevel 1 (
  echo.
  echo Swing application failed to start. Check db.properties and database service status.
  pause
  exit /b 1
)

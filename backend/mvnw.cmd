@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "LOCAL_MAVEN=%SCRIPT_DIR%..\.tools\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%LOCAL_MAVEN%" (
  call "%LOCAL_MAVEN%" %*
  exit /b %ERRORLEVEL%
)

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  call mvn %*
  exit /b %ERRORLEVEL%
)

echo Maven was not found.
echo Expected local Maven at: %LOCAL_MAVEN%
echo Or install Maven and make sure mvn is available on PATH.
exit /b 1

@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "SCRIPT_DIR=%~dp0"
set "MAVEN_DIST=%SCRIPT_DIR%.mvn\wrapper\dists"
set "MAVEN_HOME=%MAVEN_DIST%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"
set "MAVEN_ZIP=%MAVEN_DIST%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_BIN%" (
  echo Downloading Apache Maven %MAVEN_VERSION%...
  if not exist "%MAVEN_DIST%" mkdir "%MAVEN_DIST%"

  where node.exe >nul 2>nul
  if not errorlevel 1 (
    node.exe -e "const fs=require('fs'); const out=process.argv[1]; const url=process.argv[2]; fetch(url).then(r=>{ if(!r.ok) throw new Error(r.status + ' ' + r.statusText); return r.arrayBuffer(); }).then(b=>fs.writeFileSync(out, Buffer.from(b))).catch(e=>{ console.error(e.message); process.exit(1); });" "%MAVEN_ZIP%" "%MAVEN_URL%"
  )

  if not exist "%MAVEN_ZIP%" (
    where curl.exe >nul 2>nul
    if not errorlevel 1 (
      curl.exe --ssl-no-revoke --fail --location "%MAVEN_URL%" --output "%MAVEN_ZIP%"
    )
  )

  if not exist "%MAVEN_ZIP%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ErrorActionPreference = 'Stop';" ^
      "Add-Type -AssemblyName System.Net.Http;" ^
      "$client = [System.Net.Http.HttpClient]::new();" ^
      "try { $bytes = $client.GetByteArrayAsync('%MAVEN_URL%').GetAwaiter().GetResult(); [System.IO.File]::WriteAllBytes('%MAVEN_ZIP%', $bytes); } finally { $client.Dispose(); }"
  )

  if not exist "%MAVEN_ZIP%" (
    echo Failed to download Maven.
    exit /b 1
  )

  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference = 'Stop';" ^
    "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_DIST%' -Force;" ^
    "Remove-Item -LiteralPath '%MAVEN_ZIP%' -Force;"

  if errorlevel 1 (
    echo Failed to extract Maven.
    exit /b 1
  )
)

call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%

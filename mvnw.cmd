@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "MAVEN_USER_HOME=%MAVEN_USER_HOME%"
if not defined MAVEN_USER_HOME set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "MAVEN_HOME=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_DIST=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_DIST%\bin\mvn.cmd" (
  if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
  if errorlevel 1 exit /b %ERRORLEVEL%
  if not exist "%MAVEN_ZIP%" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -ErrorAction Stop -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%MAVEN_ZIP%'; exit 0 } catch { Write-Error $_; exit 1 }"
    if errorlevel 1 exit /b %ERRORLEVEL%
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Expand-Archive -ErrorAction Stop -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%MAVEN_HOME%' -Force; exit 0 } catch { Write-Error $_; exit 1 }"
  if errorlevel 1 exit /b %ERRORLEVEL%
)

call "%MAVEN_DIST%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%

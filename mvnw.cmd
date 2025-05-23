@echo off
setlocal
set BASEDIR=%~dp0
set WRAPPER_DIR=%BASEDIR%.mvn\wrapper
set PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties
set JAR=%WRAPPER_DIR%\maven-wrapper.jar

if exist "%PROPERTIES%" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%PROPERTIES%") do set "%%A=%%B"
)

if not exist "%JAR%" (
  echo Downloading Maven wrapper jar...
  if exist "%SystemRoot%\System32\curl.exe" (
    curl -fsSL -o "%JAR%" "%wrapperUrl%"
  ) else if exist "%ProgramFiles%\Git\usr\bin\curl.exe" (
    "%ProgramFiles%\Git\usr\bin\curl.exe" -fsSL -o "%JAR%" "%wrapperUrl%"
  ) else (
    echo Error: curl not found
    exit /b 1
  )
)

java %MAVEN_OPTS% -jar "%JAR%" %*
endlocal

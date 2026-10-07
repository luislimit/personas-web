@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================
REM backend-personas - Maven Wrapper aislado
REM - Usa JDK 21 local del proyecto
REM - Carga config\local.env automaticamente si existe
REM - No modifica JAVA_HOME/PATH globales de Windows
REM ============================================================

set "BASE_DIR=%~dp0"
set "PROJECT_JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1"

if not exist "%PROJECT_JAVA_HOME%\bin\java.exe" (
  echo.
  echo [ERROR] No se encuentra el JDK 21 configurado en:
  echo   %PROJECT_JAVA_HOME%
  echo.
  exit /b 1
)

REM ------------------------------------------------------------
REM Cargar configuracion local automaticamente
REM ------------------------------------------------------------
set "LOCAL_ENV=%BASE_DIR%config\local.env"

if exist "%LOCAL_ENV%" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%LOCAL_ENV%") do (
    set "KEY=%%A"
    set "VALUE=%%B"
    if defined KEY (
      if not "!KEY:~0,1!"=="#" (
        set "!KEY!=!VALUE!"
      )
    )
  )
)

REM Valores por defecto si local.env no define alguno.
if not defined DB_URL (
  set "DB_URL=jdbc:mysql://localhost:3306/gestion_personas?useUnicode=true&characterEncoding=utf8&serverTimezone=Europe/Madrid&allowPublicKeyRetrieval=true&useSSL=false"
)

if not defined DB_USERNAME (
  set "DB_USERNAME=root"
)

if not defined SERVER_PORT (
  set "SERVER_PORT=8080"
)

REM ------------------------------------------------------------
REM Java aislado
REM ------------------------------------------------------------
set "JAVA_HOME=%PROJECT_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [backend-personas] JAVA_HOME aislado: %JAVA_HOME%
if exist "%LOCAL_ENV%" (
  echo [backend-personas] Config local cargada: %LOCAL_ENV%
) else (
  echo [backend-personas] Config local no encontrada: %LOCAL_ENV%
)
echo [backend-personas] DB usuario: %DB_USERNAME%
echo [backend-personas] Puerto: %SERVER_PORT%

REM ------------------------------------------------------------
REM Maven Wrapper
REM ------------------------------------------------------------
set "PROP_FILE=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"

if not exist "%PROP_FILE%" (
  echo [ERROR] No existe %PROP_FILE%
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%PROP_FILE%") do (
  if "%%A"=="distributionUrl" set "DISTRIBUTION_URL=%%B"
)

if not defined DISTRIBUTION_URL (
  echo [ERROR] distributionUrl no configurado en %PROP_FILE%
  exit /b 1
)

for %%I in ("%DISTRIBUTION_URL%") do set "ARCHIVE_NAME=%%~nxI"
set "MAVEN_DIR_NAME=%ARCHIVE_NAME:-bin.zip=%"

if defined MAVEN_USER_HOME (
  set "M2_HOME_BASE=%MAVEN_USER_HOME%"
) else (
  set "M2_HOME_BASE=%USERPROFILE%\.m2"
)

set "WRAPPER_HOME=%M2_HOME_BASE%\wrapper\dists\%MAVEN_DIR_NAME%"
set "MAVEN_HOME=%WRAPPER_HOME%\%MAVEN_DIR_NAME%"
set "ARCHIVE=%WRAPPER_HOME%\%ARCHIVE_NAME%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%WRAPPER_HOME%" mkdir "%WRAPPER_HOME%"

  echo Downloading Maven from %DISTRIBUTION_URL%
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; Invoke-WebRequest -UseBasicParsing -Uri '%DISTRIBUTION_URL%' -OutFile '%ARCHIVE%'"
  if errorlevel 1 exit /b 1

  set "TMP_DIR=%WRAPPER_HOME%\.unpack-%RANDOM%-%RANDOM%"
  if exist "!TMP_DIR!" rmdir /s /q "!TMP_DIR!"
  mkdir "!TMP_DIR!"

  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; Expand-Archive -Force -Path '%ARCHIVE%' -DestinationPath '!TMP_DIR!'"
  if errorlevel 1 exit /b 1

  if exist "%MAVEN_HOME%" rmdir /s /q "%MAVEN_HOME%"
  move "!TMP_DIR!\%MAVEN_DIR_NAME%" "%MAVEN_HOME%" >nul
  rmdir /s /q "!TMP_DIR!"
  del /q "%ARCHIVE%"
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%

@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================
REM backend-personas - Maven Wrapper con JDK 21 aislado
REM NO usa el JAVA_HOME global de Windows.
REM ============================================================

if defined JAVA21_HOME (
  set "PROJECT_JAVA_HOME=%JAVA21_HOME%"
) else (
  set "PROJECT_JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1"
)

if not exist "%PROJECT_JAVA_HOME%\bin\java.exe" (
  echo.
  echo [ERROR] No se encuentra un JDK 21 en:
  echo   %PROJECT_JAVA_HOME%
  echo.
  echo Define JAVA21_HOME con la ruta real de tu JDK 21, por ejemplo:
  echo   set JAVA21_HOME=C:\Program Files\Java\jdk-21.0.12.1
  echo.
  exit /b 1
)

REM JAVA_HOME se cambia SOLO dentro de este proceso.
set "JAVA_HOME=%PROJECT_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [backend-personas] JAVA_HOME aislado: %JAVA_HOME%

set "BASE_DIR=%~dp0"
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

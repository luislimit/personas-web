@echo off
setlocal EnableExtensions

echo === JAVA GLOBAL ===
echo JAVA_HOME=%JAVA_HOME%
java -version
echo.

if defined JAVA21_HOME (
  set "PROJECT_JAVA_HOME=%JAVA21_HOME%"
) else (
  set "PROJECT_JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1"
)

echo === JAVA DEL PROYECTO ===
echo PROJECT_JAVA_HOME=%PROJECT_JAVA_HOME%

if not exist "%PROJECT_JAVA_HOME%\bin\java.exe" (
  echo [ERROR] No existe %PROJECT_JAVA_HOME%\bin\java.exe
  exit /b 1
)

"%PROJECT_JAVA_HOME%\bin\java.exe" -version
echo.

echo === MAVEN WRAPPER ===
call "%~dp0..\mvnw.cmd" -version

@echo off
setlocal EnableExtensions

echo ============================================================
echo backend-personas - comprobacion de entorno aislado
echo ============================================================
echo.

echo [GLOBAL - informativo]
echo JAVA_HOME=%JAVA_HOME%
where java 2>nul
java -version 2>&1
echo.
echo Lo anterior puede seguir mostrando Java 7. Es intencionado.
echo.

echo [PROYECTO]
call "%~dp0..\mvnw.cmd" -version
if errorlevel 1 (
    echo.
    echo [ERROR] El entorno aislado del proyecto no es correcto.
    exit /b 1
)

echo.
echo [OK] El Maven Wrapper del proyecto ha arrancado correctamente.

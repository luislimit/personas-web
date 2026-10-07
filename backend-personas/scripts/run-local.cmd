@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "BASE_DIR=%~dp0.."
set "LOCAL_ENV=%BASE_DIR%\config\local.env"

if not exist "%LOCAL_ENV%" (
    echo.
    echo [ERROR] No existe el fichero de configuracion local:
    echo   %LOCAL_ENV%
    echo.
    echo Copia:
    echo   config\local.env.example
    echo como:
    echo   config\local.env
    echo y completa la password de MySQL.
    echo.
    exit /b 1
)

REM Cargar KEY=VALUE sin ejecutar los caracteres especiales (&, ?, etc.)
for /f "usebackq tokens=1,* delims==" %%A in ("%LOCAL_ENV%") do (
    set "KEY=%%A"
    set "VALUE=%%B"
    if defined KEY (
        if not "!KEY:~0,1!"=="#" (
            set "!KEY!=!VALUE!"
        )
    )
)

if not defined DB_URL (
    set "DB_URL=jdbc:mysql://localhost:3306/gestion_personas?useUnicode=true&characterEncoding=utf8&serverTimezone=Europe/Madrid&allowPublicKeyRetrieval=true&useSSL=false"
)

if not defined DB_USERNAME (
    set "DB_USERNAME=root"
)

if not defined SERVER_PORT (
    set "SERVER_PORT=8080"
)

echo [backend-personas] Base de datos:
echo   URL:      !DB_URL!
echo   Usuario:  !DB_USERNAME!
echo   Password: ********
echo   Puerto:   !SERVER_PORT!
echo.

call "%BASE_DIR%\mvnw.cmd" spring-boot:run
exit /b %ERRORLEVEL%

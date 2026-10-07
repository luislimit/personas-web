@echo off
setlocal EnableExtensions

set "BASE_DIR=%~dp0.."
set "JAVA_DIR=%BASE_DIR%\src\main\java\es\gestionpersonas"
set "EXAMPLES_DIR=%BASE_DIR%\examples"
set "DB_DIR=%BASE_DIR%\database"

echo ============================================================
echo Gestion de Personas - aplicar refactor de nombres a espanol
echo ============================================================
echo.

echo [1/3] Eliminando paquetes Java anteriores en ingles...
for %%D in (account calendar common config document entity request security) do (
    if exist "%JAVA_DIR%\%%D" (
        echo   Eliminando %%D
        rmdir /s /q "%JAVA_DIR%\%%D"
    )
)

echo [2/3] Eliminando ejemplos HTTP antiguos en ingles...
for %%F in (requests.http security-auth.http sensitive-information.http calendar-recurrence.http) do (
    if exist "%EXAMPLES_DIR%\%%F" del /q "%EXAMPLES_DIR%\%%F"
)

echo [3/3] Retirando scripts de BD anteriores para evitar confusiones...
for %%F in (gestion_personas_full.sql gestion_personas_full_v3.sql gestion_personas_full_v4.sql gestion_personas_full_v5.sql gestion_personas_full_v6.sql) do (
    if exist "%DB_DIR%\%%F" del /q "%DB_DIR%\%%F"
)
if exist "%DB_DIR%\migrations" rmdir /s /q "%DB_DIR%\migrations"

for %%F in (README-INCREMENTAL-CORE-CRUD.md README-INCREMENTAL-SECURITY-V5.md README-INCREMENTAL-SENSITIVE-V6.md README-INCREMENTAL-CALENDAR-V4.md) do (
    if exist "%BASE_DIR%\%%F" del /q "%BASE_DIR%\%%F"
)

echo.
echo [OK] Refactor aplicado. Ya puedes ejecutar:
echo   .\mvnw.cmd clean test
echo   .\mvnw.cmd spring-boot:run
exit /b 0

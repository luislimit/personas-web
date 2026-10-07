@echo off
setlocal EnableExtensions

set "BASE_DIR=%~dp0.."

echo ============================================================
echo Limpieza de restos del refactor a espanol
echo ============================================================
echo.

REM Paquetes antiguos en ingles que ya no deben coexistir.
for %%D in (
  account
  calendar
  common
  config
  document
  entity
  request
  security
) do (
  if exist "%BASE_DIR%\src\main\java\es\gestionpersonas\%%D" (
    echo Eliminando paquete antiguo: %%D
    rmdir /s /q "%BASE_DIR%\src\main\java\es\gestionpersonas\%%D"
  )
)

REM También limpiar clases compiladas antiguas.
if exist "%BASE_DIR%\target" (
  echo Eliminando target para evitar clases antiguas...
  rmdir /s /q "%BASE_DIR%\target"
)

echo.
echo Limpieza completada.
echo.
echo Ejecuta ahora:
echo   .\mvnw.cmd clean test
echo   .\mvnw.cmd spring-boot:run
echo.
exit /b 0

@echo off
setlocal EnableExtensions

if "%API_PRUEBAS_URL%"=="" (
  set "API_PRUEBAS_URL=http://localhost:8080"
)

echo.
echo ============================================================
echo  Pruebas automaticas E2E - Gestion de Personas
echo ============================================================
echo API: %API_PRUEBAS_URL%
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "try { $r=Invoke-RestMethod -Method Get -Uri '%API_PRUEBAS_URL%/api/sistema/salud' -TimeoutSec 5; if ($r.estado -ne 'ACTIVO') { exit 2 } } catch { exit 1 }"

if errorlevel 2 (
  echo ERROR: /api/sistema/salud no devuelve ACTIVO.
  exit /b 2
)

if errorlevel 1 (
  echo ERROR: La API no responde en %API_PRUEBAS_URL%
  echo Arranca primero el backend con:
  echo   .\mvnw.cmd spring-boot:run
  exit /b 1
)

echo API disponible. Ejecutando JUnit E2E...
echo.

call .\mvnw.cmd "-Dtest=PruebasApiE2E" test
set "RC=%ERRORLEVEL%"

echo.
if "%RC%"=="0" (
  echo TODAS LAS PRUEBAS E2E HAN PASADO
) else (
  echo HAY PRUEBAS E2E FALLIDAS
  echo Revisa target\surefire-reports para ver el detalle.
)

exit /b %RC%

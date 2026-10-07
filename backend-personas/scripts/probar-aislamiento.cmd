@echo off
setlocal EnableExtensions

if "%API_PRUEBAS_URL%"=="" (
  set "API_PRUEBAS_URL=http://localhost:8080"
)

echo.
echo ============================================================
echo  Pruebas E2E - Aislamiento transversal de contextos
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
  exit /b 1
)

call .\mvnw.cmd "-Dtest=PruebasAislamientoE2E" test
exit /b %ERRORLEVEL%

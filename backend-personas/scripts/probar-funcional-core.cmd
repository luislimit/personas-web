@echo off
setlocal EnableExtensions

if "%API_PRUEBAS_URL%"=="" (
  set "API_PRUEBAS_URL=http://localhost:8085"
)

echo.
echo ============================================================
echo  Pruebas funcionales E2E - Calendarios y Solicitudes
echo ============================================================
echo API: %API_PRUEBAS_URL%
echo.

call .\mvnw.cmd "-Dtest=PruebasCalendariosE2E,PruebasSolicitudesE2E" test
exit /b %ERRORLEVEL%

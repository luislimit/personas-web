@echo off
setlocal EnableExtensions

if "%API_PRUEBAS_URL%"=="" set "API_PRUEBAS_URL=http://localhost:8085"

echo ============================================================
echo  GESTION DE PERSONAS - BATERIA E2E COMPLETA
echo ============================================================
echo API: %API_PRUEBAS_URL%
echo.

call .\mvnw.cmd "-Dtest=PruebasApiE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasEntidadesE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasDocumentosE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasAislamientoE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasSolicitudesE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasCalendariosE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasAuditoriaE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasReglasAlcanceE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

call .\mvnw.cmd "-Dtest=PruebasSpringSecurityE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

echo.
echo [ADMIN ACCESOS] Ejecutando PruebasAdministracionAccesosE2E...
call .\mvnw.cmd "-Dtest=PruebasAdministracionAccesosE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%


echo.
echo [CONTEXTOS JERARQUICOS] Ejecutando PruebasContextosJerarquicosE2E...
call .\mvnw.cmd "-Dtest=PruebasContextosJerarquicosE2E" test
if errorlevel 1 exit /b %ERRORLEVEL%

echo.
echo ============================================================

echo.
echo [MFA] Ejecutando PruebasMfaE2E...
call mvnw.cmd -Dtest=PruebasMfaE2E test
if errorlevel 1 exit /b %ERRORLEVEL%
echo  OK - TODAS LAS BATERIAS E2E HAN FINALIZADO CORRECTAMENTE
echo ============================================================
exit /b 0

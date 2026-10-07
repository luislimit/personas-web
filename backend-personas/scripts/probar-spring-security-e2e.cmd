@echo off
setlocal EnableExtensions
if "%API_PRUEBAS_URL%"=="" set "API_PRUEBAS_URL=http://localhost:8085"

echo ============================================================
echo  GESTION DE PERSONAS - SPRING SECURITY E2E
echo ============================================================
echo API: %API_PRUEBAS_URL%
echo.

call .\mvnw.cmd "-Dtest=PruebasSpringSecurityE2E" test
exit /b %ERRORLEVEL%

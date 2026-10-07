@echo off
setlocal
cd /d "%~dp0\.."
echo ============================================================
echo  GESTION DE PERSONAS - MFA / STEP-UP E2E
echo ============================================================
echo API: %API_PRUEBAS_URL%
echo.
call mvnw.cmd -Dtest=PruebasMfaE2E test
exit /b %ERRORLEVEL%

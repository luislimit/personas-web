@echo off
setlocal
cd /d "%~dp0.."
call mvnw.cmd "-Dtest=PruebasContextosJerarquicosE2E" test
exit /b %ERRORLEVEL%

@echo off
setlocal EnableExtensions

echo ============================================================
echo  GESTION DE PERSONAS - VALIDACION IDEMPOTENCIA E2E
echo ============================================================
echo.
echo Se ejecutara la bateria completa DOS VECES consecutivas,
echo sin recrear la BD y sin reiniciar el backend.
echo.

echo [1/2] Primera ejecucion...
call .\scripts\probar-todo-e2e.cmd
if errorlevel 1 (
  echo.
  echo ERROR - La primera ejecucion E2E ha fallado.
  exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Segunda ejecucion sobre la MISMA BD...
call .\scripts\probar-todo-e2e.cmd
if errorlevel 1 (
  echo.
  echo ERROR - La segunda ejecucion ha fallado: existe dependencia de residuo/orden.
  exit /b %ERRORLEVEL%
)

echo.
echo ============================================================
echo  OK - BATERIA E2E IDEMPOTENTE: 2 EJECUCIONES CORRECTAS
echo ============================================================
exit /b 0

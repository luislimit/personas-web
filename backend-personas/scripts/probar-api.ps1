param(
    [string]$Url = $(if ($env:API_PRUEBAS_URL) { $env:API_PRUEBAS_URL } else { "http://localhost:8080" })
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================================"
Write-Host " Pruebas automáticas E2E - Gestión de Personas"
Write-Host "============================================================"
Write-Host "API: $Url"
Write-Host ""

try {
    $salud = Invoke-RestMethod -Method Get -Uri "$Url/api/sistema/salud" -TimeoutSec 5
} catch {
    Write-Host "ERROR: La API no responde en $Url" -ForegroundColor Red
    Write-Host "Arranca primero el backend con:"
    Write-Host "  .\mvnw.cmd spring-boot:run"
    Write-Host ""
    Write-Host "Si usas otro puerto:"
    Write-Host '  $env:API_PRUEBAS_URL="http://localhost:8085"'
    Write-Host "  .\scripts\probar-api.ps1"
    exit 1
}

if ($salud.estado -ne "ACTIVO") {
    Write-Host "ERROR: /api/sistema/salud no devuelve ACTIVO" -ForegroundColor Red
    exit 1
}

$env:API_PRUEBAS_URL = $Url

Write-Host "API disponible. Ejecutando JUnit E2E..." -ForegroundColor Green
Write-Host ""

& .\mvnw.cmd "-Dtest=PruebasApiE2E" test
$codigo = $LASTEXITCODE

Write-Host ""
if ($codigo -eq 0) {
    Write-Host "TODAS LAS PRUEBAS E2E HAN PASADO" -ForegroundColor Green
} else {
    Write-Host "HAY PRUEBAS E2E FALLIDAS" -ForegroundColor Red
    Write-Host "Revisa target\surefire-reports para ver el detalle."
}

exit $codigo

$ErrorActionPreference = "Stop"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME no está definido. Debe apuntar a un JDK 21."
}

$java = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path $java)) {
    throw "No se encuentra java.exe en $java"
}

& $java -version
Write-Host ""
Write-Host "Maven Wrapper:"
& "$PSScriptRoot\..\mvnw.cmd" -version

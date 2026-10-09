<#
.SYNOPSIS
  Ejecuta la colección de Postman contra la API en marcha y guarda la evidencia.

.DESCRIPTION
  Requisitos: la API corriendo (mvnw spring-boot:run) y Node.js instalado (npx).
  Corre las 87 peticiones de docs\postman con Newman (la versión de línea de
  comandos de Postman) y deja en docs\evidencias:
    10-postman-resultado.txt   resultado de cada petición y cada verificación
    10-postman-reporte.html    reporte visual (ábrelo en el navegador)
    10-postman-junit.xml       resultado en formato JUnit

.PARAMETER BaseUrl
  URL de la API. Por omisión http://localhost:8080

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\evidencias-postman.ps1
#>
param([string] $BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
# Consola en UTF-8 para que los acentos de Maven/Newman se lean bien en Windows PowerShell 5.1
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$OutputEncoding = [System.Text.Encoding]::UTF8
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
$destino = Join-Path $raiz 'docs\evidencias'
New-Item -ItemType Directory -Force $destino | Out-Null

try {
    $salud = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
    Write-Host "API disponible en $BaseUrl (estado: $($salud.status))" -ForegroundColor Cyan
} catch {
    Write-Host "La API no responde en $BaseUrl. Arráncala primero en otra terminal con:  .\mvnw.cmd spring-boot:run" -ForegroundColor Red
    exit 1
}
if (-not (Get-Command npx -ErrorAction SilentlyContinue)) {
    Write-Host 'No se encontró npx. Instala Node.js (https://nodejs.org) o ejecuta la colección desde Postman (Run collection).' -ForegroundColor Red
    exit 1
}

$ErrorActionPreference = 'Continue'
& npx --yes -p newman -p newman-reporter-htmlextra newman run 'docs/postman/banco-onboarding.postman_collection.json' `
    -e 'docs/postman/local.postman_environment.json' --env-var "baseUrl=$BaseUrl" `
    -r 'cli,htmlextra,junit' --color off `
    --reporter-htmlextra-export 'docs/evidencias/10-postman-reporte.html' `
    --reporter-htmlextra-title 'Banco - Onboarding de clientes: pruebas de la API' `
    --reporter-junit-export 'docs/evidencias/10-postman-junit.xml' 2>&1 |
    ForEach-Object { "$_" } | Tee-Object -Variable salida
$codigo = $LASTEXITCODE
$salida | Set-Content -Encoding UTF8 (Join-Path $destino '10-postman-resultado.txt')

Write-Host ''
if ($codigo -eq 0) {
    Write-Host 'Listo: todas las verificaciones pasaron. Abre docs\evidencias\10-postman-reporte.html' -ForegroundColor Green
} else {
    Write-Host 'Algunas verificaciones fallaron. Revisa docs\evidencias\10-postman-resultado.txt' -ForegroundColor Red
    exit $codigo
}

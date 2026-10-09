<#
.SYNOPSIS
  Ejecuta todas las pruebas automatizadas y guarda la evidencia en docs\evidencias.

.DESCRIPTION
  Corre "mvnw verify": compila, ejecuta las pruebas unitarias (*Test) y las de
  integración (*IT, con un PostgreSQL 16 embebido que se descarga solo; no hace
  falta Docker ni tener PostgreSQL instalado) y genera la cobertura con JaCoCo.

  Deja en docs\evidencias:
    08-pruebas-automatizadas.txt   salida completa de Maven
    08-resumen-pruebas.md          tabla con el resultado de cada clase y cada prueba
    09-cobertura\index.html        reporte de cobertura (ábrelo en el navegador)

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\evidencias-pruebas.ps1
#>
$ErrorActionPreference = 'Stop'
# Consola en UTF-8 para que los acentos de Maven/Newman se lean bien en Windows PowerShell 5.1
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$OutputEncoding = [System.Text.Encoding]::UTF8
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
$destino = Join-Path $raiz 'docs\evidencias'
New-Item -ItemType Directory -Force $destino | Out-Null

Write-Host 'Ejecutando mvnw verify (la primera vez descarga dependencias y tarda unos minutos)...' -ForegroundColor Cyan
$env:MAVEN_OPTS = ("$env:MAVEN_OPTS -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8").Trim()
$ErrorActionPreference = 'Continue'   # Maven escribe avisos en stderr: no deben detener el script
& (Join-Path $raiz 'mvnw.cmd') -B verify 2>&1 | ForEach-Object { "$_" } | Tee-Object -Variable salida
$codigo = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
$salida | Set-Content -Encoding UTF8 (Join-Path $destino '08-pruebas-automatizadas.txt')

# ---------------------------------------------------------------- resumen en Markdown
function ANumero([string] $texto) {
    $valor = 0.0
    [void][double]::TryParse(($texto -replace ',', '.'), [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$valor)
    return $valor
}
$filas = @()
$casos = @()
foreach ($tipo in @(@{ Dir = 'target\surefire-reports'; Nombre = 'Unitaria' }, @{ Dir = 'target\failsafe-reports'; Nombre = 'Integración' })) {
    $carpeta = Join-Path $raiz $tipo.Dir
    if (-not (Test-Path $carpeta)) { continue }
    foreach ($archivo in Get-ChildItem -Path $carpeta -Filter 'TEST-*.xml') {
        $suite = ([xml](Get-Content -Raw -Encoding UTF8 $archivo.FullName)).DocumentElement
        $filas += [pscustomobject]@{
            Tipo = $tipo.Nombre; Clase = $suite.GetAttribute('name'); Pruebas = [int]$suite.GetAttribute('tests')
            Fallas = [int]$suite.GetAttribute('failures'); Errores = [int]$suite.GetAttribute('errors')
            Omitidas = [int]$suite.GetAttribute('skipped'); Segundos = ANumero $suite.GetAttribute('time')
        }
        foreach ($caso in $suite.SelectNodes('testcase')) {
            $estado = if ($caso.SelectSingleNode('failure') -or $caso.SelectSingleNode('error')) { 'FALLA' }
                      elseif ($caso.SelectSingleNode('skipped')) { 'omitida' } else { 'OK' }
            $casos += [pscustomobject]@{ Tipo = $tipo.Nombre; Clase = $suite.GetAttribute('name'); Prueba = $caso.GetAttribute('name'); Estado = $estado }
        }
    }
}

$md = New-Object System.Collections.Generic.List[string]
$md.Add('# Resultado de las pruebas automatizadas')
$md.Add('')
$md.Add("Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm') · Comando: ``mvnw verify`` · Resultado de Maven: $(if ($codigo -eq 0) { 'BUILD SUCCESS' } else { 'BUILD FAILURE' })")
$md.Add('')
$total = ($filas | Measure-Object Pruebas -Sum).Sum
$fallas = ($filas | Measure-Object Fallas -Sum).Sum + ($filas | Measure-Object Errores -Sum).Sum
$md.Add("**Total: $total pruebas, $fallas con falla o error.**")
$md.Add('')
$md.Add('| Tipo | Clase de prueba | Pruebas | Fallas | Errores | Omitidas | Tiempo (s) |')
$md.Add('|---|---|---:|---:|---:|---:|---:|')
foreach ($f in $filas | Sort-Object Tipo, Clase) {
    $md.Add("| $($f.Tipo) | $($f.Clase) | $($f.Pruebas) | $($f.Fallas) | $($f.Errores) | $($f.Omitidas) | $([math]::Round($f.Segundos, 2)) |")
}
$md.Add('')
$md.Add('## Detalle de cada prueba')
$md.Add('')
$md.Add('| Tipo | Clase | Prueba | Estado |')
$md.Add('|---|---|---|---|')
foreach ($c in $casos | Sort-Object Tipo, Clase) {
    $md.Add("| $($c.Tipo) | $($c.Clase) | $(($c.Prueba -replace '\|', '\|')) | $($c.Estado) |")
}
$md | Set-Content -Encoding UTF8 (Join-Path $destino '08-resumen-pruebas.md')

# ---------------------------------------------------------------- cobertura
$jacoco = Join-Path $raiz 'target\site\jacoco'
if (Test-Path $jacoco) {
    $cobertura = Join-Path $destino '09-cobertura'
    if (Test-Path $cobertura) { Remove-Item -Recurse -Force $cobertura }
    Copy-Item -Recurse $jacoco $cobertura
}

Write-Host ''
if ($codigo -eq 0 -and $fallas -eq 0) {
    Write-Host "Listo: $total pruebas sin fallas. Evidencia en docs\evidencias (08-*, 09-cobertura\index.html)." -ForegroundColor Green
} else {
    Write-Host "Hubo fallas (código de Maven $codigo, $fallas pruebas con falla o error). Revisa docs\evidencias\08-pruebas-automatizadas.txt" -ForegroundColor Red
    exit 1
}

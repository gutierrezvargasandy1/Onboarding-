#!/usr/bin/env bash
# =============================================================================
# Ejecuta todas las pruebas automatizadas y guarda la evidencia en docs/evidencias
# (versión para Linux y macOS; en Windows usa evidencias-pruebas.ps1).
#
# Corre "mvnw verify": compila, ejecuta las pruebas unitarias (*Test) y las de
# integración (*IT, con un PostgreSQL 16 embebido que se descarga solo; no hace
# falta Docker ni tener PostgreSQL instalado) y genera la cobertura con JaCoCo.
#
#   08-pruebas-automatizadas.txt   salida completa de Maven
#   08-resumen-pruebas.md          resultado de cada clase y de cada prueba
#   09-cobertura/index.html        reporte de cobertura
#
# Uso:  ./scripts/evidencias-pruebas.sh
# Nota: en Linux no lo ejecutes como root (PostgreSQL embebido no arranca como root).
# =============================================================================
set -uo pipefail
raiz="$(cd "$(dirname "$0")/.." && pwd)"
cd "$raiz"
destino="$raiz/docs/evidencias"
mkdir -p "$destino"

echo "Ejecutando ./mvnw verify (la primera vez descarga dependencias y tarda unos minutos)..."
./mvnw -B verify 2>&1 | tee "$destino/08-pruebas-automatizadas.txt"
codigo=${PIPESTATUS[0]}

# --------------------------------------------------------------- resumen en Markdown
resumen() {  # $1 = carpeta de reportes, $2 = tipo
  [ -d "$1" ] || return 0
  for archivo in "$1"/TEST-*.xml; do
    [ -e "$archivo" ] || continue
    awk -v tipo="$2" '
      function attr(linea, nombre,   r) {
        if (match(linea, " " nombre "=\"[^\"]*\"")) {
          r = substr(linea, RSTART + length(nombre) + 3, RLENGTH - length(nombre) - 4)
          gsub(/&quot;/, "\"", r); gsub(/&apos;/, "\047", r); gsub(/&lt;/, "<", r); gsub(/&gt;/, ">", r); gsub(/&amp;/, "\\&", r)
          return r
        }
        return ""
      }
      /<testsuite / { suite = attr($0, "name")
                      printf "S\037%s\037%s\037%s\037%s\037%s\037%s\037%s\n", tipo, suite, attr($0, "tests"), attr($0, "failures"), attr($0, "errors"), attr($0, "skipped"), attr($0, "time") }
      /<testcase /  { if (caso != "") printf "C\037%s\037%s\037%s\037%s\n", tipo, suite, caso, estado
                      caso = attr($0, "name"); estado = "OK"
                      if ($0 ~ /\/>[[:space:]]*$/) { printf "C\037%s\037%s\037%s\037%s\n", tipo, suite, caso, estado; caso = "" } }
      /<failure|<error/ { estado = "FALLA" }
      /<skipped/        { if (estado == "OK") estado = "omitida" }
      /<\/testcase>/    { if (caso != "") printf "C\037%s\037%s\037%s\037%s\n", tipo, suite, caso, estado; caso = "" }
    ' "$archivo"
  done
}
datos="$(resumen target/surefire-reports Unitaria; resumen target/failsafe-reports Integración)"
total=$(printf '%s\n' "$datos" | awk -F'\037' '$1 == "S" { t += $4 } END { print t + 0 }')
fallas=$(printf '%s\n' "$datos" | awk -F'\037' '$1 == "S" { f += $5 + $6 } END { print f + 0 }')
{
  echo "# Resultado de las pruebas automatizadas"
  echo
  echo "Fecha: $(date '+%Y-%m-%d %H:%M') · Comando: \`./mvnw verify\` · Resultado de Maven: $([ "$codigo" -eq 0 ] && echo 'BUILD SUCCESS' || echo 'BUILD FAILURE')"
  echo
  echo "**Total: $total pruebas, $fallas con falla o error.**"
  echo
  echo "| Tipo | Clase de prueba | Pruebas | Fallas | Errores | Omitidas | Tiempo (s) |"
  echo "|---|---|---:|---:|---:|---:|---:|"
  printf '%s\n' "$datos" | awk -F'\037' '$1 == "S" { gsub(/\|/, "\\|", $3); printf "| %s | %s | %s | %s | %s | %s | %s |\n", $2, $3, $4, $5, $6, $7, $8 }' | sort
  echo
  echo "## Detalle de cada prueba"
  echo
  echo "| Tipo | Clase | Prueba | Estado |"
  echo "|---|---|---|---|"
  printf '%s\n' "$datos" | awk -F'\037' '$1 == "C" { gsub(/\|/, "\\|", $3); gsub(/\|/, "\\|", $4); printf "| %s | %s | %s | %s |\n", $2, $3, $4, $5 }'
} > "$destino/08-resumen-pruebas.md"

# --------------------------------------------------------------- cobertura
if [ -d target/site/jacoco ]; then
  rm -rf "$destino/09-cobertura"
  cp -R target/site/jacoco "$destino/09-cobertura"
fi

echo
if [ "$codigo" -eq 0 ] && [ "$fallas" -eq 0 ]; then
  echo "Listo: $total pruebas sin fallas. Evidencia en docs/evidencias (08-*, 09-cobertura/index.html)."
else
  echo "Hubo fallas (código de Maven $codigo, $fallas pruebas con falla o error). Revisa docs/evidencias/08-pruebas-automatizadas.txt"
  exit 1
fi

#!/usr/bin/env bash
# =============================================================================
# Ejecuta la colección de Postman contra la API en marcha y guarda la evidencia
# (versión para Linux y macOS; en Windows usa evidencias-postman.ps1).
#
# Requisitos: la API corriendo (./mvnw spring-boot:run) y Node.js (npx).
#   10-postman-resultado.txt   resultado de cada petición y cada verificación
#   10-postman-reporte.html    reporte visual (ábrelo en el navegador)
#   10-postman-junit.xml       resultado en formato JUnit
#
# Uso:  ./scripts/evidencias-postman.sh [url-de-la-api]     (por omisión http://localhost:8080)
# =============================================================================
set -uo pipefail
raiz="$(cd "$(dirname "$0")/.." && pwd)"
cd "$raiz"
base_url="${1:-http://localhost:8080}"
mkdir -p docs/evidencias

if ! curl -fsS --max-time 5 "$base_url/actuator/health" > /dev/null; then
  echo "La API no responde en $base_url. Arráncala primero en otra terminal con:  ./mvnw spring-boot:run"
  exit 1
fi
if ! command -v npx > /dev/null; then
  echo "No se encontró npx. Instala Node.js (https://nodejs.org) o ejecuta la colección desde Postman (Run collection)."
  exit 1
fi

npx --yes -p newman -p newman-reporter-htmlextra newman run docs/postman/banco-onboarding.postman_collection.json \
  -e docs/postman/local.postman_environment.json --env-var "baseUrl=$base_url" \
  -r cli,htmlextra,junit --color off \
  --reporter-htmlextra-export docs/evidencias/10-postman-reporte.html \
  --reporter-htmlextra-title "Banco - Onboarding de clientes: pruebas de la API" \
  --reporter-junit-export docs/evidencias/10-postman-junit.xml 2>&1 | tee docs/evidencias/10-postman-resultado.txt
codigo=${PIPESTATUS[0]}

echo
if [ "$codigo" -eq 0 ]; then
  echo "Listo: todas las verificaciones pasaron. Abre docs/evidencias/10-postman-reporte.html"
else
  echo "Algunas verificaciones fallaron. Revisa docs/evidencias/10-postman-resultado.txt"
  exit "$codigo"
fi

#!/usr/bin/env bash
# Preparación interactiva explícita; no enrollment ni cambios de datos de negocio.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
bash ./setup-facial-linux.sh
if [[ ! -f ../.env || ! -f .env ]]; then
    printf '%s\n' 'NO LISTO: ejecute python3 facial-service/scripts/configure_local.py desde la raíz.' >&2
    exit 1
fi
# Doctor comprueba dependencias, claves, ODBC, conexión/esquema (SELECT), modelo y cámara.
if ! .venv/bin/python scripts/doctor.py --database --model --camera; then
    printf '%s\n' 'NO LISTO: corrija el diagnóstico anterior y repita la preparación.' >&2
    exit 1
fi
printf '%s\n' 'READY FOR DEMO' 'Preflight completado. Falta iniciar FastAPI, enrollment si no hay perfil y login real; no se ha validado identidad biométrica.'

#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ ! -x .venv/bin/python ]]; then
    printf '%s\n' 'Falta .venv. Ejecute prepare-demo-linux.sh.' >&2
    exit 1
fi
if ! .venv/bin/python scripts/doctor.py --configuration-only; then
    printf '%s\n' 'No se inicia FaceID: falta configuración válida o driver ODBC. Ejecute configure_local.py y prepare-demo-linux.sh.' >&2
    exit 1
fi
# Un worker: el bloqueo de cámara es por proceso. No activar --reload.
exec .venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8765 --workers 1

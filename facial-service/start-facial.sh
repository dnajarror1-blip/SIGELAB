#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ ! -x .venv/bin/python ]]; then
    printf '%s\n' 'Falta .venv. Consulte docs/FACIAL_INTEGRACION.md.' >&2
    exit 1
fi
# Un worker: el bloqueo de cámara es por proceso. No activar --reload.
exec .venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8765 --workers 1

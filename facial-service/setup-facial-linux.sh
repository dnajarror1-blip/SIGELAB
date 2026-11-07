#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ -n "${PYTHON_BIN:-}" ]]; then
    facial_python="$PYTHON_BIN"
elif [[ -x .venv/bin/python ]]; then
    facial_python=.venv/bin/python
else
    facial_python=''
    for candidate in python3 python3.14 python3.13 python3.12; do
        if command -v "$candidate" >/dev/null 2>&1 && "$candidate" -c 'import sys; sys.exit(0 if (3,12) <= sys.version_info[:2] <= (3,14) else 1)'; then
            facial_python="$candidate"
            break
        fi
    done
fi
if [[ -z "$facial_python" ]] || ! command -v "$facial_python" >/dev/null 2>&1; then
    printf '%s\n' 'Falta Python 3.12–3.14 con venv. Puede definir PYTHON_BIN.' >&2
    exit 2
fi
verificar_python() {
    "$1" -c 'import sys; ok=(3,12) <= sys.version_info[:2] <= (3,14); print("Python compatible: 3.12–3.14" if ok else "Se requiere Python 3.12–3.14"); sys.exit(0 if ok else 2)'
}
verificar_python "$facial_python"
if [[ -e .venv ]]; then
    if [[ ! -x .venv/bin/python ]]; then
        printf '%s\n' '.venv no contiene un intérprete válido; revíselo manualmente. No se sobrescribió.' >&2
        exit 2
    fi
    verificar_python .venv/bin/python
else
    "$facial_python" -m venv .venv
fi
# No reinstalar si las versiones e imports ya satisfacen el archivo actual.
if ! .venv/bin/python scripts/doctor.py --dependencies-only; then
    .venv/bin/python -m pip install -r requirements.txt
    .venv/bin/python -m pip check
    .venv/bin/python scripts/doctor.py --dependencies-only
fi
printf '%s\n' 'Dependencias preparadas. Configure .env con scripts/configure_local.py; después ejecute prepare-demo-linux.sh.'

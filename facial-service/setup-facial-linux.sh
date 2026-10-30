#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
facial_python="${PYTHON_BIN:-python3.12}"
if ! command -v -- "$facial_python" >/dev/null 2>&1; then
    printf '%s\n' 'Falta Python 3.12. Instálelo con soporte venv o defina PYTHON_BIN con su intérprete Python 3.12.' >&2
    exit 2
fi
verificar_python() {
    "$1" -c 'import sys; version = ".".join(map(str, sys.version_info[:3])); compatible = sys.version_info[:2] == (3, 12); print("Python " + version + (": compatible con SIGELAB facial." if compatible else ": incompatible; se requiere Python 3.12. Cree un entorno 3.12 sin cambiar requirements."), file=sys.stdout if compatible else sys.stderr); sys.exit(0 if compatible else 2)'
}
verificar_python "$facial_python"
if [[ -e .venv ]]; then
    if [[ ! -x .venv/bin/python ]]; then
        printf '%s\n' '.venv existe pero no contiene un intérprete válido. Revíselo manualmente; no se sobrescribirá.' >&2
        exit 2
    fi
    verificar_python .venv/bin/python
else
    "$facial_python" -m venv .venv
fi
.venv/bin/python -m pip install --upgrade pip
.venv/bin/python -m pip install -r requirements-dev.txt
printf '%s\n' 'Entorno Python 3.12 preparado. Configure .env manualmente según .env.example y lea docs/FACIAL_INTEGRACION.md.'

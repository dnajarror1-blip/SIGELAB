#!/usr/bin/env bash
# Enrollment inicial/administrativo de un ADMIN existente; no inicia sesión.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ ! -x .venv/bin/python ]]; then
    printf '%s\n' 'Falta .venv. Prepare Python 3.12 con setup-facial-linux.sh y configure .env.' >&2
    exit 2
fi
exec .venv/bin/python scripts/enroll_user.py "$@"

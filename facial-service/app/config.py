import os
from pathlib import Path

from dotenv import load_dotenv


# Carpeta principal del servicio facial.
BASE_DIR = Path(__file__).resolve().parent.parent

# Configuracion privada del servicio.
ENV_FILE = BASE_DIR / ".env"

# Entorno real > .env raíz compartido > .env Python (compatibilidad local).
# Sin interpolación: una contraseña con ${...} se conserva literalmente.
load_dotenv(BASE_DIR.parent / ".env", override=False, interpolate=False)
load_dotenv(ENV_FILE, override=False, interpolate=False)


def get_database_config():

    host = os.getenv("DB_HOST", "127.0.0.1")
    port = int(os.getenv("DB_PORT", "1433"))
    database = os.getenv("DB_NAME", "SIGELAB")
    username = os.getenv("DB_USER", "")
    password = os.getenv("DB_PASSWORD", "")

    encrypt = os.getenv(
        "DB_ENCRYPT", "true"
    ).lower() in ("true", "yes", "1")

    trust_certificate = os.getenv(
        "DB_TRUST_SERVER_CERTIFICATE", "false"
    ).lower() in ("true", "yes", "1")

    if not database:
        raise RuntimeError(
            "Falta configurar DB_NAME en el archivo .env"
        )

    if not username:
        raise RuntimeError(
            "Falta configurar DB_USER en el archivo .env"
        )

    if password in (
        "",
        "CAMBIAR_CONTRASENA",
        "COLOCA_AQUI_TU_CONTRASENA"
    ):
        raise RuntimeError(
            "Debe configurar una contraseña valida en el archivo .env"
        )

    return {
        "driver": os.getenv("DB_DRIVER", "ODBC Driver 18 for SQL Server"),
        "host": host,
        "port": port,
        "database": database,
        "username": username,
        "password": password,
        "encrypt": encrypt,
        "trust_certificate": trust_certificate
    }

def facial_threshold():
    """Sin umbral explícito no se concede una coincidencia."""
    import math
    try:
        value = float(os.environ["FACIAL_MATCH_THRESHOLD"])
    except (KeyError, ValueError) as error:
        raise RuntimeError("Configure FACIAL_MATCH_THRESHOLD antes de verificar") from error
    if not math.isfinite(value) or not 0 < value <= 1:
        raise RuntimeError("FACIAL_MATCH_THRESHOLD debe estar entre 0 y 1")
    return value


def capture_timeout():
    value = int(os.getenv("FACIAL_CAPTURE_TIMEOUT", "120"))
    if not 10 <= value <= 140:
        raise RuntimeError("FACIAL_CAPTURE_TIMEOUT debe estar entre 10 y 140 segundos")
    return value


def model_name():
    # El formato persistido identifica este modelo y sus 512 dimensiones.
    value = os.getenv("FACIAL_MODEL", "buffalo_l")
    if value != "buffalo_l":
        raise RuntimeError("El contrato de perfiles actual requiere buffalo_l")
    return value


def liveness_threshold():
    import math
    value = float(os.getenv("FACIAL_LIVENESS_THRESHOLD", "0.80"))
    if not math.isfinite(value) or not 0 < value <= 1:
        raise RuntimeError("FACIAL_LIVENESS_THRESHOLD no válido")
    return value

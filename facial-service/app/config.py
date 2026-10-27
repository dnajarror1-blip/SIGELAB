import os
from pathlib import Path

from dotenv import load_dotenv


# Carpeta principal del servicio facial.
BASE_DIR = Path(__file__).resolve().parent.parent

# Configuracion privada del servicio.
ENV_FILE = BASE_DIR / ".env"

load_dotenv(ENV_FILE, override=True)


def get_database_config():

    host = os.getenv("DB_HOST", "127.0.0.1")
    port = int(os.getenv("DB_PORT", "1433"))
    database = os.getenv("DB_NAME", "")
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
        "host": host,
        "port": port,
        "database": database,
        "username": username,
        "password": password,
        "encrypt": encrypt,
        "trust_certificate": trust_certificate
    }
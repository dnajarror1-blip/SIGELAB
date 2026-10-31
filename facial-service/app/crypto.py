
import base64
import os

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from . import config


PREFIJO = b"SF1"
TAMANO_NONCE = 12


def obtener_clave():
    clave_texto = os.getenv("FACIAL_ENCRYPTION_KEY", "").strip()

    if not clave_texto:
        raise RuntimeError(
            "Falta FACIAL_ENCRYPTION_KEY en facial-service/.env"
        )

    try:
        clave = base64.b64decode(clave_texto, validate=True)
    except (ValueError, base64.binascii.Error) as error:
        raise RuntimeError(
            "FACIAL_ENCRYPTION_KEY no tiene un formato Base64 valido"
        ) from error

    if len(clave) != 32:
        raise RuntimeError(
            "La clave de cifrado debe contener exactamente 32 bytes"
        )

    return clave


def obtener_datos_asociados(id_usuario):
    if (
        isinstance(id_usuario, bool)
        or not isinstance(id_usuario, int)
        or id_usuario <= 0
    ):
        raise ValueError("El ID del usuario debe ser un entero positivo")

    return (
        f"SIGELAB|PerfilFacial|v1|usuario:{id_usuario}"
    ).encode("utf-8")


def cifrar_embedding(id_usuario, embedding):
    if not isinstance(embedding, bytes) or not embedding:
        raise ValueError("El embedding debe contener bytes validos")

    clave = obtener_clave()
    nonce = os.urandom(TAMANO_NONCE)
    datos_asociados = obtener_datos_asociados(id_usuario)

    aesgcm = AESGCM(clave)

    contenido_cifrado = aesgcm.encrypt(
        nonce,
        embedding,
        datos_asociados
    )

    return PREFIJO + nonce + contenido_cifrado


def descifrar_embedding(id_usuario, contenido):
    longitud_minima = len(PREFIJO) + TAMANO_NONCE + 16

    if (
        not isinstance(contenido, bytes)
        or len(contenido) < longitud_minima
        or not contenido.startswith(PREFIJO)
    ):
        raise ValueError("Formato del embedding cifrado no valido")

    clave = obtener_clave()
    datos_asociados = obtener_datos_asociados(id_usuario)

    inicio_nonce = len(PREFIJO)
    fin_nonce = inicio_nonce + TAMANO_NONCE

    nonce = contenido[inicio_nonce:fin_nonce]
    datos_cifrados = contenido[fin_nonce:]

    aesgcm = AESGCM(clave)

    try:
        return aesgcm.decrypt(
            nonce,
            datos_cifrados,
            datos_asociados
        )
    except InvalidTag as error:
        raise ValueError(
            "No fue posible autenticar el embedding cifrado"
        ) from error

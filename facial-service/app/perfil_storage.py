
import math
import struct
from decimal import Decimal, InvalidOperation

import pyodbc

from .crypto import cifrar_embedding, descifrar_embedding
from .database import get_connection


DIMENSION_FACIAL = 512
MODELO_FACIAL = "buffalo_l"
BYTES_POR_DIMENSION = 4


def validar_embedding(embedding):
    """
    Verifica que el embedding tenga exactamente
    512 valores float32 válidos.
    """

    longitud_esperada = DIMENSION_FACIAL * BYTES_POR_DIMENSION

    if not isinstance(embedding, bytes):
        raise ValueError("El embedding debe ser de tipo bytes")

    if len(embedding) != longitud_esperada:
        raise ValueError(
            f"El embedding debe contener {longitud_esperada} bytes"
        )

    valores = struct.unpack("<512f", embedding)

    if not all(math.isfinite(valor) for valor in valores):
        raise ValueError(
            "El embedding contiene valores no validos"
        )

    if not any(valor != 0 for valor in valores):
        raise ValueError(
            "El embedding no puede contener solo ceros"
        )


def guardar_perfil_facial(
    id_usuario,
    embedding,
    cantidad_muestras,
    calidad,
    *,
    conexion=None,
    registrar_auditoria=True
):
    """
    Guarda o actualiza un perfil facial cifrado.

    Si se proporciona una conexión, el llamador
    controla la transacción.

    Si no se proporciona, esta función abre,
    confirma y cierra su propia conexión.
    """

    if type(id_usuario) is not int or id_usuario <= 0:
        raise ValueError("ID de usuario no valido")

    if (
        type(cantidad_muestras) is not int
        or cantidad_muestras <= 0
    ):
        raise ValueError("Cantidad de muestras no valida")

    try:
        calidad_decimal = Decimal(str(calidad))
    except (InvalidOperation, ValueError):
        raise ValueError("Calidad facial no valida")

    if (
        not calidad_decimal.is_finite()
        or calidad_decimal < 0
        or calidad_decimal > 1
    ):
        raise ValueError(
            "La calidad facial debe estar entre 0 y 1"
        )

    validar_embedding(embedding)

    embedding_cifrado = cifrar_embedding(
        id_usuario,
        embedding
    )

    conexion_propia = conexion is None

    if conexion_propia:
        conexion = get_connection()

    try:
        cursor = conexion.cursor()

        cursor.execute(
            """
            SELECT estado
            FROM dbo.Usuario
            WHERE id_usuario = ?
            """,
            id_usuario
        )

        usuario = cursor.fetchone()

        if (
            usuario is None
            or str(usuario[0]).strip().upper() != "ACTIVO"
        ):
            raise ValueError(
                "El usuario no existe o no esta activo"
            )

        # Actualizar el perfil si ya existe.
        cursor.execute(
            """
            UPDATE dbo.PerfilFacial
            SET
                embedding = ?,
                modelo = ?,
                dimension = ?,
                cantidad_muestras = ?,
                quality_score = ?,
                fecha_actualizacion = SYSUTCDATETIME(),
                activo = 1
            WHERE id_usuario = ?
            """,
            pyodbc.Binary(embedding_cifrado),
            MODELO_FACIAL,
            DIMENSION_FACIAL,
            cantidad_muestras,
            calidad_decimal,
            id_usuario
        )

        # Si no existía, crear un perfil.
        if cursor.rowcount == 0:
            cursor.execute(
                """
                INSERT INTO dbo.PerfilFacial (
                    id_usuario,
                    embedding,
                    modelo,
                    dimension,
                    cantidad_muestras,
                    quality_score,
                    fecha_registro,
                    fecha_actualizacion,
                    activo
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?,
                    SYSUTCDATETIME(),
                    NULL,
                    1
                )
                """,
                id_usuario,
                pyodbc.Binary(embedding_cifrado),
                MODELO_FACIAL,
                DIMENSION_FACIAL,
                cantidad_muestras,
                calidad_decimal
            )

        if registrar_auditoria:
            # Registrar auditoría sin fotografías ni embeddings.
            cursor.execute(
                """
                INSERT INTO dbo.AuditoriaFacial (
                    id_usuario,
                    fecha_hora,
                    accion,
                    resultado,
                    score,
                    threshold,
                    detalle,
                    dispositivo
                )
                VALUES (
                    ?,
                    SYSUTCDATETIME(),
                    'REGISTRO',
                    'EXITO',
                    NULL,
                    NULL,
                    'Perfil facial registrado',
                    'SERVICIO_LOCAL'
                )
                """,
                id_usuario
            )

        if conexion_propia:
            conexion.commit()

        return True

    except Exception:
        if conexion_propia:
            conexion.rollback()
        raise

    finally:
        if "cursor" in locals():
            cursor.close()
        if conexion_propia:
            conexion.close()


def obtener_embedding_facial(id_usuario, *, conexion=None):
    """
    Recupera y descifra el embedding del usuario.

    Devuelve None cuando no existe un perfil habilitado.
    Nunca imprime ni registra el embedding.
    """

    if type(id_usuario) is not int or id_usuario <= 0:
        raise ValueError("ID de usuario no valido")

    conexion_propia = conexion is None

    if conexion_propia:
        conexion = get_connection()

    try:
        cursor = conexion.cursor()

        cursor.execute(
            """
            SELECT
                p.embedding,
                p.modelo,
                p.dimension
            FROM dbo.PerfilFacial AS p
            INNER JOIN dbo.Usuario AS u
                ON u.id_usuario = p.id_usuario
            WHERE
                p.id_usuario = ?
                AND p.activo = 1
                AND u.estado = 'ACTIVO'
            """,
            id_usuario
        )

        perfil = cursor.fetchone()

        if perfil is None:
            return None

        embedding_cifrado = bytes(perfil[0])
        modelo = perfil[1]
        dimension = perfil[2]

        if modelo != MODELO_FACIAL:
            raise ValueError(
                "El modelo del perfil facial no coincide"
            )

        if dimension != DIMENSION_FACIAL:
            raise ValueError(
                "La dimension del perfil facial no es valida"
            )

        embedding = descifrar_embedding(
            id_usuario,
            embedding_cifrado
        )

        validar_embedding(embedding)

        return embedding

    finally:
        if "cursor" in locals():
            cursor.close()
        if conexion_propia:
            conexion.close()

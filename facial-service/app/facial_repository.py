
from .database import get_connection


def consultar_estado_facial(id_usuario):
    """
    Consulta el estado del usuario y su perfil facial.

    Esta función solo realiza consultas SELECT.
    No modifica información en SQL Server.
    """

    if (
        isinstance(id_usuario, bool)
        or not isinstance(id_usuario, int)
        or id_usuario <= 0
    ):
        raise ValueError(
            "El identificador del usuario debe ser un entero positivo"
        )

    conexion = get_connection()

    try:
        cursor = conexion.cursor()

        # 1. Consultar si existe el usuario
        cursor.execute(
            """
            SELECT estado
            FROM dbo.Usuario
            WHERE id_usuario = ?
            """,
            id_usuario
        )

        usuario = cursor.fetchone()

        if usuario is None:
            return {
                "id_usuario": id_usuario,
                "usuario_existe": False,
                "usuario_activo": False,
                "perfil_registrado": False,
                "perfil_activo": False,
                "modelo": None,
                "dimension": None,
                "cantidad_muestras": 0
            }

        estado_usuario = str(usuario[0]).strip().upper()

        # 2. Consultar metadatos del perfil facial
        # No recuperamos ni mostramos el embedding.
        cursor.execute(
            """
            SELECT
                modelo,
                dimension,
                cantidad_muestras,
                activo
            FROM dbo.PerfilFacial
            WHERE id_usuario = ?
            """,
            id_usuario
        )

        perfil = cursor.fetchone()

        if perfil is None:
            return {
                "id_usuario": id_usuario,
                "usuario_existe": True,
                "usuario_activo": estado_usuario == "ACTIVO",
                "perfil_registrado": False,
                "perfil_activo": False,
                "modelo": None,
                "dimension": None,
                "cantidad_muestras": 0
            }

        # 3. Devolver únicamente el estado del perfil
        return {
            "id_usuario": id_usuario,
            "usuario_existe": True,
            "usuario_activo": estado_usuario == "ACTIVO",
            "perfil_registrado": True,
            "perfil_activo": bool(perfil[3]),
            "modelo": perfil[0],
            "dimension": perfil[1],
            "cantidad_muestras": perfil[2]
        }

    finally:
        conexion.close()

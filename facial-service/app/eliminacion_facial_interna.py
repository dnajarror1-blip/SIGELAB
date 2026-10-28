"""
SIGELAB FaceID - Eliminacion interna de perfiles faciales.

Responsabilidades:
- Trabajar exclusivamente con SIGELAB_TEST_FACE.
- Validar el identificador del usuario.
- Comprobar que el usuario y su perfil existen.
- Eliminar el perfil biometrico.
- Conservar los registros historicos de auditoria.
- Registrar la eliminacion en AuditoriaFacial.
- Ejecutar commit o rollback.
- No devolver datos biometricos.

SEGURIDAD:

Esta funcion NO autentica ni autoriza al solicitante.

No debe exponerse mediante HTTP hasta implementar
una autorizacion administrativa verificable.

La eliminacion es permanente respecto al registro SQL.
No elimina automaticamente copias de respaldo.
"""

from .config import get_database_config
from .database import get_connection


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


def _eliminar_perfil_interno(id_usuario):
    """
    Elimina un perfil facial de la base de pruebas.

    Esta funcion es exclusivamente interna.

    La transaccion comprende:
    1. Validaciones SQL.
    2. Eliminacion del perfil.
    3. Registro de auditoria.

    Si alguna operacion falla, se ejecuta rollback.
    """

    # ------------------------------------------
    # 1. Validar identificador
    # ------------------------------------------

    if (
        type(id_usuario) is not int
        or id_usuario <= 0
    ):
        raise ValueError(
            "El identificador del usuario no es valido"
        )

    # ------------------------------------------
    # 2. Verificar configuracion
    # ------------------------------------------

    configuracion = get_database_config()

    if configuracion["database"] != BASE_PRUEBAS:
        raise RuntimeError(
            "Eliminacion bloqueada: "
            "base de datos no autorizada"
        )

    conexion = None

    try:

        # ------------------------------------------
        # 3. Abrir conexion transaccional
        # ------------------------------------------

        conexion = get_connection()

        conexion.autocommit = False

        cursor = conexion.cursor()

        base_actual = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()[0]

        if base_actual != BASE_PRUEBAS:
            raise RuntimeError(
                "La conexion no corresponde "
                "a SIGELAB_TEST_FACE"
            )

        # ------------------------------------------
        # 4. Verificar existencia del usuario
        # ------------------------------------------

        usuario = cursor.execute(
            """
            SELECT id_usuario
            FROM dbo.Usuario WITH (UPDLOCK, HOLDLOCK)
            WHERE id_usuario = ?
            """,
            id_usuario,
        ).fetchone()

        if usuario is None:
            raise ValueError(
                "El usuario no existe"
            )

        # ------------------------------------------
        # 5. Buscar y bloquear el perfil
        # ------------------------------------------

        perfil = cursor.execute(
            """
            SELECT id_perfil_facial
            FROM dbo.PerfilFacial WITH (UPDLOCK, HOLDLOCK)
            WHERE id_usuario = ?
            """,
            id_usuario,
        ).fetchone()

        if perfil is None:
            raise ValueError(
                "El usuario no tiene "
                "un perfil facial registrado"
            )

        id_perfil = int(perfil[0])

        # ------------------------------------------
        # 6. Eliminar perfil biometrico
        # ------------------------------------------

        eliminado = cursor.execute(
            """
            DELETE FROM dbo.PerfilFacial
            OUTPUT DELETED.id_perfil_facial
            WHERE id_perfil_facial = ?
              AND id_usuario = ?
            """,
            id_perfil,
            id_usuario,
        ).fetchone()

        if eliminado is None:
            raise RuntimeError(
                "No fue posible eliminar "
                "el perfil facial"
            )

        # ------------------------------------------
        # 7. Registrar auditoria
        # ------------------------------------------

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
                'ELIMINACION',
                'COMPLETADO',
                NULL,
                NULL,
                N'Perfil facial eliminado del sistema',
                N'SERVICIO_LOCAL'
            )
            """,
            id_usuario,
        )

        # ------------------------------------------
        # 8. Confirmar transaccion
        # ------------------------------------------

        conexion.commit()

        return {
            "userId": id_usuario,
            "result": "ELIMINADO",
            "deleted": True,
            "message": (
                "Perfil facial eliminado correctamente"
            ),
        }

    except Exception:

        if conexion is not None:
            conexion.rollback()

        raise

    finally:

        if conexion is not None:
            conexion.close()
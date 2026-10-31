"""
SIGELAB FaceID - Eliminacion interna de perfiles faciales.

Funciones:
- Validar identificadores.
- Verificar la base de pruebas.
- Comprobar usuario y perfil.
- Eliminar el perfil biometrico.
- Registrar auditoria.
- Proteger la operacion mediante transacciones.

La funcion admite una conexion externa para
realizar pruebas con rollback controlado.

IMPORTANTE:
No autentica ni autoriza al solicitante.
No exponer mediante HTTP sin autorizacion verificable.
"""

from .config import get_database_config
from .database import get_connection


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


def _eliminar_perfil_interno(
    id_usuario,
    *,
    conexion=None
):
    """
    Elimina un perfil facial de la base de pruebas.

    Si no se proporciona conexion:
    - Abre su propia conexion.
    - Ejecuta commit al finalizar.
    - Ejecuta rollback ante errores.
    - Cierra la conexion.

    Si se proporciona conexion:
    - Exige autocommit desactivado.
    - Utiliza la transaccion del llamador.
    - No ejecuta commit ni rollback.
    - No cierra la conexion.

    No verifica la identidad ni los permisos
    del solicitante.
    """

    # ------------------------------------------
    # 1. Identificador
    # ------------------------------------------

    if (
        type(id_usuario) is not int
        or id_usuario <= 0
    ):
        raise ValueError(
            "El identificador del usuario no es valido"
        )

    # ------------------------------------------
    # 2. Configuracion
    # ------------------------------------------

    configuracion = get_database_config()

    if configuracion["database"] != BASE_PRUEBAS:
        raise RuntimeError(
            "Eliminacion bloqueada: "
            "base de datos no autorizada"
        )

    # Identificar quien administra la transaccion.
    conexion_propia = conexion is None

    try:

        # ------------------------------------------
        # 3. Preparar conexion
        # ------------------------------------------

        if conexion_propia:

            conexion = get_connection()
            conexion.autocommit = False

        elif conexion.autocommit:

            raise RuntimeError(
                "Se requiere una conexion "
                "con autocommit desactivado"
            )

        cursor = conexion.cursor()

        # ------------------------------------------
        # 4. Verificar base real
        # ------------------------------------------

        fila_base = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()

        if (
            fila_base is None
            or fila_base[0] != BASE_PRUEBAS
        ):
            raise RuntimeError(
                "La conexion no corresponde "
                "a SIGELAB_TEST_FACE"
            )

        # ------------------------------------------
        # 5. Comprobar usuario
        # ------------------------------------------

        usuario = cursor.execute(
            """
            SELECT id_usuario
            FROM dbo.Usuario WITH (UPDLOCK, HOLDLOCK)
            WHERE id_usuario = ?
            """,
            id_usuario
        ).fetchone()

        if usuario is None:
            raise ValueError(
                "El usuario no existe"
            )

        # ------------------------------------------
        # 6. Comprobar perfil
        # ------------------------------------------

        perfil = cursor.execute(
            """
            SELECT id_perfil_facial
            FROM dbo.PerfilFacial WITH (UPDLOCK, HOLDLOCK)
            WHERE id_usuario = ?
            """,
            id_usuario
        ).fetchone()

        if perfil is None:
            raise ValueError(
                "El usuario no tiene "
                "un perfil facial registrado"
            )

        id_perfil = int(perfil[0])

        # ------------------------------------------
        # 7. Eliminar perfil
        # ------------------------------------------

        eliminado = cursor.execute(
            """
            DELETE FROM dbo.PerfilFacial
            OUTPUT DELETED.id_perfil_facial
            WHERE id_perfil_facial = ?
              AND id_usuario = ?
            """,
            id_perfil,
            id_usuario
        ).fetchone()

        if eliminado is None:
            raise RuntimeError(
                "No fue posible eliminar "
                "el perfil facial"
            )

        # ------------------------------------------
        # 8. Registrar auditoria
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
            id_usuario
        )

        # ------------------------------------------
        # 9. Confirmar si la conexion es propia
        # ------------------------------------------

        if conexion_propia:
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

        # Las conexiones externas son responsabilidad
        # del llamador, incluso cuando ocurre un error.

        if (
            conexion_propia
            and conexion is not None
        ):
            conexion.rollback()

        raise

    finally:

        if (
            conexion_propia
            and conexion is not None
        ):
            conexion.close()
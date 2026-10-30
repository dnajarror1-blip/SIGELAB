"""
SIGELAB - Proteccion del registro facial.

Este modulo:
- Verifica la base de pruebas.
- Comprueba que el usuario este ACTIVO.
- Impide registrar un perfil si ya existe.
- Utiliza el almacenamiento cifrado existente.
- Deja el control de la transaccion al llamador.

IMPORTANTE:
No autentica al usuario.
Debe utilizarse solamente despues de validar
su identidad mediante el backend de SIGELAB.
"""

from .perfil_storage import guardar_perfil_facial


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


def guardar_perfil_nuevo(
    id_usuario,
    embedding,
    cantidad_muestras,
    calidad,
    *,
    conexion
):
    """
    Registra un perfil facial exclusivamente si
    el usuario todavia no tiene uno.

    Requiere una conexion SQL Server con
    autocommit desactivado.

    No ejecuta commit ni rollback.
    El llamador controla la transaccion.
    """

    # 1. Validar identificador.
    if type(id_usuario) is not int or id_usuario <= 0:
        raise ValueError(
            "El identificador del usuario no es valido"
        )

    # 2. Exigir una conexion transaccional.
    if conexion is None:
        raise ValueError(
            "Se requiere una conexion a SQL Server"
        )

    if conexion.autocommit:
        raise RuntimeError(
            "El registro requiere una transaccion activa"
        )

    cursor = conexion.cursor()

    # 3. Confirmar la base de pruebas.
    cursor.execute(
        "SELECT DB_NAME()"
    )

    base_actual = cursor.fetchone()[0]

    if base_actual != BASE_PRUEBAS:
        raise RuntimeError(
            "Registro bloqueado: "
            "la base de datos no esta autorizada"
        )

    # 4. Comprobar existencia y estado del usuario.
    cursor.execute(
        """
        SELECT estado
        FROM dbo.Usuario WITH (UPDLOCK, HOLDLOCK)
        WHERE id_usuario = ?
        """,
        id_usuario
    )

    usuario = cursor.fetchone()

    if usuario is None:
        raise ValueError(
            "El usuario no existe"
        )

    if str(usuario[0]).strip().upper() != "ACTIVO":
        raise ValueError(
            "El usuario no esta activo"
        )

    # 5. Impedir sobrescribir perfiles existentes.
    cursor.execute(
        """
        SELECT 1
        FROM dbo.PerfilFacial WITH (UPDLOCK, HOLDLOCK)
        WHERE id_usuario = ?
        """,
        id_usuario
    )

    perfil_existente = cursor.fetchone()

    if perfil_existente is not None:
        raise ValueError(
            "El usuario ya tiene un perfil facial. "
            "No se permite sobrescribirlo."
        )

    # 6. Delegar el cifrado, guardado y auditoria.
    resultado = guardar_perfil_facial(
        id_usuario=id_usuario,
        embedding=embedding,
        cantidad_muestras=cantidad_muestras,
        calidad=calidad,
        conexion=conexion
    )

    return resultado
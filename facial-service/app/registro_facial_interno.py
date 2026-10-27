"""
SIGELAB FaceID - Coordinacion interna del registro facial.

Este modulo:
- Trabaja exclusivamente con SIGELAB_TEST_FACE.
- Comprueba el estado del usuario.
- Utiliza el bloqueo compartido de camara.
- Prepara seis muestras con liveness.
- Almacena el perfil mediante el registro protegido.
- Controla commit y rollback.
- No devuelve fotografias ni embeddings.

IMPORTANTE:
No autentica ni autoriza al solicitante.

NO exponer esta funcion como endpoint HTTP hasta que
SIGELAB disponga de una autorizacion verificable
del administrador que solicita el registro.
"""

import math

from .camara_control import BLOQUEO_CAMARA
from .config import get_database_config
from .database import get_connection, probar_conexion
from .facial_enrollment import (
    MUESTRAS_REQUERIDAS,
    preparar_perfil_preliminar,
)
from .facial_repository import consultar_estado_facial
from .perfil_storage import validar_embedding
from .registro_facial_nuevo import guardar_perfil_nuevo


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


def _verificar_entorno_pruebas():
    """
    Comprueba la configuracion y la base real
    antes de comenzar una captura.
    """

    configuracion = get_database_config()

    if configuracion["database"] != BASE_PRUEBAS:
        raise RuntimeError(
            "Registro bloqueado: base de datos no autorizada"
        )

    resultado = probar_conexion()

    if resultado["base_datos"] != BASE_PRUEBAS:
        raise RuntimeError(
            "La conexion no corresponde a SIGELAB_TEST_FACE"
        )

    tablas = set(
        resultado["tablas_faciales"]
    )

    if not {
        "PerfilFacial",
        "AuditoriaFacial",
    }.issubset(tablas):

        raise RuntimeError(
            "Faltan tablas necesarias para FaceID"
        )


def _verificar_usuario_preliminar(id_usuario):
    """
    Realiza una comprobacion antes de abrir la camara.

    El guardado protegido volvera a verificar
    estos estados dentro de la transaccion.
    """

    estado = consultar_estado_facial(
        id_usuario
    )

    if not estado["usuario_existe"]:
        raise ValueError(
            "El usuario no existe"
        )

    if not estado["usuario_activo"]:
        raise ValueError(
            "El usuario no esta activo"
        )

    if estado["perfil_registrado"]:
        raise ValueError(
            "El usuario ya tiene un perfil facial"
        )


def _validar_perfil_preliminar(perfil):
    """
    Verifica los resultados antes de solicitar
    su almacenamiento.
    """

    if not isinstance(perfil, dict):
        raise ValueError(
            "El perfil preliminar no es valido"
        )

    embedding = perfil["embedding"]

    validar_embedding(
        embedding
    )

    cantidad = perfil["cantidad_muestras"]

    if (
        type(cantidad) is not int
        or cantidad != MUESTRAS_REQUERIDAS
    ):
        raise ValueError(
            "El registro no contiene seis muestras validas"
        )

    calidad = float(
        perfil["confianza_deteccion_media"]
    )

    liveness = float(
        perfil["liveness_promedio"]
    )

    if (
        not math.isfinite(calidad)
        or not 0.0 <= calidad <= 1.0
    ):
        raise ValueError(
            "Calidad facial fuera de rango"
        )

    if (
        not math.isfinite(liveness)
        or not 0.0 <= liveness <= 1.0
    ):
        raise ValueError(
            "Resultado de liveness fuera de rango"
        )

    return (
        embedding,
        cantidad,
        calidad,
        liveness
    )


def _registrar_perfil_interno(id_usuario):
    """
    Coordina la preparacion y el guardado facial.

    FUNCION INTERNA SIN AUTORIZACION DE SOLICITANTE.
    No debe conectarse directamente a una ruta HTTP.

    La captura se realiza antes de iniciar
    la transaccion de escritura SQL Server.
    """

    # --------------------------------------------------
    # 1. Identificador
    # --------------------------------------------------

    if (
        type(id_usuario) is not int
        or id_usuario <= 0
    ):
        raise ValueError(
            "Identificador de usuario no valido"
        )

    # --------------------------------------------------
    # 2. Validaciones previas
    # --------------------------------------------------

    _verificar_entorno_pruebas()

    _verificar_usuario_preliminar(
        id_usuario
    )

    # --------------------------------------------------
    # 3. Reservar la camara
    # --------------------------------------------------

    if not BLOQUEO_CAMARA.acquire(
        blocking=False
    ):
        raise RuntimeError(
            "La camara esta siendo utilizada"
        )

    try:

        # Preparar las seis muestras.
        perfil = preparar_perfil_preliminar()

    finally:

        # Liberar incluso cuando existe un error.
        BLOQUEO_CAMARA.release()

    # --------------------------------------------------
    # 4. Cancelacion
    # --------------------------------------------------

    if perfil is None:
        return {
            "userId": id_usuario,
            "result": "CANCELADO",
            "registered": False,
            "message": "Registro facial cancelado",
        }

    # --------------------------------------------------
    # 5. Validar resultado de captura
    # --------------------------------------------------

    (
        embedding,
        cantidad,
        calidad,
        liveness
    ) = _validar_perfil_preliminar(
        perfil
    )

    # --------------------------------------------------
    # 6. Iniciar transaccion
    # --------------------------------------------------

    conexion = None

    try:

        conexion = get_connection()

        conexion.autocommit = False

        # Esta funcion vuelve a comprobar:
        # - Base de datos.
        # - Existencia del usuario.
        # - Estado ACTIVO.
        # - Ausencia de un perfil previo.
        #
        # Tambien cifra y registra auditoria.

        guardar_perfil_nuevo(
            id_usuario=id_usuario,
            embedding=embedding,
            cantidad_muestras=cantidad,
            calidad=calidad,
            conexion=conexion
        )

        conexion.commit()

    except Exception:

        if conexion is not None:
            conexion.rollback()

        raise

    finally:

        if conexion is not None:
            conexion.close()

    # --------------------------------------------------
    # 7. Respuesta sin datos biometricos
    # --------------------------------------------------

    return {
        "userId": id_usuario,
        "result": "REGISTRADO",
        "registered": True,
        "cantidadMuestras": cantidad,
        "calidad": round(calidad, 4),
        "livenessPromedio": round(liveness, 4),
        "message": "Perfil facial registrado correctamente",
    }
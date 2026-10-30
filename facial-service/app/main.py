"""
API local de diagnostico facial para SIGELAB.

Esta version SOLO opera con SIGELAB_TEST_FACE.
No autoriza el inicio de sesion.
"""

import hmac
from typing import Annotated

from dotenv import dotenv_values
from fastapi import Depends, FastAPI, Header, HTTPException, Path
from pydantic import BaseModel, Field

from .config import ENV_FILE, get_database_config
from .database import get_connection, probar_conexion
from .facial_pipeline import (
    UMBRAL_LIVENESS_INICIAL,
    analizar_captura_facial,
)
from .facial_repository import consultar_estado_facial
from .perfil_storage import obtener_embedding_facial, validar_embedding
from .camara_control import BLOQUEO_CAMARA


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


app = FastAPI(
    title="SIGELAB FaceID",
    description="Servicio de reconocimiento facial de SIGELAB",
    version="0.2.0",
)


class SolicitudVerificacion(BaseModel):
    userId: int = Field(gt=0)


def validar_clave_api(
    x_facial_api_key: str | None = Header(
        default=None,
        alias="X-Facial-Api-Key",
    ),
):
    """
    Protege el acceso a los endpoints internos.
    """

    variables = dotenv_values(ENV_FILE)

    clave_servidor = (
        variables.get("FACIAL_API_KEY") or ""
    ).strip()

    if len(clave_servidor) < 32:
        raise HTTPException(
            status_code=503,
            detail="La seguridad de FaceID no esta configurada",
        )

    if (
        not x_facial_api_key
        or not hmac.compare_digest(
            x_facial_api_key,
            clave_servidor,
        )
    ):
        raise HTTPException(
            status_code=401,
            detail="Acceso no autorizado al servicio facial",
        )

    return True


def verificar_base_face():
    """
    Impide ejecutar las pruebas contra otras bases.
    """

    config = get_database_config()

    if config["database"] != BASE_PRUEBAS:
        raise RuntimeError(
            "Base de datos no autorizada para pruebas FaceID"
        )

    resultado = probar_conexion()

    if resultado["base_datos"] != BASE_PRUEBAS:
        raise RuntimeError(
            "La conexion no corresponde a SIGELAB_TEST_FACE"
        )

    tablas = set(resultado["tablas_faciales"])

    if not {
        "PerfilFacial",
        "AuditoriaFacial",
    }.issubset(tablas):
        raise RuntimeError(
            "Faltan tablas necesarias para FaceID"
        )

    return resultado


@app.get("/health")
def health():
    """
    Comprueba SQL Server sin inicializar la camara.
    """

    try:
        resultado = verificar_base_face()

        return {
            "status": "UP",
            "service": "SIGELAB Facial Service",
            "ready": False,
            "modelLoaded": False,
            "cameraReady": False,
            "database": "ok",
            "databaseName": resultado["base_datos"],
            "tablesReady": True,
        }

    except Exception:
        return {
            "status": "DEGRADED",
            "service": "SIGELAB Facial Service",
            "ready": False,
            "modelLoaded": False,
            "cameraReady": False,
            "database": "error",
            "databaseName": None,
            "tablesReady": False,
        }


@app.get(
    "/face/status/{userId}",
    dependencies=[Depends(validar_clave_api)],
)
def consultar_perfil(
    userId: Annotated[int, Path(gt=0)]
):
    """
    Devuelve los metadatos del perfil facial.
    """

    try:
        verificar_base_face()

        return consultar_estado_facial(userId)

    except Exception:
        raise HTTPException(
            status_code=503,
            detail="No fue posible consultar el perfil facial",
        )


def _guardar_auditoria(
    id_usuario,
    resultado,
    detalle,
    similitud=None,
):
    """
    Registra eventos sin almacenar fotografias
    ni embeddings.
    """

    conexion = get_connection()

    try:
        cursor = conexion.cursor()

        base_actual = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()[0]

        if base_actual != BASE_PRUEBAS:
            raise RuntimeError(
                "Base de datos no autorizada"
            )

        score = (
            None
            if similitud is None
            else round(float(similitud), 5)
        )

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
                'VERIFICACION',
                ?,
                ?,
                NULL,
                ?,
                'SERVICIO_LOCAL'
            )
            """,
            id_usuario,
            resultado,
            score,
            detalle,
        )

        conexion.commit()

    except Exception:
        conexion.rollback()
        raise

    finally:
        conexion.close()


def registrar_auditoria_verificacion(
    id_usuario,
    resultado_pipeline,
):
    """
    Registra el resultado diagnostico de liveness
    y reconocimiento facial.
    """

    if (
        resultado_pipeline["livenessStatus"]
        == "input_rejected"
    ):
        detalle = "Liveness: captura no concluyente"

    elif resultado_pipeline["livenessPassed"] is False:
        detalle = "Liveness: posible suplantacion"

    else:
        detalle = (
            "Liveness: presencial; "
            "comparacion de identidad diagnostica"
        )

    _guardar_auditoria(
        id_usuario,
        resultado="DIAGNOSTICO",
        detalle=detalle,
        similitud=resultado_pipeline["similarity"],
    )


def registrar_auditoria_incidencia(
    id_usuario,
    resultado,
):
    """
    Registra cancelaciones y errores de validacion.
    """

    detalles_permitidos = {
        "CANCELADO": (
            "Captura cancelada por el usuario"
        ),
        "ERROR_VALIDACION": (
            "La captura o el perfil no supero la validacion"
        ),
    }

    if resultado not in detalles_permitidos:
        raise ValueError(
            "Resultado de auditoria no permitido"
        )

    _guardar_auditoria(
        id_usuario,
        resultado=resultado,
        detalle=detalles_permitidos[resultado],
    )

def registrar_rechazo_por_estado(
    id_usuario,
    causa,
):
    """
    Registra verificaciones rechazadas cuando
    cambia el estado del usuario o perfil facial.

    No registra fotografias ni embeddings.
    """

    detalles_permitidos = {
        "USUARIO_DESACTIVADO": (
            "La cuenta fue desactivada "
            "durante la verificacion facial"
        ),
        "PERFIL_DESACTIVADO": (
            "El perfil facial fue desactivado "
            "durante la verificacion"
        ),
    }

    if causa not in detalles_permitidos:
        raise ValueError(
            "Causa de rechazo no permitida"
        )

    _guardar_auditoria(
        id_usuario,
        resultado="RECHAZADO_ESTADO",
        detalle=detalles_permitidos[causa],
    )

@app.post(
    "/face/verify",
    dependencies=[Depends(validar_clave_api)],
)
def verificar_rostro_api(
    solicitud: SolicitudVerificacion
):
    """
    Verifica presencia real y compara identidad.

    Esta funcion no autoriza el inicio de sesion.
    """

    id_usuario = solicitud.userId

    # Evitar dos capturas simultaneas.
    if not BLOQUEO_CAMARA.acquire(blocking=False):
        raise HTTPException(
            status_code=409,
            detail="La camara esta siendo utilizada",
        )

    try:
        # ------------------------------------------
        # 1. Verificar base de datos
        # ------------------------------------------

        verificar_base_face()

        # ------------------------------------------
        # 2. Verificar usuario
        # ------------------------------------------

        estado = consultar_estado_facial(
            id_usuario
        )

        if not estado["usuario_existe"]:
            raise HTTPException(
                status_code=404,
                detail="Usuario no encontrado",
            )

        if not estado["usuario_activo"]:
            raise HTTPException(
                status_code=403,
                detail="El usuario esta inactivo",
            )

        if not estado["perfil_activo"]:
            raise HTTPException(
                status_code=409,
                detail=(
                    "El usuario no tiene "
                    "un perfil facial activo"
                ),
            )

        # ------------------------------------------
        # 3. Recuperar perfil cifrado
        # ------------------------------------------

        embedding_registrado = (
            obtener_embedding_facial(id_usuario)
        )

        if embedding_registrado is None:
            raise HTTPException(
                status_code=409,
                detail=(
                    "No fue posible recuperar "
                    "el perfil facial"
                ),
            )

        validar_embedding(
            embedding_registrado
        )

        # ------------------------------------------
        # 4. Capturar rostro
        # ------------------------------------------

        from scripts.diagnostico_embedding import (
            capturar_rostro
        )

        print("")
        print(
            "SIGELAB - Verificacion facial mediante API"
        )
        print(f"Usuario interno: {id_usuario}")
        print("Esperando captura de camara...")

        imagen = capturar_rostro()

        if imagen is None:
            registrar_auditoria_incidencia(
                id_usuario,
                "CANCELADO",
            )

            raise HTTPException(
                status_code=409,
                detail=(
                    "La captura facial fue cancelada"
                ),
            )

        # ------------------------------------------
        # 5. Presencia real + reconocimiento
        # ------------------------------------------

        resultado_pipeline = analizar_captura_facial(
            imagen,
            embedding_registrado,
        )

        # ------------------------------------------
        # 6. Revalidar estado despues de la captura
        # ------------------------------------------

        estado_final = consultar_estado_facial(
            id_usuario
        )

        # El usuario pudo haber sido eliminado.
        if not estado_final["usuario_existe"]:
            raise HTTPException(
                status_code=404,
                detail=(
                    "El usuario ya no existe "
                    "al finalizar la verificacion"
                ),
            )

                # El administrador pudo haberlo desactivado.
        if not estado_final["usuario_activo"]:

            registrar_rechazo_por_estado(
                id_usuario,
                "USUARIO_DESACTIVADO",
            )

            raise HTTPException(
                status_code=403,
                detail=(
                    "La cuenta fue desactivada "
                    "durante la verificacion facial"
                ),
            )

        # El perfil facial pudo haber sido desactivado.
        if not estado_final["perfil_activo"]:

            registrar_rechazo_por_estado(
                id_usuario,
                "PERFIL_DESACTIVADO",
            )

            raise HTTPException(
                status_code=409,
                detail=(
                    "El perfil facial dejo de estar activo "
                    "durante la verificacion"
                ),
            )

        # ------------------------------------------
        # 7. Registrar auditoria
        # ------------------------------------------

        registrar_auditoria_verificacion(
            id_usuario,
            resultado_pipeline,
        )

        # ------------------------------------------
        # 8. Preparar respuesta
        # ------------------------------------------

        # Captura no concluyente.
        if (
            resultado_pipeline["livenessStatus"]
            == "input_rejected"
        ):
            mensaje = (
                "Captura no concluyente para liveness. "
                "Intente otra vez."
            )

        # La captura no supero la prueba de presencia real.
        elif (
            resultado_pipeline["livenessPassed"]
            is False
        ):
            mensaje = (
                "La captura no supero la comprobacion "
                "de presencia real."
            )

        # Analisis completado.
        else:
            mensaje = (
                "Presencia real evaluada y comparacion "
                "facial completada. La autenticacion "
                "todavia no esta habilitada."
            )

        return {
            "userId": id_usuario,
            "result": "DIAGNOSTIC_ONLY",
            "similarity": resultado_pipeline["similarity"],
            "verified": False,
            "authenticationAllowed": False,
            "livenessPassed": (
                resultado_pipeline["livenessPassed"]
            ),
            "livenessStatus": (
                resultado_pipeline["livenessStatus"]
            ),
            "livenessScore": (
                resultado_pipeline["livenessScore"]
            ),
            "livenessThreshold": (
                UMBRAL_LIVENESS_INICIAL
            ),
            "threshold": None,
            "message": mensaje,
        }

    except HTTPException:
        raise

    except ValueError as error:
        try:
            registrar_auditoria_incidencia(
                id_usuario,
                "ERROR_VALIDACION",
            )

        except Exception:
            raise HTTPException(
                status_code=503,
                detail=(
                    "No fue posible registrar "
                    "la incidencia facial"
                ),
            )

        raise HTTPException(
            status_code=422,
            detail=str(error),
        )

    except Exception:
        raise HTTPException(
            status_code=503,
            detail=(
                "No fue posible completar "
                "la verificacion facial"
            ),
        )

    finally:
        BLOQUEO_CAMARA.release()
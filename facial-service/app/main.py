"""API biométrica local. Java conserva autenticación, permisos y navegación."""
import hmac
import os
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException, Path, Query
from pydantic import BaseModel, ConfigDict, Field

from . import auth_service
from .administracion_repository import RechazoFacial, registrar_evento
from .config import BASE_DIR, facial_threshold, model_name, liveness_threshold, capture_timeout
from .crypto import obtener_clave
from .database import comprobar_esquema
from .facial_repository import consultar_estado_facial
from .camara_control import BLOQUEO_CAMARA

app = FastAPI(title="SIGELAB FaceID", version="0.3.0")


class SolicitudVerificacion(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    userId: int = Field(gt=0)


class SolicitudRegistro(SolicitudVerificacion):
    adminUserId: int = Field(gt=0)
    replace: bool = False


def validar_clave_api(x_facial_api_key: str | None = Header(default=None, alias="X-Facial-Api-Key")):
    clave = os.getenv("FACIAL_API_KEY", "").strip()
    if len(clave) < 32:
        raise HTTPException(503, "La seguridad de FaceID no está configurada")
    if not x_facial_api_key or not hmac.compare_digest(x_facial_api_key.encode(), clave.encode()):
        raise HTTPException(401, "Acceso no autorizado al servicio facial")


def verificar_base_face():
    # Nombre retenido por compatibilidad con pruebas; ya no es una restricción a TEST_FACE.
    return comprobar_esquema()


@app.get("/health")
def health():
    database_ok = False
    configured = False
    try:
        verificar_base_face()
        database_ok = True
        obtener_clave()
        facial_threshold()
        liveness_threshold()
        capture_timeout()
        configured = len(os.getenv("FACIAL_API_KEY", "").strip()) >= 32
        models_present = (BASE_DIR / "models" / model_name()).is_dir()
    except Exception:
        models_present = False
    return {"status": "UP" if database_ok and configured and models_present else "DEGRADED",
            "database": "ok" if database_ok else "error", "configurationReady": configured,
            "modelsPresent": models_present, "modelLoaded": False,
            "cameraReady": False, "ready": False,
            "message": "La disponibilidad de cámara y liveness requiere diagnóstico interactivo"}


def ejecutar(operacion):
    try:
        verificar_base_face()
        return operacion()
    except RechazoFacial as error:
        raise HTTPException(error.codigo, str(error)) from None
    except Exception:
        raise HTTPException(503, "No fue posible completar la operación facial") from None


@app.get("/face/status/{userId}", dependencies=[Depends(validar_clave_api)])
def consultar_perfil(userId: Annotated[int, Path(gt=0)]):
    return ejecutar(lambda: consultar_estado_facial(userId))


@app.post("/face/verify", dependencies=[Depends(validar_clave_api)])
def verificar_rostro_api(solicitud: SolicitudVerificacion):
    return ejecutar(lambda: auth_service.verificar(solicitud.userId))


@app.post("/face/enroll", dependencies=[Depends(validar_clave_api)])
def registrar_rostro_api(solicitud: SolicitudRegistro):
    return ejecutar(lambda: auth_service.registrar(solicitud.userId, solicitud.adminUserId, solicitud.replace))


@app.delete("/face/profile/{userId}", dependencies=[Depends(validar_clave_api)])
def eliminar_perfil_api(userId: Annotated[int, Path(gt=0)], adminUserId: Annotated[int, Query(gt=0)]):
    return ejecutar(lambda: auth_service.eliminar(userId, adminUserId))


def _guardar_auditoria(id_usuario, resultado, detalle, similitud=None):
    # Adaptador para la prueba de auditoría ya existente. SQL permanece en Repository.
    registrar_evento(id_usuario, "VERIFICACION", resultado, detalle, similitud)


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

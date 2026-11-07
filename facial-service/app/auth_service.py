"""Reglas biométricas operativas; SQL y captura se delegan a sus fronteras."""
import math
from contextlib import contextmanager

from . import administracion_repository as repository
from .camara_control import BLOQUEO_CAMARA
from .capture_client import ejecutar_captura, ErrorCaptura
from .config import facial_threshold, liveness_threshold
from .crypto import obtener_clave
from .perfil_storage import obtener_embedding_facial, validar_embedding


@contextmanager
def reservar_camara():
    if not BLOQUEO_CAMARA.acquire(blocking=False):
        raise repository.RechazoFacial(409, "La cámara está siendo utilizada")
    try:
        yield
    finally:
        BLOQUEO_CAMARA.release()


def verificar(user_id):
    threshold = facial_threshold()
    obtener_clave()
    with reservar_camara():
        repository.registrar_evento(user_id, "VERIFICACION", "INICIADO", "Verificación solicitada")
        try:
            version = repository.version_perfil(user_id)
            embedding = obtener_embedding_facial(user_id)
            if embedding is None:
                raise repository.RechazoFacial(409, "No se pudo recuperar el perfil facial")
            validar_embedding(embedding)
            # La instantánea también debe corresponder al embedding leído antes de abrir cámara.
            if repository.version_perfil(user_id) != version:
                raise repository.RechazoFacial(409, "El perfil facial cambió; repita la verificación")
            result = ejecutar_captura("verify", embedding)
            if result.get("cancelled"):
                raise repository.RechazoFacial(409, "Captura facial cancelada")
            if repository.version_perfil(user_id) != version:
                raise repository.RechazoFacial(409, "El perfil facial cambió durante la captura")
            score = result.get("score")
            live = result.get("livenessPassed") is True
            live_score = result.get("livenessScore")
            if (type(score) not in (int, float) or not math.isfinite(score) or not -1 <= score <= 1
                    or type(live_score) not in (int, float) or not math.isfinite(live_score)
                    or not 0 <= live_score <= 1):
                raise ValueError("Resultado biométrico inválido")
            live = live and live_score >= liveness_threshold()
            matched = live and score >= threshold
            repository.registrar_evento(user_id, "LIVENESS", "EXITO" if live else "FALLO",
                                        "Evaluación de presencia real", live_score, liveness_threshold())
            repository.registrar_evento(user_id, "VERIFICACION", "EXITO" if matched else "FALLO",
                                        "Liveness superado" if live else "Liveness rechazado", score, threshold)
            return {"userId": user_id, "matched": matched, "score": score, "threshold": threshold,
                    "livenessPassed": live, "livenessScore": live_score}
        except Exception as error:
            causa = error.code if isinstance(error, ErrorCaptura) else "Captura, calidad, liveness o estado no válidos"
            repository.registrar_evento(user_id, "VERIFICACION", "FALLO", causa)
            raise


def registrar(user_id, admin_id, replace):
    repository.comprobar_registro(user_id, admin_id, replace)
    obtener_clave()  # Fallar antes de abrir cámara si falta la clave persistente.
    with reservar_camara():
        repository.registrar_evento(user_id, "ENROLL", "INICIADO", f"Actor ADMIN id={admin_id}")
        try:
            perfil = ejecutar_captura("enroll")
            if perfil.get("cancelled"):
                raise repository.RechazoFacial(409, "Registro facial cancelado")
            validar_embedding(perfil["embedding"])
            cantidad = perfil.get("cantidad_muestras")
            calidad = perfil.get("confianza_deteccion_media")
            live = perfil.get("liveness_promedio")
            if (type(cantidad) is not int or cantidad != 6
                    or type(calidad) not in (int, float) or not math.isfinite(calidad) or not .70 <= calidad <= 1
                    or type(live) not in (int, float) or not math.isfinite(live)
                    or not liveness_threshold() <= live <= 1):
                raise ValueError("Perfil preliminar no válido")
            # La frontera SQL revalida actor, objetivo y replace bajo bloqueo transaccional.
            repository.guardar_registro(user_id, admin_id, replace, perfil)
            return {"userId": user_id, "registered": True}
        except Exception as error:
            causa = error.code if isinstance(error, ErrorCaptura) else "Registro denegado"
            repository.registrar_evento(user_id, "ENROLL", "FALLO", f"{causa}; actor ADMIN id={admin_id}")
            raise


def eliminar(user_id, admin_id):
    repository.comprobar_administrador(admin_id)
    repository.registrar_evento(user_id, "ELIMINACION", "INICIADO", f"Actor ADMIN id={admin_id}")
    try:
        return repository.eliminar_registro(user_id, admin_id)
    except Exception:
        repository.registrar_evento(user_id, "ELIMINACION", "FALLO", f"Operación denegada; actor ADMIN id={admin_id}")
        raise

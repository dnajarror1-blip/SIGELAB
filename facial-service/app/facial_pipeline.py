"""
SIGELAB FaceID
Analisis combinado de presencia real y reconocimiento facial.

Este modulo:
- Detecta exactamente un rostro.
- Comprueba presencia real mediante liveness.
- Compara el rostro con el perfil registrado si pasa liveness.
- Reutiliza el analizador facial durante la vida del proceso.
- Evita analisis simultaneos sobre el mismo analizador.
- No concede acceso al sistema.
- No guarda fotografias ni embeddings.
"""

import math
from functools import lru_cache
from pathlib import Path
from threading import Lock

import numpy as np
def FaceAnalysis(*args, **kwargs):
    from insightface.app import FaceAnalysis as factory
    return factory(*args, **kwargs)


from .config import liveness_threshold, model_name
from .facial_matching import calcular_similitud
from .perfil_storage import validar_embedding


CARPETA_SERVICIO = Path(__file__).resolve().parents[1]

# Umbral inicial pendiente de calibracion.
UMBRAL_LIVENESS_INICIAL = liveness_threshold()

# Protege la carga y el uso del analizador compartido.
_BLOQUEO_ANALISIS = Lock()


@lru_cache(maxsize=1)
def _obtener_analizador():
    """
    Crea y prepara el analizador facial una sola vez
    por proceso, siempre que la inicializacion
    termine correctamente.

    Las llamadas posteriores reutilizan el modelo.

    Esta funcion se utiliza bajo _BLOQUEO_ANALISIS.
    """

    print("Cargando reconocimiento facial y liveness...")

    analizador = FaceAnalysis(
        name=model_name(),
        root=str(CARPETA_SERVICIO),
        providers=["CPUExecutionProvider"],
        allowed_modules=[
            "detection",
            "recognition"
        ],
        addons=["liveness"],
        liveness_mode="normal",
        liveness_threshold=UMBRAL_LIVENESS_INICIAL
    )

    analizador.prepare(
        ctx_id=-1,
        det_size=(320, 320)
    )

    print("Modelos preparados y disponibles para reutilizacion.")

    return analizador


def analizar_captura_facial(imagen, embedding_registrado, *, exigir_calidad=False):
    """
    Analiza una imagen y devuelve resultados diagnosticos.

    Primero verifica presencia real.
    Solo compara identidad si supera esa comprobacion.

    No autoriza el inicio de sesion.
    No almacena datos biometricos.
    """

    if imagen is None:
        raise ValueError(
            "No se recibio una imagen"
        )

    validar_embedding(embedding_registrado)

    # Se serializa el acceso al analizador compartido.
    with _BLOQUEO_ANALISIS:

        analizador = _obtener_analizador()

        print("Analizando captura...")

        rostros = analizador.get(
            imagen,
            max_num=0
        )

        if len(rostros) != 1:
            raise ValueError(
                f"Se esperaba exactamente un rostro; "
                f"se detectaron {len(rostros)}"
            )

        rostro = rostros[0]

        evaluacion = rostro.liveness

        if evaluacion is None:
            raise RuntimeError(
                "El complemento de liveness no genero resultado"
            )

        estado = evaluacion.status

        # Imagen inadecuada para evaluar presencia real.
        if estado == "input_rejected":
            return {
                "livenessStatus": "input_rejected",
                "livenessPassed": None,
                "livenessScore": None,
                "similarity": None
            }

        if (
            estado != "ok"
            or evaluacion.live_score is None
        ):
            raise RuntimeError(
                "Respuesta de liveness no valida"
            )

        puntuacion = float(
            evaluacion.live_score
        )

        if (
            not math.isfinite(puntuacion)
            or not (0.0 <= puntuacion <= 1.0)
        ):
            raise RuntimeError(
                "Puntuacion de liveness fuera de rango"
            )

        # Posible suplantacion:
        # no continuar con reconocimiento de identidad.
        if evaluacion.is_live is not True:
            return {
                "livenessStatus": "ok",
                "livenessPassed": False,
                "livenessScore": round(puntuacion, 4),
                "similarity": None
            }

        if exigir_calidad:
            # Misma calidad mínima de captura; sin cargar otro motor ni modificar enrollment.
            import cv2
            confianza = float(rostro.det_score)
            if not math.isfinite(confianza) or not .70 <= confianza <= 1:
                raise ValueError("Deteccion facial poco confiable")
            alto, ancho = imagen.shape[:2]
            bbox = np.asarray(rostro.bbox, dtype=float)
            if bbox.shape != (4,) or not np.all(np.isfinite(bbox)):
                raise ValueError("Posicion facial no valida")
            x1, y1, x2, y2 = bbox.astype(int)
            x1, y1 = max(0, x1), max(0, y1)
            x2, y2 = min(ancho, x2), min(alto, y2)
            if min(x2 - x1, y2 - y1) < 90:
                raise ValueError("Acercate un poco a la camara")
            gris = cv2.cvtColor(imagen[y1:y2, x1:x2], cv2.COLOR_BGR2GRAY)
            brillo = float(np.mean(gris))
            nitidez = float(cv2.Laplacian(gris, cv2.CV_64F).var())
            if not math.isfinite(brillo) or not 45 <= brillo <= 220:
                raise ValueError("Ajusta la iluminacion")
            if not math.isfinite(nitidez) or nitidez < 30:
                raise ValueError("La imagen esta borrosa")

        # La captura supero la comprobacion
        # de presencia real.
        if rostro.embedding is None:
            raise RuntimeError(
                "No se obtuvo el embedding facial"
            )

        embedding_actual = np.asarray(
            rostro.embedding,
            dtype="<f4"
        ).reshape(-1).tobytes()

        validar_embedding(
            embedding_actual
        )

        similitud = float(
            calcular_similitud(
                embedding_registrado,
                embedding_actual
            )
        )

        if not math.isfinite(similitud):
            raise RuntimeError(
                "Resultado de similitud no valido"
            )

        return {
            "livenessStatus": "ok",
            "livenessPassed": True,
            "livenessScore": round(puntuacion, 4),
            "similarity": round(similitud, 4)
        }
"""
SIGELAB FaceID
Analisis combinado de presencia real y reconocimiento facial.

Este modulo:
- Detecta exactamente un rostro.
- Comprueba presencia real mediante liveness.
- Compara el rostro con el perfil registrado si pasa liveness.
- No concede acceso al sistema.
- No guarda fotografias ni embeddings.
"""

import math
from pathlib import Path

import numpy as np
from insightface.app import FaceAnalysis

from .facial_matching import calcular_similitud
from .perfil_storage import validar_embedding


CARPETA_SERVICIO = Path(__file__).resolve().parents[1]

# Umbral inicial pendiente de calibracion.
UMBRAL_LIVENESS_INICIAL = 0.80


def analizar_captura_facial(imagen, embedding_registrado):
    """
    Analiza una imagen y devuelve resultados diagnosticos.

    Primero verifica la presencia real.
    Solo compara identidad si supera esa comprobacion.
    """

    if imagen is None:
        raise ValueError("No se recibio una imagen")

    validar_embedding(embedding_registrado)

    print("Cargando reconocimiento facial y liveness...")

    analizador = FaceAnalysis(
        name="buffalo_l",
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

    print("Modelos preparados.")
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

    if estado != "ok" or evaluacion.live_score is None:
        raise RuntimeError(
            "Respuesta de liveness no valida"
        )

    puntuacion = float(evaluacion.live_score)

    if (
        not math.isfinite(puntuacion)
        or not (0.0 <= puntuacion <= 1.0)
    ):
        raise RuntimeError(
            "Puntuacion de liveness fuera de rango"
        )

    # Posible suplantacion: no continuar con identidad.
    if evaluacion.is_live is not True:
        return {
            "livenessStatus": "ok",
            "livenessPassed": False,
            "livenessScore": round(puntuacion, 4),
            "similarity": None
        }

    # La captura supero la comprobacion de presencia real.
    if rostro.embedding is None:
        raise RuntimeError(
            "No se obtuvo el embedding facial"
        )

    embedding_actual = np.asarray(
        rostro.embedding,
        dtype="<f4"
    ).reshape(-1).tobytes()

    validar_embedding(embedding_actual)

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
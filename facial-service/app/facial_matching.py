
import math

import numpy as np

from .perfil_storage import validar_embedding


def calcular_similitud(embedding_a, embedding_b):
    """
    Calcula la similitud del coseno entre dos
    embeddings faciales de 512 dimensiones.

    Devuelve un valor entre -1 y 1.
    """

    validar_embedding(embedding_a)
    validar_embedding(embedding_b)

    vector_a = np.frombuffer(
        embedding_a,
        dtype="<f4"
    ).astype(np.float64)

    vector_b = np.frombuffer(
        embedding_b,
        dtype="<f4"
    ).astype(np.float64)

    norma_a = np.linalg.norm(vector_a)
    norma_b = np.linalg.norm(vector_b)

    denominador = float(norma_a * norma_b)

    if (
        not math.isfinite(denominador)
        or denominador <= 0
    ):
        raise ValueError(
            "No es posible comparar embeddings invalidos"
        )

    similitud = float(
        np.dot(vector_a, vector_b) / denominador
    )

    if not math.isfinite(similitud):
        raise ValueError(
            "La similitud calculada no es valida"
        )

    # Corregir pequenas diferencias de precision numerica.
    return max(-1.0, min(1.0, similitud))

"""
SIGELAB FaceID - Preparacion de registro facial.

Este modulo:
- Captura seis muestras.
- Comprueba presencia real.
- Valida la calidad de las capturas.
- Genera un embedding promedio.
- No guarda datos en SQL Server.
- No autoriza el registro de usuarios.
"""

import math
from pathlib import Path
from time import perf_counter

import cv2
import numpy as np
def FaceAnalysis(*args, **kwargs):
    from insightface.app import FaceAnalysis as factory
    return factory(*args, **kwargs)


from .camara import abrir_camara, ErrorCamara
from .config import capture_timeout, model_name
from .facial_matching import calcular_similitud
from .facial_pipeline import UMBRAL_LIVENESS_INICIAL
from .perfil_storage import validar_embedding


CARPETA_SERVICIO = Path(__file__).resolve().parents[1]

MUESTRAS_REQUERIDAS = 6
MAX_TIEMPO_CAPTURA_SEG = capture_timeout()

POSICIONES = (
    "Mira de frente",
    "Gira un poco a tu izquierda",
    "Gira un poco a tu derecha",
    "Inclina ligeramente hacia arriba",
    "Inclina ligeramente hacia abajo",
    "Vuelve a mirar de frente",
)


def cargar_modelo_registro():
    """
    Prepara reconocimiento facial con liveness.
    """

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

    return analizador


def analizar_muestra_registro(
    analizador,
    imagen,
    muestras_anteriores
):
    """
    Verifica una muestra facial.

    Devuelve:
    - Embedding
    - Confianza de deteccion
    - Puntuacion de liveness
    """

    if imagen is None:
        raise ValueError(
            "No se recibio una imagen"
        )

    rostros = analizador.get(
        imagen,
        max_num=0
    )

    if len(rostros) != 1:
        raise ValueError(
            f"Se detectaron {len(rostros)} rostros; "
            "se necesita exactamente uno"
        )

    rostro = rostros[0]

    # Comprobar presencia real.
    liveness = rostro.liveness

    if liveness is None:
        raise RuntimeError(
            "No se ejecuto el control de liveness"
        )

    if liveness.status == "input_rejected":
        raise ValueError(
            "Centra tu rostro y alejate un poco de los bordes"
        )

    if (
        liveness.status != "ok"
        or liveness.is_live is not True
    ):
        raise ValueError(
            "La captura no supero la prueba de presencia real"
        )

    score_liveness = float(
        liveness.live_score
    )

    if (
        not math.isfinite(score_liveness)
        or not 0.0 <= score_liveness <= 1.0
    ):
        raise RuntimeError(
            "Puntuacion de liveness invalida"
        )

    # Comprobar confianza de deteccion.
    confianza = float(
        rostro.det_score
    )

    if (
        not math.isfinite(confianza)
        or confianza < 0.70
    ):
        raise ValueError(
            "Deteccion facial poco confiable"
        )

    # Comprobar dimensiones del rostro.
    alto, ancho = imagen.shape[:2]

    x1, y1, x2, y2 = np.asarray(
        rostro.bbox,
        dtype=int
    )

    x1 = max(0, x1)
    y1 = max(0, y1)
    x2 = min(ancho, x2)
    y2 = min(alto, y2)

    if min(x2 - x1, y2 - y1) < 90:
        raise ValueError(
            "Acercate un poco a la camara"
        )

    region = imagen[y1:y2, x1:x2]

    gris = cv2.cvtColor(
        region,
        cv2.COLOR_BGR2GRAY
    )

    brillo = float(
        np.mean(gris)
    )

    desenfoque = float(
        cv2.Laplacian(
            gris,
            cv2.CV_64F
        ).var()
    )

    if not 45 <= brillo <= 220:
        raise ValueError(
            "Ajusta la iluminacion"
        )

    if desenfoque < 30:
        raise ValueError(
            "La imagen esta borrosa. "
            "Manten la camara estable."
        )

    # Extraer embedding.
    if rostro.embedding is None:
        raise ValueError(
            "No se obtuvo el embedding facial"
        )

    datos = np.asarray(
        rostro.embedding,
        dtype="<f4"
    ).reshape(-1).tobytes()

    validar_embedding(datos)

    # Comparar con primera muestra.
    if muestras_anteriores:

        similitud = calcular_similitud(
            muestras_anteriores[0],
            datos
        )

        if similitud < 0.45:
            raise ValueError(
                "Muestra inconsistente con "
                "la primera captura"
            )

    return (
        datos,
        confianza,
        score_liveness
    )


def promedio_embeddings(muestras):
    """
    Genera un embedding promedio normalizado.
    """

    if len(muestras) != MUESTRAS_REQUERIDAS:
        raise ValueError(
            "Cantidad de muestras incompleta"
        )

    for datos in muestras:
        validar_embedding(datos)

    vectores = np.stack(
        [
            np.frombuffer(
                datos,
                dtype="<f4"
            ).astype(np.float64)
            for datos in muestras
        ]
    )

    normas = np.linalg.norm(
        vectores,
        axis=1,
        keepdims=True
    )

    if np.any(normas < 1e-8):
        raise ValueError(
            "Se recibio una muestra facial inestable"
        )

    promedio = np.mean(
        vectores / normas,
        axis=0
    )

    norma = float(
        np.linalg.norm(promedio)
    )

    if (
        not math.isfinite(norma)
        or norma < 1e-8
    ):
        raise ValueError(
            "No se pudo generar un perfil facial estable"
        )

    datos = (
        promedio / norma
    ).astype("<f4").tobytes()

    validar_embedding(datos)

    return datos


def capturar_muestras_registro(analizador):
    """
    Captura seis muestras en memoria.

    Q o ESC cancela sin guardar datos.
    """

    camara = abrir_camara()

    if not camara.isOpened():
        camara.release()

        raise RuntimeError(
            "No se pudo abrir la camara 0"
        )

    camara.set(
        cv2.CAP_PROP_FRAME_WIDTH,
        640
    )

    camara.set(
        cv2.CAP_PROP_FRAME_HEIGHT,
        480
    )

    muestras = []
    confianzas = []
    puntuaciones_liveness = []

    mensaje = "ESPACIO: capturar / Q o ESC: cancelar"

    inicio = perf_counter()

    try:
        while len(muestras) < MUESTRAS_REQUERIDAS:

            if (
                perf_counter() - inicio
                > MAX_TIEMPO_CAPTURA_SEG
            ):
                raise TimeoutError(
                    "Tiempo agotado para el registro facial"
                )

            correcto, imagen = camara.read()

            if not correcto:
                raise ErrorCamara(
                    "La camara dejo de entregar imagenes"
                )

            numero = len(muestras)

            vista = imagen.copy()

            cv2.putText(
                vista,
                f"SIGELAB FaceID: {numero}/{MUESTRAS_REQUERIDAS}",
                (15, 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.6,
                (255, 255, 255),
                2
            )

            cv2.putText(
                vista,
                POSICIONES[numero],
                (15, 65),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.52,
                (255, 255, 255),
                2
            )

            cv2.putText(
                vista,
                mensaje[:60],
                (15, 100),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.43,
                (255, 255, 255),
                1
            )

            cv2.imshow(
                "SIGELAB - Preparacion facial",
                vista
            )

            tecla = cv2.waitKey(1) & 0xFF
            if cv2.getWindowProperty("SIGELAB - Preparacion facial", cv2.WND_PROP_VISIBLE) < 1:
                return None

            if tecla in (
                ord("q"),
                ord("Q"),
                27
            ):
                print(
                    "Registro cancelado; "
                    "no se guardaron datos"
                )

                return None

            if tecla != 32:
                continue

            try:
                datos, confianza, score = (
                    analizar_muestra_registro(
                        analizador,
                        imagen,
                        muestras
                    )
                )

            except ValueError as error:
                mensaje = str(error)

                print(
                    "Muestra rechazada:",
                    error
                )

                continue

            muestras.append(datos)
            confianzas.append(confianza)
            puntuaciones_liveness.append(score)

            mensaje = (
                "Muestra aceptada; "
                "cambia suavemente de posicion"
            )

            print(
                f"Muestra {len(muestras)}/"
                f"{MUESTRAS_REQUERIDAS}: OK"
            )

    finally:
        camara.release()
        cv2.destroyAllWindows()

    return (
        muestras,
        confianzas,
        puntuaciones_liveness
    )


def preparar_perfil_preliminar():
    """
    Prepara un perfil exclusivamente en memoria.

    No realiza operaciones SQL.
    """

    analizador = cargar_modelo_registro()

    resultado = capturar_muestras_registro(
        analizador
    )

    if resultado is None:
        return None

    muestras, confianzas, puntuaciones_liveness = resultado

    return {
        "embedding": promedio_embeddings(muestras),
        "cantidad_muestras": len(muestras),
        "confianza_deteccion_media": round(
            float(np.mean(confianzas)),
            4
        ),
        "liveness_promedio": round(
            float(np.mean(puntuaciones_liveness)),
            4
        )
    }
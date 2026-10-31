"""Captura automática de verificación. No registra perfiles ni decide identidad/acceso."""
from concurrent.futures import ThreadPoolExecutor
import math
from statistics import median, mean
from time import perf_counter

import cv2

from .camara import abrir_camara, ErrorCamara
from .config import capture_timeout, liveness_threshold
from .facial_pipeline import analizar_captura_facial
from .perfil_storage import validar_embedding

VENTANA = "SIGELAB - Verificacion facial"
MUESTRAS = 3
INTERVALO = 0.6
MAX_RECHAZOS_LIVENESS = 3


def _preview(imagen, mensaje):
    vista = imagen.copy()  # Solo en memoria; nunca se altera el frame analizado.
    for i, texto in enumerate(("Mantenga el rostro frente a la camara", mensaje, "ESC / Q: cancelar")):
        cv2.putText(vista, texto, (15, 30 + i * 30), cv2.FONT_HERSHEY_SIMPLEX,
                    .55, (255, 255, 255), 1)
    try:
        if cv2.getWindowProperty(VENTANA, cv2.WND_PROP_VISIBLE) < 1:
            return False
    except cv2.error:
        return False
    cv2.imshow(VENTANA, vista)
    tecla = cv2.waitKey(1) & 0xFF
    try:
        visible = cv2.getWindowProperty(VENTANA, cv2.WND_PROP_VISIBLE) >= 1
    except cv2.error:
        visible = False
    return visible and tecla not in (27, ord('q'), ord('Q'))


def capturar_verificacion(embedding_registrado):
    validar_embedding(embedding_registrado)  # Antes de cámara o carga de modelo.
    timeout = capture_timeout()
    minimo_live = liveness_threshold()
    camara = abrir_camara()
    analisis = None
    puntuaciones, presencias, rechazos = [], [], []
    mensaje = "Buscando rostro..."
    pendiente = None
    proxima = inicio = None
    resultado = None
    mostrar_hasta = None
    try:
        analisis = ThreadPoolExecutor(max_workers=1)
        cv2.namedWindow(VENTANA, cv2.WINDOW_AUTOSIZE)
        proxima = inicio = perf_counter()
        while True:
            ahora = perf_counter()
            if ahora - inicio >= timeout:
                raise TimeoutError("Tiempo agotado para la verificacion facial")
            correcto, imagen = camara.read()
            if not correcto or imagen is None:
                raise ErrorCamara("La camara dejo de entregar imagenes")
            if not _preview(imagen, mensaje):
                return {"cancelled": True}
            if resultado is not None:
                # Dejar visible 3/3 o el rechazo de presencia, sin decidir MATCH en la UI.
                if ahora >= mostrar_hasta:
                    return resultado
                continue
            if pendiente is not None and pendiente.done():
                try:
                    muestra = pendiente.result()
                except ValueError:
                    # Cero/varios rostros, posición o calidad corregibles.
                    mensaje = "Mantenga el rostro frente a la camara"
                else:
                    if muestra.get('livenessStatus') == 'input_rejected':
                        mensaje = "Mantenga el rostro frente a la camara"
                    elif muestra.get('livenessStatus') != 'ok':
                        raise RuntimeError("Respuesta de liveness no valida")
                    else:
                        live = muestra.get('livenessScore')
                        if type(live) not in (int, float) or not math.isfinite(live) or not 0 <= live <= 1:
                            raise RuntimeError("Puntuacion de liveness no valida")
                        if muestra.get('livenessPassed') is not True or live < minimo_live:
                            rechazos.append(live)
                            puntuaciones.clear()
                            presencias.clear()
                            mensaje = "Presencia real no confirmada"
                            if len(rechazos) >= MAX_RECHAZOS_LIVENESS:
                                resultado = {"cancelled": False, "livenessPassed": False,
                                             "livenessScore": mean(rechazos), "score": -1.0}
                        else:
                            score = muestra.get('similarity')
                            if type(score) not in (int, float) or not math.isfinite(score) or not -1 <= score <= 1:
                                raise RuntimeError("Similitud no valida")
                            puntuaciones.append(score)
                            presencias.append(live)
                            mensaje = f"Muestra automatica {len(puntuaciones)}/{MUESTRAS}"
                            if len(puntuaciones) == MUESTRAS:
                                resultado = {"cancelled": False, "livenessPassed": True,
                                             "livenessScore": mean(presencias), "score": median(puntuaciones)}
                    if resultado is not None:
                        mostrar_hasta = perf_counter() + INTERVALO
                pendiente = None
                proxima = perf_counter() + INTERVALO
            if pendiente is None and resultado is None and ahora >= proxima:
                if not puntuaciones and not rechazos:
                    mensaje = "Verificando identidad..."
                # ONNX en un hilo; preview/cancelación OpenCV siguen en MainThread.
                pendiente = analisis.submit(analizar_captura_facial, imagen.copy(),
                                             embedding_registrado, exigir_calidad=True)
    finally:
        camara.release()
        cv2.destroyAllWindows()
        # Un análisis en curso no accede a cámara/GUI ni persiste datos. El proceso
        # padre conserva además su timeout duro si el motor nativo no responde.
        if analisis is not None:
            analisis.shutdown(wait=False, cancel_futures=True)

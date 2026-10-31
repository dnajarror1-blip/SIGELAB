
import time
from pathlib import Path

import cv2
import numpy as np
from insightface.app import FaceAnalysis


CARPETA_SERVICIO = Path(__file__).resolve().parent.parent

TITULO = "SIGELAB FaceID - Diagnostico facial"

MODELO = "buffalo_l"


def capturar_rostro():
    camara = cv2.VideoCapture(0, cv2.CAP_DSHOW)

    if not camara.isOpened():
        raise RuntimeError("No se pudo abrir la camara")

    camara.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    camara.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    imagen_capturada = None

    try:
        cv2.namedWindow(TITULO, cv2.WINDOW_NORMAL)

        print("Colocate frente a la camara.")
        print("Presiona ESPACIO para analizar tu rostro.")
        print("Presiona Q o ESC para cancelar.")

        while True:
            correcto, imagen = camara.read()

            if not correcto:
                raise RuntimeError(
                    "No se pudo obtener una imagen de la camara"
                )

            imagen = cv2.flip(imagen, 1)

            # La imagen original no se modifica.
            vista_previa = imagen.copy()

            cv2.putText(
                vista_previa,
                "SIGELAB - Diagnostico facial",
                (15, 35),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.65,
                (255, 255, 255),
                2
            )

            cv2.putText(
                vista_previa,
                "ESPACIO: Analizar rostro",
                (15, 440),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.6,
                (0, 255, 0),
                2
            )

            cv2.imshow(TITULO, vista_previa)

            tecla = cv2.waitKey(1) & 0xFF

            if tecla == 32:
                imagen_capturada = imagen.copy()
                print("Imagen recibida para analisis.")
                break

            if tecla in (ord("q"), ord("Q"), 27):
                print("Analisis cancelado.")
                break

            if cv2.getWindowProperty(
                TITULO,
                cv2.WND_PROP_VISIBLE
            ) < 1:
                break

    finally:
        camara.release()
        cv2.destroyAllWindows()

    return imagen_capturada


def analizar_rostro(imagen):
    print("")
    print("Cargando modelo InsightFace...")

    inicio_carga = time.perf_counter()

    analizador = FaceAnalysis(
        name=MODELO,
        root=str(CARPETA_SERVICIO),
        providers=["CPUExecutionProvider"],
        allowed_modules=[
            "detection",
            "recognition"
        ]
    )

    analizador.prepare(
        ctx_id=-1,
        det_size=(320, 320)
    )

    print(
        f"Modelo preparado en "
        f"{time.perf_counter() - inicio_carga:.2f} segundos"
    )

    print("Analizando rostro...")

    inicio_analisis = time.perf_counter()

    rostros = analizador.get(
        imagen,
        max_num=1
    )

    duracion = time.perf_counter() - inicio_analisis

    print(f"Tiempo de analisis: {duracion:.2f} segundos")
    print(f"Rostros detectados: {len(rostros)}")

    if not rostros:
        print("No se detecto ningun rostro.")
        print("Prueba nuevamente con mejor iluminacion.")
        return

    rostro = rostros[0]

    if rostro.embedding is None:
        print("Se detecto el rostro, pero no hay embedding.")
        return

    embedding = np.asarray(
        rostro.embedding,
        dtype=np.float32
    ).reshape(-1)

    dimensiones = embedding.size
    datos = embedding.astype("<f4").tobytes()

    valido = (
        dimensiones == 512
        and np.all(np.isfinite(embedding))
        and np.any(embedding != 0)
    )

    print("")
    print("RESULTADOS DEL RECONOCIMIENTO")
    print("Rostro detectado: SI")
    print(f"Dimensiones del embedding: {dimensiones}")
    print(f"Tamano en bytes: {len(datos)}")
    print(f"Embedding valido: {bool(valido)}")
    print(f"Confianza de deteccion: {float(rostro.det_score):.4f}")

    if valido:
        print("")
        print("DIAGNOSTICO FACIAL COMPLETADO CORRECTAMENTE")

    print("")
    print("No se guardaron imagenes ni embeddings.")


def main():
    imagen = capturar_rostro()

    if imagen is None:
        return

    analizar_rostro(imagen)


if __name__ == "__main__":
    main()

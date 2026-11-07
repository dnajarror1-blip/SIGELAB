
import time
from pathlib import Path

import cv2
import numpy as np
from insightface.app import FaceAnalysis

from app.facial_matching import calcular_similitud
from app.perfil_storage import validar_embedding


CARPETA_SERVICIO = Path(__file__).resolve().parent.parent
TITULO = "SIGELAB FaceID - Comparacion facial"


def capturar_dos_imagenes():
    camara = cv2.VideoCapture(0, cv2.CAP_DSHOW)

    if not camara.isOpened():
        raise RuntimeError("No se pudo abrir la camara")

    camara.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    camara.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    capturas = []
    ultima_captura = 0.0

    try:
        cv2.namedWindow(TITULO, cv2.WINDOW_NORMAL)

        print("SIGELAB - Prueba de similitud facial")
        print("Presiona ESPACIO para realizar cada captura.")
        print("Presiona Q o ESC para cancelar.")

        while len(capturas) < 2:
            correcto, imagen = camara.read()

            if not correcto:
                raise RuntimeError(
                    "No fue posible obtener imagen de la camara"
                )

            imagen = cv2.flip(imagen, 1)
            vista = imagen.copy()

            numero_captura = len(capturas) + 1

            cv2.putText(
                vista,
                "SIGELAB FaceID",
                (15, 35),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                (255, 255, 255),
                2
            )

            cv2.putText(
                vista,
                f"Captura {numero_captura} de 2",
                (15, 70),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.65,
                (0, 255, 0),
                2
            )

            cv2.putText(
                vista,
                "ESPACIO: Capturar",
                (15, 440),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.6,
                (255, 255, 255),
                2
            )

            cv2.imshow(TITULO, vista)

            tecla = cv2.waitKey(1) & 0xFF

            if tecla == 32:
                ahora = time.monotonic()

                if ahora - ultima_captura >= 1.5:
                    capturas.append(imagen.copy())
                    ultima_captura = ahora

                    print(
                        f"Captura {len(capturas)} recibida correctamente"
                    )

                    if len(capturas) == 1:
                        print(
                            "Mueve ligeramente tu cabeza "
                            "antes de la segunda captura."
                        )

            elif tecla in (ord("q"), ord("Q"), 27):
                print("Prueba cancelada.")
                break

            if cv2.getWindowProperty(
                TITULO,
                cv2.WND_PROP_VISIBLE
            ) < 1:
                break

    finally:
        camara.release()
        cv2.destroyAllWindows()

    return capturas


def obtener_embedding(analizador, imagen):
    rostros = analizador.get(imagen, max_num=0)

    if len(rostros) != 1:
        raise ValueError(
            f"Se esperaba exactamente un rostro; "
            f"se detectaron {len(rostros)}"
        )

    rostro = rostros[0]

    if rostro.embedding is None:
        raise ValueError(
            "InsightFace no genero un embedding"
        )

    embedding = np.asarray(
        rostro.embedding,
        dtype=np.float32
    ).reshape(-1)

    datos = embedding.astype("<f4").tobytes()

    validar_embedding(datos)

    return datos


def comparar_capturas(capturas):
    print("")
    print("Cargando modelo buffalo_l...")

    analizador = FaceAnalysis(
        name="buffalo_l",
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

    print("Modelo preparado correctamente.")
    print("Analizando primera captura...")

    embedding_1 = obtener_embedding(
        analizador,
        capturas[0]
    )

    print("Analizando segunda captura...")

    embedding_2 = obtener_embedding(
        analizador,
        capturas[1]
    )

    similitud = calcular_similitud(
        embedding_1,
        embedding_2
    )

    print("")
    print("RESULTADO DE COMPARACION FACIAL")
    print("Primera captura: 512 dimensiones")
    print("Segunda captura: 512 dimensiones")
    print(f"Similitud del coseno: {similitud:.4f}")
    print("")
    print("Comparacion realizada correctamente.")
    print("No se guardaron imagenes ni embeddings.")


def main():
    capturas = capturar_dos_imagenes()

    if len(capturas) != 2:
        print("Se necesitan dos capturas para comparar.")
        return

    comparar_capturas(capturas)


if __name__ == "__main__":
    main()


import time
import cv2


INDICE_CAMARA = 0
ANCHO = 640
ALTO = 480
TITULO = "SIGELAB FaceID - Vista previa"


def iniciar_preview():
    print("Iniciando camara SIGELAB FaceID...")

    camara = cv2.VideoCapture(
        INDICE_CAMARA,
        cv2.CAP_DSHOW
    )

    if not camara.isOpened():
        print("ERROR: No se pudo abrir la camara.")
        return

    camara.set(cv2.CAP_PROP_FRAME_WIDTH, ANCHO)
    camara.set(cv2.CAP_PROP_FRAME_HEIGHT, ALTO)

    fps_promedio = 0.0
    tiempo_anterior = time.perf_counter()

    try:
        cv2.namedWindow(
            TITULO,
            cv2.WINDOW_NORMAL
        )

        cv2.resizeWindow(TITULO, 720, 540)

        print("Camara abierta correctamente.")
        print("Presiona Q o ESC para salir.")
        print("No se guardaran imagenes ni videos.")

        while True:
            correcto, imagen = camara.read()

            if not correcto:
                print("ERROR: No se pudo recibir una imagen.")
                break

            # Vista de espejo para mayor comodidad.
            imagen = cv2.flip(imagen, 1)

            # Calcular FPS de forma suavizada.
            tiempo_actual = time.perf_counter()
            diferencia = tiempo_actual - tiempo_anterior
            tiempo_anterior = tiempo_actual

            if diferencia > 0:
                fps_actual = 1.0 / diferencia

                if fps_promedio == 0:
                    fps_promedio = fps_actual
                else:
                    fps_promedio = (
                        0.85 * fps_promedio
                        + 0.15 * fps_actual
                    )

            cv2.putText(
                imagen,
                "SIGELAB - FaceID",
                (15, 35),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                (255, 255, 255),
                2
            )

            cv2.putText(
                imagen,
                f"FPS: {fps_promedio:.1f}",
                (15, 70),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.65,
                (0, 255, 0),
                2
            )

            cv2.putText(
                imagen,
                "Vista previa - Sin almacenamiento",
                (15, ALTO - 45),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.5,
                (255, 255, 255),
                1
            )

            cv2.putText(
                imagen,
                "Q / ESC: Salir",
                (15, ALTO - 20),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.5,
                (255, 255, 255),
                1
            )

            cv2.imshow(TITULO, imagen)

            tecla = cv2.waitKey(1) & 0xFF

            if tecla in (ord("q"), ord("Q"), 27):
                print("Cerrando vista previa...")
                break

            if cv2.getWindowProperty(
                TITULO,
                cv2.WND_PROP_VISIBLE
            ) < 1:
                break

    finally:
        camara.release()
        cv2.destroyAllWindows()

        print("Camara liberada correctamente.")


if __name__ == "__main__":
    iniciar_preview()

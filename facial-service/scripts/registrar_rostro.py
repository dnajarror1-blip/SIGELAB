from pathlib import Path

import cv2
import numpy as np
from insightface.app import FaceAnalysis

from app.database import get_connection
from app.facial_matching import calcular_similitud
from app.facial_repository import consultar_estado_facial
from app.perfil_storage import validar_embedding
from app.registro_facial_nuevo import guardar_perfil_nuevo

BASE_PRUEBAS = "SIGELAB_TEST_FACE"
USERNAME = "faceid.integrante08"
MUESTRAS_REQUERIDAS = 6

CARPETA_SERVICIO = Path(__file__).resolve().parents[1]

POSICIONES = (
    "Mira de frente",
    "Gira un poco a tu izquierda",
    "Gira un poco a tu derecha",
    "Inclina ligeramente hacia arriba",
    "Inclina ligeramente hacia abajo",
    "Vuelve a mirar de frente",
)


def verificar_usuario():
    conexion = get_connection()

    try:
        cursor = conexion.cursor()

        base = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()[0]

        if base != BASE_PRUEBAS:
            raise RuntimeError(
                "Registro bloqueado: base de datos incorrecta"
            )

        usuario = cursor.execute(
            """
            SELECT id_usuario, estado
            FROM dbo.Usuario
            WHERE username = ?
            """,
            USERNAME,
        ).fetchone()

        if usuario is None:
            raise RuntimeError(
                "No se encontro el usuario FaceID"
            )

        if str(usuario[1]).strip().upper() != "ACTIVO":
            raise RuntimeError(
                "El usuario FaceID no esta ACTIVO"
            )

        id_usuario = int(usuario[0])

        existente = cursor.execute(
            """
            SELECT 1
            FROM dbo.PerfilFacial
            WHERE id_usuario = ?
            """,
            id_usuario,
        ).fetchone()

        if existente:
            raise RuntimeError(
                "El usuario ya tiene un perfil facial. "
                "No se sobrescribira."
            )

        return id_usuario

    finally:
        conexion.close()


def cargar_modelo():
    print("Cargando modelo buffalo_l en CPU...")

    analizador = FaceAnalysis(
        name="buffalo_l",
        root=str(CARPETA_SERVICIO),
        providers=["CPUExecutionProvider"],
        allowed_modules=[
            "detection",
            "recognition",
        ],
    )

    analizador.prepare(
        ctx_id=-1,
        det_size=(320, 320),
    )

    print("Modelo listo.")

    return analizador


def analizar_captura(analizador, imagen, anteriores):
    rostros = analizador.get(
        imagen,
        max_num=0,
    )

    if len(rostros) != 1:
        raise ValueError(
            f"Se detectaron {len(rostros)} rostros; "
            "se necesita exactamente 1"
        )

    rostro = rostros[0]

    confianza = float(rostro.det_score)

    if confianza < 0.70:
        raise ValueError(
            f"Deteccion poco confiable: {confianza:.3f}"
        )

    alto, ancho = imagen.shape[:2]

    x1, y1, x2, y2 = np.asarray(
        rostro.bbox,
        dtype=int,
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
        cv2.COLOR_BGR2GRAY,
    )

    brillo = float(np.mean(gris))

    desenfoque = float(
        cv2.Laplacian(
            gris,
            cv2.CV_64F,
        ).var()
    )

    if not 45 <= brillo <= 220:
        raise ValueError(
            f"Ajusta la iluminacion: {brillo:.1f}"
        )

    if desenfoque < 30:
        raise ValueError(
            "La imagen esta borrosa. "
            "Manten la camara estable."
        )

    if rostro.embedding is None:
        raise ValueError(
            "InsightFace no genero un embedding"
        )

    vector = np.asarray(
        rostro.embedding,
        dtype=np.float32,
    ).reshape(-1)

    datos = vector.astype("<f4").tobytes()

    validar_embedding(datos)

    if anteriores:
        similitud = calcular_similitud(
            anteriores[0],
            datos,
        )

        if similitud < 0.45:
            raise ValueError(
                "Muestra inconsistente. "
                f"Similitud: {similitud:.3f}"
            )

    return datos, confianza


def capturar_muestras(analizador):
    camara = cv2.VideoCapture(
        0,
        cv2.CAP_DSHOW,
    )

    if not camara.isOpened():
        raise RuntimeError(
            "No se pudo abrir la camara 0"
        )

    camara.set(
        cv2.CAP_PROP_FRAME_WIDTH,
        640,
    )

    camara.set(
        cv2.CAP_PROP_FRAME_HEIGHT,
        480,
    )

    muestras = []
    calidades = []

    mensaje = "ESPACIO: capturar / Q o ESC: cancelar"

    try:
        while len(muestras) < MUESTRAS_REQUERIDAS:
            correcto, imagen = camara.read()

            if not correcto:
                raise RuntimeError(
                    "La camara dejo de entregar imagenes"
                )

            vista = imagen.copy()

            numero = len(muestras)

            cv2.putText(
                vista,
                (
                    f"FaceID integrante 08: "
                    f"{numero}/{MUESTRAS_REQUERIDAS}"
                ),
                (15, 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.6,
                (255, 255, 255),
                2,
            )

            cv2.putText(
                vista,
                POSICIONES[numero],
                (15, 65),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.52,
                (255, 255, 255),
                2,
            )

            cv2.putText(
                vista,
                mensaje[:65],
                (15, 100),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.45,
                (255, 255, 255),
                1,
            )

            cv2.imshow(
                "SIGELAB - Registro FaceID 08",
                vista,
            )

            tecla = cv2.waitKey(1) & 0xFF

            if tecla in (
                ord("q"),
                ord("Q"),
                27,
            ):
                print(
                    "Registro cancelado. "
                    "No se guardaron datos biometricos."
                )
                return None, None

            if tecla != 32:
                continue

            try:
                embedding, confianza = analizar_captura(
                    analizador,
                    imagen,
                    muestras,
                )

            except ValueError as error:
                mensaje = str(error)

                print(
                    "Captura rechazada:",
                    error,
                )
                continue

            muestras.append(embedding)
            calidades.append(confianza)

            mensaje = (
                "Muestra aceptada. "
                "Cambia suavemente de posicion."
            )

            print(
                f"Muestra {len(muestras)}/"
                f"{MUESTRAS_REQUERIDAS}: OK "
                f"(deteccion: {confianza:.3f})"
            )

    finally:
        camara.release()
        cv2.destroyAllWindows()

    return muestras, calidades


def promedio_embeddings(muestras):
    vectores = np.stack(
        [
            np.frombuffer(
                muestra,
                dtype="<f4",
            ).astype(np.float64)
            for muestra in muestras
        ]
    )

    vectores /= np.linalg.norm(
        vectores,
        axis=1,
        keepdims=True,
    )

    promedio = np.mean(
        vectores,
        axis=0,
    )

    norma = float(
        np.linalg.norm(promedio)
    )

    if norma < 1e-8:
        raise ValueError(
            "No se pudo generar un perfil facial estable"
        )

    datos = (
        promedio / norma
    ).astype("<f4").tobytes()

    validar_embedding(datos)

    return datos


def guardar_perfil(
    id_usuario,
    datos,
    muestras,
    calidades,
):
    """
    Registra un perfil facial nuevo.

    Comprueba la base de pruebas, la identidad
    del usuario y su estado.

    Utiliza guardar_perfil_nuevo para impedir
    sobrescrituras y proteger la transaccion.
    """

    conexion = get_connection()

    try:
        # 1. Exigir una transaccion controlada.
        conexion.autocommit = False

        cursor = conexion.cursor()

        # 2. Verificar la base autorizada.
        base = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()[0]

        if base != BASE_PRUEBAS:
            raise RuntimeError(
                "Guardado bloqueado: "
                "base de datos incorrecta"
            )

        # 3. Confirmar que el ID corresponde
        # al usuario configurado.
        usuario = cursor.execute(
            """
            SELECT estado
            FROM dbo.Usuario
            WHERE id_usuario = ?
              AND username = ?
            """,
            id_usuario,
            USERNAME,
        ).fetchone()

        if (
            usuario is None
            or str(usuario[0]).strip().upper() != "ACTIVO"
        ):
            raise RuntimeError(
                "El usuario ya no esta activo "
                "o no corresponde al registro solicitado"
            )

        # 4. Comprobar previamente si tiene perfil.
        existente = cursor.execute(
            """
            SELECT 1
            FROM dbo.PerfilFacial
            WHERE id_usuario = ?
            """,
            id_usuario,
        ).fetchone()

        if existente:
            raise RuntimeError(
                "Ya existe un perfil. "
                "No se sobrescribira."
            )

        # 5. Calcular calidad de las muestras.
        calidad_promedio = round(
            float(np.mean(calidades)),
            4,
        )

        # 6. Realizar el registro protegido.
        #
        # Esta funcion vuelve a comprobar:
        # - Base autorizada.
        # - Usuario existente y activo.
        # - Ausencia de perfil previo.
        #
        # Ademas utiliza bloqueos SQL para
        # proteger el registro concurrente.
        guardar_perfil_nuevo(
            id_usuario=id_usuario,
            embedding=datos,
            cantidad_muestras=len(muestras),
            calidad=calidad_promedio,
            conexion=conexion,
        )

        # 7. Confirmar perfil y auditoria juntos.
        conexion.commit()

    except Exception:
        conexion.rollback()
        raise

    finally:
        conexion.close()


def main():
    id_usuario = verificar_usuario()

    print("Base:", BASE_PRUEBAS)
    print("Usuario:", USERNAME)
    print("ID interno:", id_usuario)
    print()
    print(
        "Se capturaran 6 muestras. "
        "No se guardaran fotografias en disco."
    )
    print(
        "Haz clic en la ventana de la camara "
        "y pulsa ESPACIO para cada muestra."
    )

    analizador = cargar_modelo()

    muestras, calidades = capturar_muestras(
        analizador
    )

    if muestras is None:
        return

    datos = promedio_embeddings(
        muestras
    )

    print()
    print(
        "Perfil temporal preparado: "
        "512 dimensiones, 2048 bytes."
    )

    print(
        "Confianza media de deteccion:",
        round(
            float(np.mean(calidades)),
            4,
        ),
    )

    print(
        "Esta puntuacion no demuestra "
        "presencia de una persona viva."
    )

    confirmacion = input(
        "Escribe SI para guardar el perfil cifrado: "
    )

    if confirmacion.strip().upper() != "SI":
        print(
            "Registro descartado. "
            "No se guardo nada."
        )
        return

    guardar_perfil(
        id_usuario,
        datos,
        muestras,
        calidades,
    )

    estado = consultar_estado_facial(
        id_usuario
    )

    print()
    print("REGISTRO COMPLETADO")
    print(
        "Perfil registrado:",
        estado["perfil_registrado"],
    )
    print(
        "Perfil activo:",
        estado["perfil_activo"],
    )
    print(
        "Cantidad de muestras:",
        estado["cantidad_muestras"],
    )
    print(
        "El embedding quedo cifrado "
        "en SQL Server."
    )


if __name__ == "__main__":
    main()
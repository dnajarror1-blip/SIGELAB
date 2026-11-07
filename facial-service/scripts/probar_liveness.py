"""
SIGELAB - Prueba de presencia real.

Utiliza InsightFace buffalo_l y el complemento liveness.

Este script:
- No modifica SQL Server.
- No registra perfiles.
- No guarda fotografias.
- No guarda embeddings.
- No concede acceso al sistema.
"""

from pathlib import Path
from time import perf_counter

from insightface.app import FaceAnalysis

from scripts.diagnostico_embedding import capturar_rostro


CARPETA_SERVICIO = Path(__file__).resolve().parents[1]


def leer_campo(resultado, nombre):
    """
    Permite consultar resultados representados
    como objetos o diccionarios.
    """

    if isinstance(resultado, dict):
        return resultado.get(nombre)

    return getattr(resultado, nombre, None)


def main():

    print("SIGELAB - Diagnostico de presencia real")
    print("Modelo: buffalo_l + liveness")
    print("Proveedor: CPUExecutionProvider")
    print("Umbral inicial: 0.80 (NO calibrado)")
    print("No se registraran datos biometricos.")
    print("")

    # ======================================================
    # 1. CAPTURAR IMAGEN
    # ======================================================

    imagen = capturar_rostro()

    if imagen is None:
        print("Prueba cancelada sin guardar informacion.")
        return

    # ======================================================
    # 2. PREPARAR INSIGHTFACE
    # ======================================================

    print("Preparando modelos...")

    inicio = perf_counter()

    analizador = FaceAnalysis(
        name="buffalo_l",
        root=str(CARPETA_SERVICIO),
        providers=["CPUExecutionProvider"],
        allowed_modules=[
            "detection",
            "recognition"
        ],
        addons=["liveness"],
        liveness_mode="observe",
        liveness_threshold=0.8
    )

    analizador.prepare(
        ctx_id=-1,
        det_size=(320, 320)
    )

    tiempo_carga = perf_counter() - inicio

    print(f"Modelos preparados en {tiempo_carga:.2f} s")

    # ======================================================
    # 3. ANALIZAR ROSTRO
    # ======================================================

    print("Analizando captura...")

    inicio = perf_counter()

    rostros = analizador.get(
        imagen,
        max_num=0
    )

    tiempo_analisis = perf_counter() - inicio

    print(f"Analisis finalizado en {tiempo_analisis:.2f} s")

    if len(rostros) != 1:
        print(
            f"Prueba no concluyente: "
            f"se detectaron {len(rostros)} rostros."
        )
        return

    # ======================================================
    # 4. CONSULTAR RESULTADO DE LIVENESS
    # ======================================================

    resultado = getattr(
        rostros[0],
        "liveness",
        None
    )

    if resultado is None:
        print(
            "No se genero un resultado de presencia real."
        )
        return

    estado = leer_campo(
        resultado,
        "status"
    )

    es_vivo = leer_campo(
        resultado,
        "is_live"
    )

    puntuacion = leer_campo(
        resultado,
        "live_score"
    )

    # ======================================================
    # 5. MOSTRAR RESULTADOS
    # ======================================================

    print("")
    print("RESULTADO DEL MODELO DE PRESENCIA REAL")

    print("Estado:", estado)

    if puntuacion is not None:
        print(f"Puntuacion: {float(puntuacion):.4f}")
    else:
        print("Puntuacion: No disponible")

    print("Clasificacion:", es_vivo)

    if estado == "input_rejected":

        print(
            "La imagen fue rechazada. "
            "Aleja o centra el rostro y repite."
        )

    elif estado == "ok" and es_vivo is True:

        print(
            "El modelo clasifica esta captura "
            "como presencial."
        )

    elif estado == "ok" and es_vivo is False:

        print(
            "El modelo clasifica esta captura "
            "como posible suplantacion."
        )

    else:

        print(
            "Resultado no concluyente."
        )

    print("")
    print(
        "Esta prueba NO concede acceso "
        "ni demuestra proteccion absoluta."
    )

    print(
        "No se guardaron fotografias ni embeddings."
    )


if __name__ == "__main__":
    main()
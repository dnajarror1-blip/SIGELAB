
import time
from pathlib import Path

from insightface.app import FaceAnalysis


MODELO = "buffalo_l"
TAMANO_DETECCION = (320, 320)


def probar_modelo():
    inicio = time.perf_counter()

    # Carpeta principal del servicio facial.
    carpeta_servicio = Path(__file__).resolve().parent.parent

    print("SIGELAB - Diagnostico InsightFace")
    print("Modelo:", MODELO)
    print("Proveedor: CPUExecutionProvider")
    print("Carpeta:", carpeta_servicio / "models" / MODELO)
    print("")

    print("Cargando modelo facial...")
    print("La primera ejecucion puede descargar archivos.")
    print("")

    analizador = FaceAnalysis(
        name=MODELO,
        root=str(carpeta_servicio),
        providers=["CPUExecutionProvider"],
        allowed_modules=[
            "detection",
            "recognition"
        ]
    )

    analizador.prepare(
        ctx_id=-1,
        det_size=TAMANO_DETECCION
    )

    modulos = set(analizador.models.keys())

    if "detection" not in modulos:
        raise RuntimeError(
            "No se encontro el modulo de deteccion facial"
        )

    if "recognition" not in modulos:
        raise RuntimeError(
            "No se encontro el modulo de reconocimiento facial"
        )

    duracion = time.perf_counter() - inicio

    print("")
    print("MODELO CARGADO CORRECTAMENTE")
    print("Deteccion facial: OK")
    print("Reconocimiento facial: OK")
    print("Modulos:", sorted(modulos))
    print(f"Tiempo de inicializacion: {duracion:.2f} segundos")


if __name__ == "__main__":
    probar_modelo()

"""Proceso interactivo. Protocolo JSON por pipes, nunca por archivos ni argumentos biométricos."""
import base64
import contextlib
import json
import os
import sys


def main():
    # Todas las salidas de librerías van a stderr; stdout solo transporta el resultado en memoria.
    with contextlib.redirect_stdout(sys.stderr):
        request = json.load(sys.stdin)
        if request["operation"] == "enroll":
            from .facial_enrollment import preparar_perfil_preliminar
            perfil = preparar_perfil_preliminar()
            if perfil is None:
                response = {"cancelled": True}
            else:
                response = dict(perfil)
                response["embedding"] = base64.b64encode(perfil["embedding"]).decode("ascii")
        elif request["operation"] == "verify":
            from .facial_verification import capturar_verificacion
            registered = base64.b64decode(request["embedding"], validate=True)
            response = capturar_verificacion(registered)
        else:
            raise ValueError("Operación no válida")
    return response


if __name__ == "__main__":
    # Reservar stdout para el protocolo y redirigir también prints nativos (OpenCV/ONNX),
    # que no respetan contextlib.redirect_stdout.
    with os.fdopen(os.dup(sys.stdout.fileno()), "w", encoding="utf-8") as output:
        os.dup2(sys.stderr.fileno(), sys.stdout.fileno())
        exit_code = 0
        try:
            response = main()
        except Exception as error:
            from .camara import ErrorCamara
            code = ("CAMERA_ERROR" if isinstance(error, ErrorCamara) else
                    "CAPTURE_TIMEOUT" if isinstance(error, TimeoutError) else
                    "ENGINE_UNAVAILABLE" if isinstance(error, ImportError) else "CAPTURE_FAILED")
            response = {"error": code}
            exit_code = 1
        output.write(json.dumps(response, allow_nan=False))
    # El protocolo ya está escrito y cerrado, y la captura liberó cámara/GUI.
    # Terminar este proceso dedicado también detiene un análisis nativo pendiente:
    # sys.exit esperaría al hilo ONNX incluso después de cancelar o agotar tiempo.
    os._exit(exit_code)

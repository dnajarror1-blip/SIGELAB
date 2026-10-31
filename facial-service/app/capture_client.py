"""Ejecuta OpenCV en MainThread de otro proceso; timeout y pipes exclusivamente en memoria."""
import base64
import json
import subprocess
import sys

from .config import BASE_DIR, capture_timeout


class ErrorCaptura(RuntimeError):
    def __init__(self, code):
        super().__init__("No fue posible completar la captura facial")
        self.code = code


def ejecutar_captura(operation, embedding=None):
    request = {"operation": operation}
    if embedding is not None:
        request["embedding"] = base64.b64encode(embedding).decode("ascii")
    try:
        result = subprocess.run(
            [sys.executable, "-m", "app.capture_worker"], cwd=BASE_DIR,
            input=json.dumps(request), capture_output=True, text=True,
            timeout=capture_timeout() + 30, check=False,
        )
        response = json.loads(result.stdout)
        if not isinstance(response, dict):
            raise ValueError("Resultado de captura no válido")
        if result.returncode != 0 or "error" in response:
            allowed = {"CAMERA_ERROR", "CAPTURE_TIMEOUT", "ENGINE_UNAVAILABLE", "CAPTURE_FAILED"}
            code = response.get("error")
            raise ErrorCaptura(code if code in allowed else "CAPTURE_FAILED")
        if operation == "enroll" and not response.get("cancelled"):
            response["embedding"] = base64.b64decode(response["embedding"], validate=True)
        return response
    except subprocess.TimeoutExpired as error:
        raise ErrorCaptura("CAPTURE_TIMEOUT") from error
    except (ValueError, KeyError, OSError) as error:
        raise ErrorCaptura("CAPTURE_FAILED") from error

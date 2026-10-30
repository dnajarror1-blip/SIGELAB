"""Selección portable de cámara; solo debe llamarse desde el proceso interactivo."""
import os
import platform


class ErrorCamara(RuntimeError):
    pass


def abrir_camara():
    import cv2
    index = int(os.getenv("FACIAL_CAMERA_INDEX", "0"))
    preferred = os.getenv("FACIAL_CAMERA_BACKEND", "AUTO").upper()
    backends = {"DSHOW": cv2.CAP_DSHOW, "MSMF": cv2.CAP_MSMF,
                "V4L2": cv2.CAP_V4L2, "AVFOUNDATION": cv2.CAP_AVFOUNDATION, "ANY": cv2.CAP_ANY}
    if preferred == "AUTO":
        preferred = {"Windows": "DSHOW", "Linux": "V4L2", "Darwin": "AVFOUNDATION"}.get(platform.system(), "ANY")
    if preferred not in backends:
        raise RuntimeError("FACIAL_CAMERA_BACKEND no válido")
    for backend in dict.fromkeys((backends[preferred], cv2.CAP_ANY)):
        camera = cv2.VideoCapture(index, backend)
        if camera.isOpened():
            return camera
        camera.release()
    raise ErrorCamara("No se pudo abrir la cámara configurada")

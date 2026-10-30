"""Los estados inválidos se rechazan antes de recuperar el embedding y abrir cámara."""
import unittest
from unittest.mock import patch
from fastapi.testclient import TestClient
from app.main import app, validar_clave_api
from app.administracion_repository import RechazoFacial


class TestVerificacionEstadosAPI(unittest.TestCase):
    def test_estados_invalidos_no_abren_camara(self):
        app.dependency_overrides[validar_clave_api] = lambda: True
        try:
            for code in (403, 404, 409):
                with (patch("app.main.verificar_base_face"),
                      patch("app.auth_service.facial_threshold", return_value=.6),
                      patch("app.auth_service.obtener_clave"),
                      patch("app.auth_service.repository.registrar_evento"),
                      patch("app.auth_service.repository.version_perfil", side_effect=RechazoFacial(code,"Rechazado")),
                      patch("app.auth_service.obtener_embedding_facial") as embedding,
                      patch("app.auth_service.ejecutar_captura") as camera):
                    with TestClient(app) as client:
                        response = client.post("/face/verify", json={"userId": 7})
                    self.assertEqual(response.status_code, code)
                    camera.assert_not_called()
                    embedding.assert_not_called()
        finally:
            app.dependency_overrides.pop(validar_clave_api, None)

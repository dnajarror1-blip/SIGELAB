"""Contrato HTTP operativo: sin cámara, SQL ni carga de modelos."""
import unittest
from unittest.mock import patch
from fastapi.testclient import TestClient
from app.main import app, validar_clave_api
from app.administracion_repository import RechazoFacial


class TestMensajesVerificacion(unittest.TestCase):
    def setUp(self):
        app.dependency_overrides[validar_clave_api] = lambda: True
        self.client = TestClient(app)
        self.check = patch("app.main.verificar_base_face").start()

    def tearDown(self):
        self.client.close()
        app.dependency_overrides.pop(validar_clave_api, None)
        patch.stopall()

    def test_matched_conserva_decision_biometrica(self):
        for matched in (True, False):
            result = {"userId": 7, "matched": matched, "score": .9 if matched else .1,
                      "threshold": .6, "livenessPassed": True}
            with patch("app.auth_service.verificar", return_value=result) as verify:
                response = self.client.post("/face/verify", json={"userId": 7})
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.json(), result)
            verify.assert_called_once_with(7)

    def test_error_nativo_no_revela_detalles(self):
        with patch("app.auth_service.verificar", side_effect=RuntimeError("dato privado")):
            response = self.client.post("/face/verify", json={"userId": 7})
        self.assertEqual(response.status_code, 503)
        self.assertNotIn("privado", response.text)

    def test_esquemas_rechazan_credenciales_y_actor_ausente(self):
        for payload in ({"userId": 0}, {"userId": True}, {"userId": "7"},
                        {"userId": 7, "password": "prueba"}):
            self.assertEqual(self.client.post("/face/verify", json=payload).status_code, 422)
        self.assertEqual(self.client.post("/face/enroll", json={"userId": 7}).status_code, 422)

    def test_enroll_y_delete_transportan_actor(self):
        with patch("app.auth_service.registrar", return_value={"userId": 7, "registered": True}) as enroll:
            self.assertEqual(self.client.post("/face/enroll", json={"userId": 7, "adminUserId": 42}).status_code, 200)
            enroll.assert_called_once_with(7, 42, False)
        with patch("app.auth_service.eliminar", return_value={"userId": 7, "deleted": True}) as delete:
            self.assertEqual(self.client.delete("/face/profile/7?adminUserId=42").status_code, 200)
            delete.assert_called_once_with(7, 42)
        self.assertEqual(self.client.delete("/face/profile/7").status_code, 422)

    def test_autorizacion_denegada_se_traduce_a_403(self):
        with patch("app.auth_service.registrar", side_effect=RechazoFacial(403, "Administrador requerido")):
            self.assertEqual(self.client.post("/face/enroll", json={"userId": 7, "adminUserId": 42}).status_code, 403)

    def test_status_sin_embedding(self):
        state = {"id_usuario": 7, "usuario_existe": True, "usuario_activo": True,
                 "perfil_registrado": False, "perfil_activo": False}
        with patch("app.main.consultar_estado_facial", return_value=state):
            response = self.client.get("/face/status/7")
        self.assertEqual(response.json(), state)

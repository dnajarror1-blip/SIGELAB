import unittest

from fastapi.testclient import TestClient
from app.main import app


class TestSeguridadAPIFacial(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.cliente = TestClient(app)

    def test_consultar_sin_clave(self):
        """No permite consultar rostros sin clave."""

        respuesta = self.cliente.get(
            "/face/status/8"
        )

        self.assertEqual(respuesta.status_code, 401)

    def test_consultar_con_clave_incorrecta(self):
        """No acepta una clave de seguridad falsa."""

        respuesta = self.cliente.get(
            "/face/status/8",
            headers={
                "X-Facial-Api-Key":
                    "CLAVE_FALSA_PARA_PRUEBAS_123456789"
            }
        )

        self.assertEqual(respuesta.status_code, 401)

    def test_verificar_sin_clave(self):
        """No permite solicitar verificación sin autorización."""

        respuesta = self.cliente.post(
            "/face/verify",
            json={
                "userId": 8
            }
        )

        self.assertEqual(respuesta.status_code, 401)

    def test_verificar_con_clave_incorrecta(self):
        """No permite verificar con una clave falsa."""

        respuesta = self.cliente.post(
            "/face/verify",
            headers={
                "X-Facial-Api-Key":
                    "CLAVE_FALSA_PARA_PRUEBAS_123456789"
            },
            json={
                "userId": 8
            }
        )

        self.assertEqual(respuesta.status_code, 401)


if __name__ == "__main__":
    unittest.main()
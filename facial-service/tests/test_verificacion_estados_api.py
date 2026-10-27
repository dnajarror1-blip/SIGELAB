"""
SIGELAB - Pruebas de estados en /face/verify.

Verifica respuestas HTTP:
- 404: usuario inexistente.
- 403: usuario inactivo.
- 409: perfil facial inactivo.

No abre la camara.
No consulta ni modifica SQL Server.
No guarda datos biometricos.

La autenticacion de API se sustituye
exclusivamente dentro de estas pruebas.
"""

import unittest

from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import (
    app,
    validar_clave_api,
    BLOQUEO_CAMARA,
)


class TestVerificacionEstadosAPI(unittest.TestCase):

    def comprobar_rechazo(self, estado, codigo_esperado):

        # El bloqueo debe estar libre antes de iniciar.
        self.assertFalse(
            BLOQUEO_CAMARA.locked(),
            "El bloqueo de camara ya esta ocupado"
        )

        # Sustituir la autenticacion solo en esta prueba.
        app.dependency_overrides[validar_clave_api] = (
            lambda: None
        )

        try:
            # Simular las consultas para no utilizar SQL.
            with (
                patch("app.main.verificar_base_face"),
                patch(
                    "app.main.consultar_estado_facial",
                    return_value=estado,
                ),
                patch(
                    "app.main.obtener_embedding_facial"
                ) as obtener_embedding,
                patch(
                    "cv2.VideoCapture"
                ) as abrir_camara,
            ):

                with TestClient(app) as cliente:
                    respuesta = cliente.post(
                        "/face/verify",
                        json={"userId": 1},
                    )

                # Comprobar respuesta HTTP.
                self.assertEqual(
                    respuesta.status_code,
                    codigo_esperado,
                    (
                        "Codigo HTTP inesperado: "
                        f"{respuesta.status_code}"
                    )
                )

                # No debe recuperar embeddings.
                obtener_embedding.assert_not_called()

                # No debe abrir la camara.
                abrir_camara.assert_not_called()

        finally:
            # Restaurar la autenticacion original.
            app.dependency_overrides.pop(
                validar_clave_api,
                None
            )

        # El bloqueo debe quedar liberado.
        self.assertFalse(
            BLOQUEO_CAMARA.locked(),
            "El bloqueo de camara no fue liberado"
        )

    def test_usuario_inexistente(self):

        estado = {
            "usuario_existe": False,
            "usuario_activo": False,
            "perfil_activo": False,
        }

        self.comprobar_rechazo(
            estado,
            404
        )

        print(
            "\nOK: Usuario inexistente devuelve HTTP 404"
        )

    def test_usuario_inactivo(self):

        estado = {
            "usuario_existe": True,
            "usuario_activo": False,
            "perfil_activo": True,
        }

        self.comprobar_rechazo(
            estado,
            403
        )

        print(
            "\nOK: Usuario INACTIVO devuelve HTTP 403"
        )

    def test_perfil_inactivo(self):

        estado = {
            "usuario_existe": True,
            "usuario_activo": True,
            "perfil_activo": False,
        }

        self.comprobar_rechazo(
            estado,
            409
        )

        print(
            "\nOK: Perfil inactivo devuelve HTTP 409"
        )


if __name__ == "__main__":
    unittest.main()
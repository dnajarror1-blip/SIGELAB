import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

import app.main as facial


class TestMensajesVerificacion(unittest.TestCase):

    def setUp(self):
        self.dependencia_anterior = (
            facial.app.dependency_overrides.get(
                facial.validar_clave_api
            )
        )

        facial.app.dependency_overrides[
            facial.validar_clave_api
        ] = lambda: True

        self.client = TestClient(facial.app)

    def tearDown(self):
        self.client.close()

        if self.dependencia_anterior is None:
            facial.app.dependency_overrides.pop(
                facial.validar_clave_api,
                None,
            )
        else:
            facial.app.dependency_overrides[
                facial.validar_clave_api
            ] = self.dependencia_anterior

    def verificar_caso(
        self,
        estado_liveness,
        presencia_real,
        texto_esperado,
    ):
        usuario_activo = {
            "usuario_existe": True,
            "usuario_activo": True,
            "perfil_activo": True,
        }

        resultado_pipeline = {
            "similarity": 0.87,
            "livenessPassed": presencia_real,
            "livenessStatus": estado_liveness,
            "livenessScore": 0.75,
        }

        with (
            patch(
                "app.main.verificar_base_face"
            ),
            patch(
                "app.main.consultar_estado_facial",
                return_value=usuario_activo,
            ),
            patch(
                "app.main.obtener_embedding_facial",
                return_value=b"EMBEDDING_SIMULADO",
            ),
            patch(
                "app.main.validar_embedding"
            ),
            patch(
                "scripts.diagnostico_embedding.capturar_rostro",
                return_value=object(),
            ) as captura,
            patch(
                "app.main.analizar_captura_facial",
                return_value=resultado_pipeline,
            ),
            patch(
                "app.main.registrar_auditoria_verificacion"
            ) as auditoria,
        ):

            respuesta = self.client.post(
                "/face/verify",
                json={"userId": 1},
            )

            self.assertEqual(
                respuesta.status_code,
                200,
                respuesta.text,
            )

            datos = respuesta.json()

            self.assertIn(
                texto_esperado,
                datos["message"],
            )

            self.assertEqual(
                datos["result"],
                "DIAGNOSTIC_ONLY",
            )

            self.assertFalse(
                datos["verified"]
            )

            self.assertFalse(
                datos["authenticationAllowed"]
            )

            self.assertEqual(
                datos["livenessPassed"],
                presencia_real,
            )

            captura.assert_called_once()
            auditoria.assert_called_once()

            self.assertFalse(
                facial.BLOQUEO_CAMARA.locked()
            )

    def test_captura_no_concluyente(self):
        self.verificar_caso(
            estado_liveness="input_rejected",
            presencia_real=None,
            texto_esperado="Captura no concluyente",
        )

        print(
            "OK: Captura no concluyente identificada"
        )

    def test_presencia_real_rechazada(self):
        self.verificar_caso(
            estado_liveness="rejected",
            presencia_real=False,
            texto_esperado="no supero la comprobacion",
        )

        print(
            "OK: Presencia real rechazada correctamente"
        )

    def test_diagnostico_completado(self):
        self.verificar_caso(
            estado_liveness="passed",
            presencia_real=True,
            texto_esperado="comparacion facial completada",
        )

        print(
            "OK: Diagnostico completado correctamente"
        )
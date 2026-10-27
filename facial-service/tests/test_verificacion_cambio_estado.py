"""
SIGELAB - Cambio de estado durante FaceID.

Comprueba que la verificacion facial se rechaza
cuando un usuario es desactivado durante
el procesamiento de su rostro.

Todas las operaciones externas son simuladas.

No abre la camara.
No consulta SQL Server.
No modifica perfiles.
No registra auditorias reales.
"""

import unittest

from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import (
    app,
    validar_clave_api,
    BLOQUEO_CAMARA,
)


class TestCambioEstadoFacial(unittest.TestCase):

    def test_usuario_desactivado_durante_verificacion(self):

        # Estado antes de iniciar la captura.
        estado_inicial = {
            "usuario_existe": True,
            "usuario_activo": True,
            "perfil_activo": True,
        }

        # Estado despues del analisis facial.
        estado_final = {
            "usuario_existe": True,
            "usuario_activo": False,
            "perfil_activo": True,
        }

        # Resultado artificial del reconocimiento.
        resultado_simulado = {
            "similarity": 0.85,
            "livenessPassed": True,
            "livenessStatus": "ok",
            "livenessScore": 0.99,
        }

        self.assertFalse(
            BLOQUEO_CAMARA.locked(),
            "El bloqueo de camara esta ocupado"
        )

        # Sustituir autenticacion solamente en esta prueba.
        app.dependency_overrides[validar_clave_api] = (
            lambda: None
        )

        try:
            with (
                patch("app.main.verificar_base_face"),

                patch(
                    "app.main.consultar_estado_facial",
                    side_effect=[
                        estado_inicial,
                        estado_final,
                    ],
                ) as consultar_estado,

                patch(
                    "app.main.obtener_embedding_facial",
                    return_value=b"PERFIL_SIMULADO",
                ) as obtener_embedding,

                patch(
                    "app.main.validar_embedding"
                ),

                patch(
                    "scripts.diagnostico_embedding.capturar_rostro",
                    return_value=object(),
                ) as capturar_rostro,

                patch(
                    "app.main.analizar_captura_facial",
                    return_value=resultado_simulado,
                ) as analizar_rostro,

                patch(
                    "app.main.registrar_auditoria_verificacion"
                ) as registrar_auditoria,

                patch(
                    "cv2.VideoCapture"
                ) as abrir_camara,
            ):

                with TestClient(app) as cliente:
                    respuesta = cliente.post(
                        "/face/verify",
                        json={"userId": 1},
                    )

                # La cuenta debe ser rechazada.
                self.assertEqual(
                    respuesta.status_code,
                    403
                )

                self.assertEqual(
                    respuesta.json()["detail"],
                    (
                        "La cuenta fue desactivada "
                        "durante la verificacion facial"
                    )
                )

                # Deben haberse realizado dos consultas
                # de estado: antes y despues del analisis.
                self.assertEqual(
                    consultar_estado.call_count,
                    2
                )

                # El flujo simulado debe llegar al analisis.
                obtener_embedding.assert_called_once()

                capturar_rostro.assert_called_once()

                analizar_rostro.assert_called_once()

                # Una verificacion rechazada no debe
                # registrarse como verificacion completada.
                registrar_auditoria.assert_not_called()

                # No debe abrirse una camara real.
                abrir_camara.assert_not_called()

                print(
                    "\nOK: Usuario inicialmente ACTIVO"
                )

                print(
                    "OK: Captura y analisis simulados"
                )

                print(
                    "OK: Usuario desactivado durante el proceso"
                )

                print(
                    "OK: Verificacion rechazada con HTTP 403"
                )

                print(
                    "OK: No se registro verificacion exitosa"
                )

                print(
                    "OK: Camara real no utilizada"
                )

        finally:
            # Restaurar dependencia original.
            app.dependency_overrides.pop(
                validar_clave_api,
                None
            )

        # Comprobar que el bloqueo se libero.
        self.assertFalse(
            BLOQUEO_CAMARA.locked(),
            "El bloqueo de camara no fue liberado"
        )

        print(
            "OK: Bloqueo de camara liberado"
        )


if __name__ == "__main__":
    unittest.main()
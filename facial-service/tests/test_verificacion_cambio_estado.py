"""
SIGELAB - Auditoria por cambio de estado durante FaceID.

Comprueba que una verificacion facial sea rechazada
si el administrador desactiva la cuenta o el perfil
durante el procesamiento.

Las dependencias externas estan simuladas.

No utiliza camara real.
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
    registrar_rechazo_por_estado,
)


class TestCambioEstadoFacial(unittest.TestCase):

    def verificar_rechazo(
        self,
        estado_final,
        codigo_esperado,
        mensaje_esperado,
        causa_esperada,
    ):

        estado_inicial = {
            "usuario_existe": True,
            "usuario_activo": True,
            "perfil_activo": True,
        }

        resultado_simulado = {
            "similarity": 0.85,
            "livenessPassed": True,
            "livenessStatus": "ok",
            "livenessScore": 0.99,
        }

        self.assertFalse(
            BLOQUEO_CAMARA.locked()
        )

        dependencia_anterior = (
            app.dependency_overrides.get(
                validar_clave_api
            )
        )

        app.dependency_overrides[
            validar_clave_api
        ] = lambda: None

        try:
            with (
                patch(
                    "app.main.verificar_base_face"
                ),

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
                    "app.main.registrar_rechazo_por_estado"
                ) as registrar_rechazo,

                patch(
                    "app.main.registrar_auditoria_verificacion"
                ) as auditoria_completada,

                patch(
                    "cv2.VideoCapture"
                ) as abrir_camara,
            ):

                with TestClient(app) as cliente:

                    respuesta = cliente.post(
                        "/face/verify",
                        json={"userId": 1},
                    )

                self.assertEqual(
                    respuesta.status_code,
                    codigo_esperado,
                )

                self.assertEqual(
                    respuesta.json()["detail"],
                    mensaje_esperado,
                )

                self.assertEqual(
                    consultar_estado.call_count,
                    2,
                )

                obtener_embedding.assert_called_once()

                capturar_rostro.assert_called_once()

                analizar_rostro.assert_called_once()

                registrar_rechazo.assert_called_once_with(
                    1,
                    causa_esperada,
                )

                auditoria_completada.assert_not_called()

                abrir_camara.assert_not_called()

        finally:

            if dependencia_anterior is None:
                app.dependency_overrides.pop(
                    validar_clave_api,
                    None,
                )
            else:
                app.dependency_overrides[
                    validar_clave_api
                ] = dependencia_anterior

        self.assertFalse(
            BLOQUEO_CAMARA.locked(),
            "La camara quedo bloqueada",
        )

    def test_usuario_desactivado_durante_verificacion(self):

        estado_final = {
            "usuario_existe": True,
            "usuario_activo": False,
            "perfil_activo": True,
        }

        self.verificar_rechazo(
            estado_final=estado_final,
            codigo_esperado=403,
            mensaje_esperado=(
                "La cuenta fue desactivada "
                "durante la verificacion facial"
            ),
            causa_esperada="USUARIO_DESACTIVADO",
        )

        print(
            "OK: Usuario desactivado rechazado y auditado"
        )

    def test_perfil_desactivado_durante_verificacion(self):

        estado_final = {
            "usuario_existe": True,
            "usuario_activo": True,
            "perfil_activo": False,
        }

        self.verificar_rechazo(
            estado_final=estado_final,
            codigo_esperado=409,
            mensaje_esperado=(
                "El perfil facial dejo de estar activo "
                "durante la verificacion"
            ),
            causa_esperada="PERFIL_DESACTIVADO",
        )

        print(
            "OK: Perfil desactivado rechazado y auditado"
        )

    def test_formato_auditoria_usuario(self):

        with patch(
            "app.main._guardar_auditoria"
        ) as guardar:

            registrar_rechazo_por_estado(
                1,
                "USUARIO_DESACTIVADO",
            )

            guardar.assert_called_once_with(
                1,
                resultado="RECHAZADO_ESTADO",
                detalle=(
                    "La cuenta fue desactivada "
                    "durante la verificacion facial"
                ),
            )

        print(
            "OK: Formato de auditoria de usuario correcto"
        )

    def test_formato_auditoria_perfil(self):

        with patch(
            "app.main._guardar_auditoria"
        ) as guardar:

            registrar_rechazo_por_estado(
                1,
                "PERFIL_DESACTIVADO",
            )

            guardar.assert_called_once_with(
                1,
                resultado="RECHAZADO_ESTADO",
                detalle=(
                    "El perfil facial fue desactivado "
                    "durante la verificacion"
                ),
            )

        print(
            "OK: Formato de auditoria de perfil correcto"
        )


if __name__ == "__main__":
    unittest.main()
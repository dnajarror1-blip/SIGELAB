"""
Pruebas de reutilizacion del modelo facial SIGELAB.

No utilizan:
- Camara.
- SQL Server.
- Fotografias reales.
- Perfiles biometricos de usuarios.
"""

import struct
import unittest

from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import numpy as np

from app import facial_pipeline


class TestFacialPipelineCache(unittest.TestCase):

    def setUp(self):
        facial_pipeline._obtener_analizador.cache_clear()

        self.embedding = struct.pack(
            "<512f",
            *([0.1] * 512)
        )

        self.imagen = object()

    def tearDown(self):
        facial_pipeline._obtener_analizador.cache_clear()

    def crear_rostro(
        self,
        *,
        estado="ok",
        es_vivo=True,
        puntuacion=0.95
    ):
        evaluacion = SimpleNamespace(
            status=estado,
            is_live=es_vivo,
            live_score=puntuacion
        )

        rostro = SimpleNamespace(
            liveness=evaluacion,
            embedding=np.full(
                512,
                0.1,
                dtype=np.float32
            )
        )

        return rostro

    def test_modelo_se_carga_una_sola_vez(self):

        analizador = MagicMock()

        analizador.get.return_value = [
            self.crear_rostro()
        ]

        with patch.object(
            facial_pipeline,
            "FaceAnalysis",
            return_value=analizador
        ) as constructor:

            with patch.object(
                facial_pipeline,
                "calcular_similitud",
                return_value=0.93
            ):

                primero = (
                    facial_pipeline.analizar_captura_facial(
                        self.imagen,
                        self.embedding
                    )
                )

                segundo = (
                    facial_pipeline.analizar_captura_facial(
                        self.imagen,
                        self.embedding
                    )
                )

        self.assertEqual(
            constructor.call_count,
            1
        )

        self.assertEqual(
            analizador.prepare.call_count,
            1
        )

        self.assertEqual(
            analizador.get.call_count,
            2
        )

        self.assertTrue(
            primero["livenessPassed"]
        )

        self.assertTrue(
            segundo["livenessPassed"]
        )

        self.assertEqual(
            primero["similarity"],
            0.93
        )

        print(
            "OK: Modelo inicializado una vez y reutilizado"
        )

    def test_suplantacion_no_compara_identidad(self):

        analizador = MagicMock()

        analizador.get.return_value = [
            self.crear_rostro(
                es_vivo=False,
                puntuacion=0.12
            )
        ]

        with patch.object(
            facial_pipeline,
            "FaceAnalysis",
            return_value=analizador
        ):

            with patch.object(
                facial_pipeline,
                "calcular_similitud"
            ) as comparar:

                resultado = (
                    facial_pipeline.analizar_captura_facial(
                        self.imagen,
                        self.embedding
                    )
                )

                comparar.assert_not_called()

        self.assertFalse(
            resultado["livenessPassed"]
        )

        self.assertIsNone(
            resultado["similarity"]
        )

        print(
            "OK: Posible suplantacion bloquea comparacion"
        )

    def test_captura_no_concluyente(self):

        analizador = MagicMock()

        analizador.get.return_value = [
            self.crear_rostro(
                estado="input_rejected",
                es_vivo=None,
                puntuacion=None
            )
        ]

        with patch.object(
            facial_pipeline,
            "FaceAnalysis",
            return_value=analizador
        ):

            with patch.object(
                facial_pipeline,
                "calcular_similitud"
            ) as comparar:

                resultado = (
                    facial_pipeline.analizar_captura_facial(
                        self.imagen,
                        self.embedding
                    )
                )

                comparar.assert_not_called()

        self.assertEqual(
            resultado["livenessStatus"],
            "input_rejected"
        )

        self.assertIsNone(
            resultado["livenessPassed"]
        )

        self.assertIsNone(
            resultado["similarity"]
        )

        print(
            "OK: Captura no concluyente respetada"
        )

    def test_identidad_invalida_rechazada(self):

        with patch.object(
            facial_pipeline,
            "FaceAnalysis"
        ) as constructor:

            with self.assertRaises(ValueError):

                facial_pipeline.analizar_captura_facial(
                    self.imagen,
                    b"datos-invalidos"
                )

            constructor.assert_not_called()

        print(
            "OK: Embedding invalido rechazado antes de cargar modelo"
        )

    def test_fallo_de_carga_no_se_almacena(self):

        analizador = MagicMock()

        analizador.get.return_value = [
            self.crear_rostro()
        ]

        with patch.object(
            facial_pipeline,
            "FaceAnalysis",
            side_effect=[
                RuntimeError("Fallo simulado"),
                analizador
            ]
        ) as constructor:

            with self.assertRaises(RuntimeError):

                facial_pipeline.analizar_captura_facial(
                    self.imagen,
                    self.embedding
                )

            with patch.object(
                facial_pipeline,
                "calcular_similitud",
                return_value=0.91
            ):

                resultado = (
                    facial_pipeline.analizar_captura_facial(
                        self.imagen,
                        self.embedding
                    )
                )

        self.assertEqual(
            constructor.call_count,
            2
        )

        self.assertTrue(
            resultado["livenessPassed"]
        )

        print(
            "OK: Fallo de carga no queda almacenado"
        )


if __name__ == "__main__":
    unittest.main()
"""
SIGELAB FaceID - Pruebas de preparacion facial.

Comprueba:
- Configuracion del modelo con liveness.
- Rechazo de posibles suplantaciones.
- Rechazo de capturas de baja calidad.
- Consistencia entre muestras.
- Generacion del embedding promedio.
- Captura de seis muestras.
- Cancelacion y tiempo de espera.
- Liberacion de la camara.

SEGURIDAD:
No utiliza una camara real.
No modifica SQL Server.
No registra perfiles.
No almacena fotografias ni embeddings.
No concede acceso al sistema.
"""

import unittest

from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import numpy as np

from app import facial_enrollment as enrollment


class TestFacialEnrollment(unittest.TestCase):

    def setUp(self):

        self.imagen = np.full(
            (480, 640, 3),
            128,
            dtype=np.uint8
        )

        vector = np.zeros(
            512,
            dtype="<f4"
        )

        vector[0] = 1.0

        self.embedding = vector.tobytes()

    def crear_rostro(
        self,
        *,
        vivo=True,
        estado="ok",
        confianza=0.95
    ):

        liveness = SimpleNamespace(
            status=estado,
            is_live=vivo,
            live_score=0.99 if vivo else 0.02
        )

        return SimpleNamespace(
            liveness=liveness,
            det_score=confianza,
            bbox=[100, 100, 300, 300],
            embedding=np.frombuffer(
                self.embedding,
                dtype="<f4"
            ).copy()
        )

    def crear_analizador(self, rostro):

        analizador = MagicMock()

        analizador.get.return_value = [
            rostro
        ]

        return analizador

    def analizar_con_calidad_simulada(
        self,
        analizador,
        anteriores
    ):

        gris = np.full(
            (200, 200),
            120,
            dtype=np.uint8
        )

        bordes = np.tile(
            [0.0, 100.0],
            20000
        ).reshape(200, 200)

        with (
            patch.object(
                enrollment.cv2,
                "cvtColor",
                return_value=gris
            ),
            patch.object(
                enrollment.cv2,
                "Laplacian",
                return_value=bordes
            )
        ):

            return enrollment.analizar_muestra_registro(
                analizador,
                self.imagen,
                anteriores
            )

    def crear_camara_simulada(self):

        camara = MagicMock()

        camara.isOpened.return_value = True

        camara.read.return_value = (
            True,
            self.imagen
        )

        return camara

    # --------------------------------------------------
    # 1. MODELO FACIAL
    # --------------------------------------------------

    def test_modelo_utiliza_liveness(self):

        analizador = MagicMock()

        with patch.object(
            enrollment,
            "FaceAnalysis",
            return_value=analizador
        ) as constructor:

            resultado = enrollment.cargar_modelo_registro()

        self.assertIs(
            resultado,
            analizador
        )

        opciones = constructor.call_args.kwargs

        self.assertEqual(
            opciones["name"],
            "buffalo_l"
        )

        self.assertIn(
            "liveness",
            opciones["addons"]
        )

        self.assertEqual(
            opciones["liveness_mode"],
            "normal"
        )

        self.assertEqual(
            opciones["liveness_threshold"],
            enrollment.UMBRAL_LIVENESS_INICIAL
        )

        analizador.prepare.assert_called_once_with(
            ctx_id=-1,
            det_size=(320, 320)
        )

    # --------------------------------------------------
    # 2. PRESENCIA REAL
    # --------------------------------------------------

    def test_suplantacion_rechazada(self):

        rostro = self.crear_rostro(
            vivo=False
        )

        analizador = self.crear_analizador(
            rostro
        )

        with patch.object(
            enrollment,
            "calcular_similitud"
        ) as similitud:

            with self.assertRaisesRegex(
                ValueError,
                "presencia real"
            ):

                enrollment.analizar_muestra_registro(
                    analizador,
                    self.imagen,
                    []
                )

            similitud.assert_not_called()

    def test_captura_no_concluyente_rechazada(self):

        rostro = self.crear_rostro(
            estado="input_rejected"
        )

        analizador = self.crear_analizador(
            rostro
        )

        with self.assertRaisesRegex(
            ValueError,
            "Centra tu rostro"
        ):

            enrollment.analizar_muestra_registro(
                analizador,
                self.imagen,
                []
            )

    # --------------------------------------------------
    # 3. CALIDAD DE CAPTURA
    # --------------------------------------------------

    def test_confianza_baja_rechazada(self):

        rostro = self.crear_rostro(
            confianza=0.60
        )

        analizador = self.crear_analizador(
            rostro
        )

        with self.assertRaisesRegex(
            ValueError,
            "poco confiable"
        ):

            enrollment.analizar_muestra_registro(
                analizador,
                self.imagen,
                []
            )

    def test_muestras_inconsistentes_rechazadas(self):

        rostro = self.crear_rostro()

        analizador = self.crear_analizador(
            rostro
        )

        with patch.object(
            enrollment,
            "calcular_similitud",
            return_value=0.20
        ) as comparar:

            with self.assertRaisesRegex(
                ValueError,
                "inconsistente"
            ):

                self.analizar_con_calidad_simulada(
                    analizador,
                    [self.embedding]
                )

            comparar.assert_called_once()

    def test_muestra_valida_aceptada(self):

        rostro = self.crear_rostro()

        analizador = self.crear_analizador(
            rostro
        )

        with patch.object(
            enrollment,
            "calcular_similitud",
            return_value=0.90
        ):

            datos, confianza, liveness = (
                self.analizar_con_calidad_simulada(
                    analizador,
                    [self.embedding]
                )
            )

        self.assertEqual(
            len(datos),
            2048
        )

        self.assertAlmostEqual(
            confianza,
            0.95
        )

        self.assertAlmostEqual(
            liveness,
            0.99
        )

    # --------------------------------------------------
    # 4. EMBEDDING PROMEDIO
    # --------------------------------------------------

    def test_promedio_exige_seis_muestras(self):

        muestras = [
            self.embedding
        ] * 5

        with self.assertRaisesRegex(
            ValueError,
            "Cantidad de muestras incompleta"
        ):

            enrollment.promedio_embeddings(
                muestras
            )

    def test_promedio_normalizado(self):

        muestras = [
            self.embedding
        ] * 6

        resultado = enrollment.promedio_embeddings(
            muestras
        )

        self.assertEqual(
            len(resultado),
            2048
        )

        vector = np.frombuffer(
            resultado,
            dtype="<f4"
        )

        self.assertEqual(
            len(vector),
            512
        )

        norma = np.linalg.norm(
            vector
        )

        self.assertAlmostEqual(
            float(norma),
            1.0,
            places=5
        )

    # --------------------------------------------------
    # 5. CAMARA SIMULADA
    # --------------------------------------------------

    def test_cancelacion_libera_camara(self):

        camara = self.crear_camara_simulada()

        with (
            patch.object(
                enrollment.cv2,
                "VideoCapture",
                return_value=camara
            ),
            patch.object(
                enrollment.cv2,
                "putText"
            ),
            patch.object(
                enrollment.cv2,
                "imshow"
            ),
            patch.object(enrollment.cv2, "getWindowProperty", return_value=1),
            patch.object(
                enrollment.cv2,
                "waitKey",
                return_value=ord("q")
            ),
            patch.object(
                enrollment.cv2,
                "destroyAllWindows"
            ) as cerrar_ventanas
        ):

            resultado = (
                enrollment.capturar_muestras_registro(
                    MagicMock()
                )
            )

        self.assertIsNone(
            resultado
        )

        camara.release.assert_called_once()

        cerrar_ventanas.assert_called_once()

    def test_tiempo_agotado_libera_camara(self):

        camara = self.crear_camara_simulada()

        with (
            patch.object(
                enrollment.cv2,
                "VideoCapture",
                return_value=camara
            ),
            patch.object(
                enrollment,
                "perf_counter",
                side_effect=[0.0, 121.0]
            ),
            patch.object(
                enrollment.cv2,
                "destroyAllWindows"
            )
        ):

            with self.assertRaises(
                TimeoutError
            ):

                enrollment.capturar_muestras_registro(
                    MagicMock()
                )

        camara.release.assert_called_once()

    def test_captura_seis_muestras(self):

        camara = self.crear_camara_simulada()

        with (
            patch.object(
                enrollment.cv2,
                "VideoCapture",
                return_value=camara
            ),
            patch.object(
                enrollment.cv2,
                "putText"
            ),
            patch.object(
                enrollment.cv2,
                "imshow"
            ),
            patch.object(enrollment.cv2, "getWindowProperty", return_value=1),
            patch.object(
                enrollment.cv2,
                "waitKey",
                return_value=32
            ),
            patch.object(
                enrollment.cv2,
                "destroyAllWindows"
            ),
            patch.object(
                enrollment,
                "analizar_muestra_registro",
                return_value=(
                    self.embedding,
                    0.95,
                    0.99
                )
            ) as analizar
        ):

            resultado = (
                enrollment.capturar_muestras_registro(
                    MagicMock()
                )
            )

        muestras, confianzas, puntuaciones = resultado

        self.assertEqual(
            len(muestras),
            6
        )

        self.assertEqual(
            len(confianzas),
            6
        )

        self.assertEqual(
            len(puntuaciones),
            6
        )

        self.assertEqual(
            analizar.call_count,
            6
        )

        camara.release.assert_called_once()

    # --------------------------------------------------
    # 6. PREPARACION FINAL
    # --------------------------------------------------

    def test_preparacion_perfil_correcta(self):

        analizador = MagicMock()

        muestras = [
            self.embedding
        ] * 6

        with (
            patch.object(
                enrollment,
                "cargar_modelo_registro",
                return_value=analizador
            ),
            patch.object(
                enrollment,
                "capturar_muestras_registro",
                return_value=(
                    muestras,
                    [0.95] * 6,
                    [0.99] * 6
                )
            ) as capturar
        ):

            resultado = (
                enrollment.preparar_perfil_preliminar()
            )

        capturar.assert_called_once_with(
            analizador
        )

        self.assertEqual(
            resultado["cantidad_muestras"],
            6
        )

        self.assertEqual(
            len(resultado["embedding"]),
            2048
        )

        self.assertEqual(
            resultado["confianza_deteccion_media"],
            0.95
        )

        self.assertEqual(
            resultado["liveness_promedio"],
            0.99
        )

    def test_preparacion_cancelada(self):

        with (
            patch.object(
                enrollment,
                "cargar_modelo_registro"
            ),
            patch.object(
                enrollment,
                "capturar_muestras_registro",
                return_value=None
            ),
            patch.object(
                enrollment,
                "promedio_embeddings"
            ) as promedio
        ):

            resultado = (
                enrollment.preparar_perfil_preliminar()
            )

        self.assertIsNone(
            resultado
        )

        promedio.assert_not_called()


if __name__ == "__main__":
    unittest.main()
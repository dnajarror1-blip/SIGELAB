"""
SIGELAB FaceID - Pruebas del coordinador interno.

Comprueba:
- Identificadores invalidos.
- Usuarios inexistentes e inactivos.
- Perfiles duplicados.
- Bloqueo compartido de camara.
- Cancelaciones.
- Transacciones y rollback.
- Validacion de la base de pruebas.

No abre una camara real.
No modifica SQL Server.
No registra datos biometricos.
"""

import struct
import unittest

from unittest.mock import MagicMock, patch

from app import registro_facial_interno as registro


class TestRegistroFacialInterno(unittest.TestCase):

    def reemplazar(self, nombre, **opciones):

        parche = patch.object(
            registro,
            nombre,
            **opciones
        )

        self.addCleanup(
            parche.stop
        )

        return parche.start()

    def setUp(self):

        self.id_usuario = 7

        self.embedding = struct.pack(
            "<512f",
            *([0.01] * 512)
        )

        self.perfil = {
            "embedding": self.embedding,
            "cantidad_muestras": 6,
            "confianza_deteccion_media": 0.95,
            "liveness_promedio": 0.99,
        }

        self.estado = {
            "usuario_existe": True,
            "usuario_activo": True,
            "perfil_registrado": False,
            "perfil_activo": False,
        }

        self.conexion = MagicMock()
        self.conexion.autocommit = True

        self.bloqueo = MagicMock()
        self.bloqueo.acquire.return_value = True

        self.verificar_base = self.reemplazar(
            "_verificar_entorno_pruebas"
        )

        self.consultar_estado = self.reemplazar(
            "consultar_estado_facial",
            return_value=self.estado
        )

        self.preparar = self.reemplazar(
            "preparar_perfil_preliminar",
            return_value=self.perfil
        )

        self.obtener_conexion = self.reemplazar(
            "get_connection",
            return_value=self.conexion
        )

        self.guardar = self.reemplazar(
            "guardar_perfil_nuevo",
            return_value=True
        )

        self.reemplazar(
            "BLOQUEO_CAMARA",
            new=self.bloqueo
        )

    # --------------------------------------------------
    # 1. IDENTIFICADORES
    # --------------------------------------------------

    def test_identificadores_invalidos(self):

        invalidos = [
            None,
            0,
            -1,
            True,
            "7",
            7.5,
        ]

        for identificador in invalidos:

            with self.subTest(
                identificador=identificador
            ):

                with self.assertRaises(
                    ValueError
                ):

                    registro._registrar_perfil_interno(
                        identificador
                    )

        self.verificar_base.assert_not_called()
        self.preparar.assert_not_called()
        self.guardar.assert_not_called()

    # --------------------------------------------------
    # 2. ESTADO DEL USUARIO
    # --------------------------------------------------

    def test_usuario_inexistente(self):

        self.estado["usuario_existe"] = False

        with self.assertRaisesRegex(
            ValueError,
            "usuario no existe"
        ):

            registro._registrar_perfil_interno(
                self.id_usuario
            )

        self.bloqueo.acquire.assert_not_called()
        self.preparar.assert_not_called()
        self.guardar.assert_not_called()

    def test_usuario_inactivo(self):

        self.estado["usuario_activo"] = False

        with self.assertRaisesRegex(
            ValueError,
            "usuario no esta activo"
        ):

            registro._registrar_perfil_interno(
                self.id_usuario
            )

        self.bloqueo.acquire.assert_not_called()
        self.preparar.assert_not_called()
        self.guardar.assert_not_called()

    def test_perfil_duplicado(self):

        self.estado["perfil_registrado"] = True

        with self.assertRaisesRegex(
            ValueError,
            "ya tiene un perfil facial"
        ):

            registro._registrar_perfil_interno(
                self.id_usuario
            )

        self.bloqueo.acquire.assert_not_called()
        self.preparar.assert_not_called()
        self.guardar.assert_not_called()

    # --------------------------------------------------
    # 3. CONTROL DE CAMARA
    # --------------------------------------------------

    def test_camara_ocupada(self):

        self.bloqueo.acquire.return_value = False

        with self.assertRaisesRegex(
            RuntimeError,
            "camara esta siendo utilizada"
        ):

            registro._registrar_perfil_interno(
                self.id_usuario
            )

        self.bloqueo.acquire.assert_called_once_with(
            blocking=False
        )

        self.bloqueo.release.assert_not_called()
        self.preparar.assert_not_called()
        self.obtener_conexion.assert_not_called()

    def test_cancelacion_no_guarda(self):

        self.preparar.return_value = None

        resultado = registro._registrar_perfil_interno(
            self.id_usuario
        )

        self.assertEqual(
            resultado["result"],
            "CANCELADO"
        )

        self.assertFalse(
            resultado["registered"]
        )

        self.bloqueo.release.assert_called_once()

        self.obtener_conexion.assert_not_called()
        self.guardar.assert_not_called()

    def test_fallo_captura_libera_camara(self):

        self.preparar.side_effect = RuntimeError(
            "Fallo simulado de captura"
        )

        with self.assertRaisesRegex(
            RuntimeError,
            "Fallo simulado"
        ):

            registro._registrar_perfil_interno(
                self.id_usuario
            )

        self.bloqueo.release.assert_called_once()

        self.obtener_conexion.assert_not_called()
        self.guardar.assert_not_called()

    # --------------------------------------------------
    # 4. VALIDACIONES DEL PERFIL
    # --------------------------------------------------

    def test_cantidad_muestras_invalida(self):

        self.perfil["cantidad_muestras"] = 5

        with self.assertRaisesRegex(
            ValueError,
            "seis muestras"
        ):

            registro._registrar_perfil_interno(
                self.id_usuario
            )

        self.bloqueo.release.assert_called_once()

        self.obtener_conexion.assert_not_called()
        self.guardar.assert_not_called()

    # --------------------------------------------------
    # 5. TRANSACCIONES
    # --------------------------------------------------

    def test_registro_correcto(self):

        resultado = registro._registrar_perfil_interno(
            self.id_usuario
        )

        self.assertEqual(
            resultado["result"],
            "REGISTRADO"
        )

        self.assertTrue(
            resultado["registered"]
        )

        self.assertEqual(
            resultado["userId"],
            self.id_usuario
        )

        self.assertEqual(
            resultado["cantidadMuestras"],
            6
        )

        self.assertEqual(
            resultado["calidad"],
            0.95
        )

        self.assertEqual(
            resultado["livenessPromedio"],
            0.99
        )

        # No devolver informacion biometrica.
        self.assertNotIn(
            "embedding",
            resultado
        )

        self.assertFalse(
            self.conexion.autocommit
        )

        self.guardar.assert_called_once_with(
            id_usuario=self.id_usuario,
            embedding=self.embedding,
            cantidad_muestras=6,
            calidad=0.95,
            conexion=self.conexion
        )

        self.conexion.commit.assert_called_once()

        self.conexion.rollback.assert_not_called()

        self.conexion.close.assert_called_once()

        self.bloqueo.release.assert_called_once()

    def test_fallo_guardado_ejecuta_rollback(self):

        self.guardar.side_effect = ValueError(
            "Perfil duplicado durante la transaccion"
        )

        with self.assertRaisesRegex(
            ValueError,
            "Perfil duplicado"
        ):

            registro._registrar_perfil_interno(
                self.id_usuario
            )

        self.conexion.commit.assert_not_called()

        self.conexion.rollback.assert_called_once()

        self.conexion.close.assert_called_once()

        self.bloqueo.release.assert_called_once()


class TestBaseRegistroInterno(unittest.TestCase):

    def test_base_configurada_incorrectamente(self):

        with (
            patch.object(
                registro,
                "get_database_config",
                return_value={
                    "database": "SIGELAB_PRODUCCION"
                }
            ),
            patch.object(
                registro,
                "probar_conexion"
            ) as probar
        ):

            with self.assertRaisesRegex(
                RuntimeError,
                "base de datos no autorizada"
            ):

                registro._verificar_entorno_pruebas()

            probar.assert_not_called()

    def test_conexion_apunta_a_otra_base(self):

        with (
            patch.object(
                registro,
                "get_database_config",
                return_value={
                    "database": "SIGELAB_TEST_FACE"
                }
            ),
            patch.object(
                registro,
                "probar_conexion",
                return_value={
                    "base_datos": "OTRA_BASE",
                    "tablas_faciales": [
                        "PerfilFacial",
                        "AuditoriaFacial",
                    ],
                }
            )
        ):

            with self.assertRaisesRegex(
                RuntimeError,
                "no corresponde"
            ):

                registro._verificar_entorno_pruebas()

    def test_faltan_tablas_faciales(self):

        with (
            patch.object(
                registro,
                "get_database_config",
                return_value={
                    "database": "SIGELAB_TEST_FACE"
                }
            ),
            patch.object(
                registro,
                "probar_conexion",
                return_value={
                    "base_datos": "SIGELAB_TEST_FACE",
                    "tablas_faciales": [
                        "PerfilFacial"
                    ],
                }
            )
        ):

            with self.assertRaisesRegex(
                RuntimeError,
                "Faltan tablas"
            ):

                registro._verificar_entorno_pruebas()


if __name__ == "__main__":
    unittest.main()
import unittest

from unittest.mock import MagicMock, patch

from app import eliminacion_facial_interna as modulo


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


class TestEliminacionFacialInterna(unittest.TestCase):

    def preparar_simulacion(
        self,
        respuestas,
        error_auditoria=False,
    ):

        cursor = MagicMock()

        cursor.fetchone.side_effect = respuestas

        def ejecutar(consulta, *parametros):

            if (
                error_auditoria
                and "INSERT INTO dbo.AuditoriaFacial"
                in consulta
            ):
                raise RuntimeError(
                    "Error simulado de auditoria"
                )

            return cursor

        cursor.execute.side_effect = ejecutar

        conexion = MagicMock()

        conexion.cursor.return_value = cursor

        return conexion, cursor

    def ejecutar_simulado(
        self,
        conexion,
        id_usuario=8,
    ):

        with patch.object(
            modulo,
            "get_database_config",
            return_value={
                "database": BASE_PRUEBAS
            },
        ), patch.object(
            modulo,
            "get_connection",
            return_value=conexion,
        ):

            return modulo._eliminar_perfil_interno(
                id_usuario
            )

    def test_eliminacion_correcta(self):

        conexion, cursor = self.preparar_simulacion(
            [
                (BASE_PRUEBAS,),
                (8,),
                (44,),
                (44,),
            ]
        )

        resultado = self.ejecutar_simulado(
            conexion
        )

        self.assertTrue(
            resultado["deleted"]
        )

        self.assertEqual(
            resultado["result"],
            "ELIMINADO"
        )

        conexion.commit.assert_called_once()

        conexion.rollback.assert_not_called()

        conexion.close.assert_called_once()

        consultas = [
            llamada.args[0]
            for llamada
            in cursor.execute.call_args_list
        ]

        self.assertTrue(
            any(
                "DELETE FROM dbo.PerfilFacial"
                in consulta
                for consulta in consultas
            )
        )

        self.assertTrue(
            any(
                "INSERT INTO dbo.AuditoriaFacial"
                in consulta
                for consulta in consultas
            )
        )

    def test_identificadores_invalidos(self):

        identificadores = [
            None,
            0,
            -1,
            True,
            "8",
            8.0,
        ]

        for identificador in identificadores:

            with self.subTest(
                identificador=identificador
            ):

                conexion = MagicMock()

                with self.assertRaises(
                    ValueError
                ):

                    self.ejecutar_simulado(
                        conexion,
                        identificador,
                    )

                conexion.cursor.assert_not_called()

    def test_configuracion_base_incorrecta(self):

        with patch.object(
            modulo,
            "get_database_config",
            return_value={
                "database": "SIGELAB"
            },
        ), patch.object(
            modulo,
            "get_connection",
        ) as obtener_conexion:

            with self.assertRaises(
                RuntimeError
            ):

                modulo._eliminar_perfil_interno(8)

            obtener_conexion.assert_not_called()

    def test_conexion_base_incorrecta(self):

        conexion, _ = self.preparar_simulacion(
            [
                ("SIGELAB",),
            ]
        )

        with self.assertRaises(
            RuntimeError
        ):

            self.ejecutar_simulado(
                conexion
            )

        conexion.commit.assert_not_called()

        conexion.rollback.assert_called_once()

        conexion.close.assert_called_once()

    def test_usuario_inexistente(self):

        conexion, _ = self.preparar_simulacion(
            [
                (BASE_PRUEBAS,),
                None,
            ]
        )

        with self.assertRaises(
            ValueError
        ):

            self.ejecutar_simulado(
                conexion
            )

        conexion.commit.assert_not_called()

        conexion.rollback.assert_called_once()

        conexion.close.assert_called_once()

    def test_perfil_inexistente(self):

        conexion, _ = self.preparar_simulacion(
            [
                (BASE_PRUEBAS,),
                (8,),
                None,
            ]
        )

        with self.assertRaises(
            ValueError
        ):

            self.ejecutar_simulado(
                conexion
            )

        conexion.commit.assert_not_called()

        conexion.rollback.assert_called_once()

        conexion.close.assert_called_once()

    def test_eliminacion_no_realizada(self):

        conexion, _ = self.preparar_simulacion(
            [
                (BASE_PRUEBAS,),
                (8,),
                (44,),
                None,
            ]
        )

        with self.assertRaises(
            RuntimeError
        ):

            self.ejecutar_simulado(
                conexion
            )

        conexion.commit.assert_not_called()

        conexion.rollback.assert_called_once()

        conexion.close.assert_called_once()

    def test_error_auditoria_rollback(self):

        conexion, _ = self.preparar_simulacion(
            [
                (BASE_PRUEBAS,),
                (8,),
                (44,),
                (44,),
            ],
            error_auditoria=True,
        )

        with self.assertRaises(
            RuntimeError
        ):

            self.ejecutar_simulado(
                conexion
            )

        conexion.commit.assert_not_called()

        conexion.rollback.assert_called_once()

        conexion.close.assert_called_once()


if __name__ == "__main__":
    unittest.main()
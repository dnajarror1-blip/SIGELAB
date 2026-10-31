"""
SIGELAB - Pruebas del registro facial protegido.

Comprueba que registrar_rostro.py:
- Utilice guardar_perfil_nuevo.
- Impida registros duplicados.
- Ejecute rollback si el registro protegido falla.

No abre la camara.
No utiliza SQL Server real.
No modifica perfiles faciales.
"""

import struct
import unittest

from unittest.mock import MagicMock, patch

from scripts.registrar_rostro import (
    guardar_perfil,
    USERNAME,
)


class TestRegistrarRostroSeguro(unittest.TestCase):

    def setUp(self):

        self.id_usuario = 1

        self.embedding = struct.pack(
            "<512f",
            *([0.01] * 512)
        )

        self.muestras = [
            object() for _ in range(6)
        ]

        self.calidades = [0.90] * 6

        self.conexion = MagicMock()
        self.conexion.autocommit = True

        self.cursor = MagicMock()

        self.conexion.cursor.return_value = self.cursor
        self.cursor.execute.return_value = self.cursor

    def simular_consultas(self, *respuestas):

        self.cursor.fetchone.side_effect = respuestas

    def test_utiliza_registro_protegido(self):

        self.simular_consultas(
            ("SIGELAB_TEST_FACE",),
            ("ACTIVO",),
            None,
        )

        with (
            patch(
                "scripts.registrar_rostro.get_connection",
                return_value=self.conexion,
            ),
            patch(
                "scripts.registrar_rostro.guardar_perfil_nuevo"
            ) as registro_protegido,
        ):

            guardar_perfil(
                self.id_usuario,
                self.embedding,
                self.muestras,
                self.calidades,
            )

            registro_protegido.assert_called_once_with(
                id_usuario=self.id_usuario,
                embedding=self.embedding,
                cantidad_muestras=6,
                calidad=0.90,
                conexion=self.conexion,
            )

        self.assertFalse(
            self.conexion.autocommit
        )

        self.conexion.commit.assert_called_once()

        self.conexion.rollback.assert_not_called()

        self.conexion.close.assert_called_once()

        # Confirmar que la consulta incluyo
        # el identificador y username esperado.
        consulta_usuario = (
            self.cursor.execute.call_args_list[1]
        )

        self.assertEqual(
            consulta_usuario.args[1],
            self.id_usuario,
        )

        self.assertEqual(
            consulta_usuario.args[2],
            USERNAME,
        )

        print(
            "OK: Se utilizo guardar_perfil_nuevo"
        )

        print(
            "OK: Transaccion confirmada correctamente"
        )

    def test_perfil_existente_bloqueado(self):

        self.simular_consultas(
            ("SIGELAB_TEST_FACE",),
            ("ACTIVO",),
            (1,),
        )

        with (
            patch(
                "scripts.registrar_rostro.get_connection",
                return_value=self.conexion,
            ),
            patch(
                "scripts.registrar_rostro.guardar_perfil_nuevo"
            ) as registro_protegido,
        ):

            with self.assertRaisesRegex(
                RuntimeError,
                "Ya existe un perfil",
            ):
                guardar_perfil(
                    self.id_usuario,
                    self.embedding,
                    self.muestras,
                    self.calidades,
                )

            registro_protegido.assert_not_called()

        self.conexion.commit.assert_not_called()

        self.conexion.rollback.assert_called_once()

        self.conexion.close.assert_called_once()

        print(
            "OK: Perfil existente bloqueado"
        )

        print(
            "OK: Rollback ejecutado"
        )

    def test_rechazo_del_registro_protegido(self):

        self.simular_consultas(
            ("SIGELAB_TEST_FACE",),
            ("ACTIVO",),
            None,
        )

        with (
            patch(
                "scripts.registrar_rostro.get_connection",
                return_value=self.conexion,
            ),
            patch(
                "scripts.registrar_rostro.guardar_perfil_nuevo",
                side_effect=ValueError(
                    "El usuario ya tiene un perfil facial"
                ),
            ) as registro_protegido,
        ):

            with self.assertRaisesRegex(
                ValueError,
                "ya tiene un perfil facial",
            ):
                guardar_perfil(
                    self.id_usuario,
                    self.embedding,
                    self.muestras,
                    self.calidades,
                )

            registro_protegido.assert_called_once()

        self.conexion.commit.assert_not_called()

        self.conexion.rollback.assert_called_once()

        self.conexion.close.assert_called_once()

        print(
            "OK: Rechazo del registro protegido respetado"
        )

        print(
            "OK: No se confirmo la transaccion"
        )


if __name__ == "__main__":
    unittest.main()
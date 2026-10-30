"""
SIGELAB - Pruebas de seguridad del registro facial.

Verifica:
- Identificadores invalidos.
- Usuarios inexistentes.
- Usuarios INACTIVOS.

Base exclusiva: SIGELAB_TEST_FACE.

Las operaciones temporales se revierten
mediante ROLLBACK.
"""

import secrets
import unittest

from uuid import uuid4

import bcrypt

from app.database import get_connection
from app.registro_facial_nuevo import guardar_perfil_nuevo


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


class TestRegistroSeguridad(unittest.TestCase):

    def setUp(self):
        self.conexion = get_connection()

        try:
            self.conexion.autocommit = False
            self.cursor = self.conexion.cursor()

            base = self.cursor.execute(
                "SELECT DB_NAME()"
            ).fetchone()[0]

            self.assertEqual(
                base,
                BASE_PRUEBAS,
                "Base de datos no autorizada"
            )

        except Exception:
            self.conexion.close()
            raise

    def tearDown(self):
        self.conexion.rollback()
        self.conexion.close()

    def test_identificadores_invalidos(self):

        valores_invalidos = [
            None,
            0,
            -1,
            True,
            "1"
        ]

        for valor in valores_invalidos:
            with self.subTest(valor=repr(valor)):

                with self.assertRaisesRegex(
                    ValueError,
                    "identificador"
                ):
                    guardar_perfil_nuevo(
                        id_usuario=valor,
                        embedding=b"PRUEBA",
                        cantidad_muestras=3,
                        calidad=0.95,
                        conexion=self.conexion
                    )

        print(
            "\nOK: Identificadores invalidos rechazados"
        )

    def test_usuario_inexistente(self):

        id_inexistente = 99999

        usuario = self.cursor.execute(
            """
            SELECT 1
            FROM dbo.Usuario
            WHERE id_usuario = ?
            """,
            id_inexistente
        ).fetchone()

        self.assertIsNone(
            usuario,
            "El identificador de prueba ya existe"
        )

        with self.assertRaisesRegex(
            ValueError,
            "no existe"
        ):
            guardar_perfil_nuevo(
                id_usuario=id_inexistente,
                embedding=b"PRUEBA",
                cantidad_muestras=3,
                calidad=0.95,
                conexion=self.conexion
            )

        print(
            "\nOK: Usuario inexistente rechazado"
        )

    def test_usuario_inactivo(self):

        username_temporal = (
            "faceid.test." + uuid4().hex[:16]
        )

        hash_temporal = bcrypt.hashpw(
            secrets.token_bytes(32),
            bcrypt.gensalt()
        ).decode("ascii")

        # Obtener rol de prueba.
        rol = self.cursor.execute(
            """
            SELECT id_rol
            FROM dbo.Rol
            WHERE nombre = ?
            """,
            "ENCARGADO"
        ).fetchone()

        self.assertIsNotNone(rol)

        # Crear usuario temporal INACTIVO.
        fila = self.cursor.execute(
            """
            INSERT INTO dbo.Usuario (
                nombre,
                apellido,
                username,
                password_hash,
                correo,
                id_rol,
                estado
            )
            OUTPUT INSERTED.id_usuario
            VALUES (
                ?, ?, ?, ?, ?, ?, 'INACTIVO'
            )
            """,
            "Prueba",
            "Inactivo",
            username_temporal,
            hash_temporal,
            username_temporal + "@sigelab.local",
            rol[0]
        ).fetchone()

        id_temporal = int(fila[0])

        # Intentar registrar perfil.
        with self.assertRaisesRegex(
            ValueError,
            "no esta activo"
        ):
            guardar_perfil_nuevo(
                id_usuario=id_temporal,
                embedding=b"PRUEBA",
                cantidad_muestras=3,
                calidad=0.95,
                conexion=self.conexion
            )

        # Confirmar ausencia de perfil.
        perfiles = self.cursor.execute(
            """
            SELECT COUNT(*)
            FROM dbo.PerfilFacial
            WHERE id_usuario = ?
            """,
            id_temporal
        ).fetchone()[0]

        self.assertEqual(perfiles, 0)

        # Confirmar ausencia de registro exitoso.
        auditorias = self.cursor.execute(
            """
            SELECT COUNT(*)
            FROM dbo.AuditoriaFacial
            WHERE id_usuario = ?
              AND accion = 'REGISTRO'
              AND resultado = 'EXITO'
            """,
            id_temporal
        ).fetchone()[0]

        self.assertEqual(auditorias, 0)

        # Revertir usuario temporal.
        self.conexion.rollback()

        usuario_restante = self.cursor.execute(
            """
            SELECT COUNT(*)
            FROM dbo.Usuario
            WHERE username = ?
            """,
            username_temporal
        ).fetchone()[0]

        self.assertEqual(usuario_restante, 0)

        print(
            "\nOK: Usuario INACTIVO rechazado"
        )

        print(
            "OK: Usuario temporal revertido"
        )


if __name__ == "__main__":
    unittest.main()
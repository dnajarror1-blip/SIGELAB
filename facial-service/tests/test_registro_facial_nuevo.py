"""
SIGELAB - Prueba del registro facial protegido.

Base autorizada: SIGELAB_TEST_FACE.

Crea un usuario temporal, registra un perfil
cifrado, comprueba la proteccion contra
duplicados y revierte toda la transaccion.

No utiliza la camara ni modifica usuarios reales.
"""

import secrets
import struct
import unittest

from uuid import uuid4

import bcrypt

from app.database import get_connection

from app.registro_facial_nuevo import (
    guardar_perfil_nuevo
)

from app.perfil_storage import (
    obtener_embedding_facial
)


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


class TestRegistroFacialNuevo(unittest.TestCase):

    def test_registro_duplicado_y_rollback(self):

        username_temporal = (
            "faceid.test." + uuid4().hex[:16]
        )

        hash_temporal = bcrypt.hashpw(
            secrets.token_bytes(32),
            bcrypt.gensalt()
        ).decode("ascii")

        conexion = get_connection()
        conexion.autocommit = False

        id_temporal = None

        try:
            cursor = conexion.cursor()

            # 1. Verificar la base autorizada.
            base = cursor.execute(
                "SELECT DB_NAME()"
            ).fetchone()[0]

            self.assertEqual(
                base,
                BASE_PRUEBAS,
                "Base de datos no autorizada"
            )

            # 2. Obtener el rol de prueba.
            rol = cursor.execute(
                """
                SELECT id_rol
                FROM dbo.Rol
                WHERE nombre = ?
                """,
                "ENCARGADO"
            ).fetchone()

            self.assertIsNotNone(
                rol,
                "No existe el rol ENCARGADO"
            )

            # 3. Crear usuario temporal.
            fila = cursor.execute(
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
                    ?, ?, ?, ?, ?, ?, 'ACTIVO'
                )
                """,
                "Prueba",
                "Registro",
                username_temporal,
                hash_temporal,
                username_temporal + "@sigelab.local",
                rol[0]
            ).fetchone()

            id_temporal = int(fila[0])

            self.assertGreater(
                id_temporal,
                0
            )

            # 4. Crear embedding artificial.
            embedding_original = struct.pack(
                "<512f",
                *([0.01] * 512)
            )

            # 5. Registrar perfil nuevo.
            resultado = guardar_perfil_nuevo(
                id_usuario=id_temporal,
                embedding=embedding_original,
                cantidad_muestras=3,
                calidad=0.95,
                conexion=conexion
            )

            self.assertTrue(resultado)

            print(
                "\nOK: Perfil temporal registrado"
            )

            # 6. Comprobar descifrado.
            recuperado = obtener_embedding_facial(
                id_temporal,
                conexion=conexion
            )

            self.assertEqual(
                embedding_original,
                recuperado
            )

            # 7. Comprobar cifrado almacenado.
            fila_cifrada = cursor.execute(
                """
                SELECT embedding
                FROM dbo.PerfilFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()

            self.assertIsNotNone(
                fila_cifrada
            )

            cifrado_original = bytes(
                fila_cifrada[0]
            )

            self.assertNotEqual(
                embedding_original,
                cifrado_original
            )

            self.assertTrue(
                cifrado_original.startswith(b"SF1")
            )

            print(
                "OK: Embedding cifrado y descifrado"
            )

            # 8. Verificar la auditoria.
            auditorias_antes = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.AuditoriaFacial
                WHERE id_usuario = ?
                  AND accion = 'REGISTRO'
                  AND resultado = 'EXITO'
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                auditorias_antes,
                1
            )

            # 9. Intentar registrar nuevamente.
            with self.assertRaisesRegex(
                ValueError,
                "ya tiene un perfil facial"
            ):
                guardar_perfil_nuevo(
                    id_usuario=id_temporal,
                    embedding=embedding_original,
                    cantidad_muestras=3,
                    calidad=0.95,
                    conexion=conexion
                )

            print(
                "OK: Registro duplicado bloqueado"
            )

            # 10. Confirmar que no cambio el perfil.
            fila_despues = cursor.execute(
                """
                SELECT embedding
                FROM dbo.PerfilFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()

            self.assertIsNotNone(
                fila_despues
            )

            self.assertEqual(
                cifrado_original,
                bytes(fila_despues[0])
            )

            # 11. Confirmar que sigue una auditoria.
            auditorias_despues = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.AuditoriaFacial
                WHERE id_usuario = ?
                  AND accion = 'REGISTRO'
                  AND resultado = 'EXITO'
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                auditorias_despues,
                1
            )

            print(
                "OK: Perfil original y auditoria intactos"
            )

        finally:
            # Revertir siempre la prueba.
            conexion.rollback()
            conexion.close()

            print(
                "OK: ROLLBACK ejecutado"
            )

        # 12. Verificar el resultado con otra conexion.
        verificacion = get_connection()

        try:
            cursor = verificacion.cursor()

            base = cursor.execute(
                "SELECT DB_NAME()"
            ).fetchone()[0]

            self.assertEqual(
                base,
                BASE_PRUEBAS
            )

            usuarios = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.Usuario
                WHERE username = ?
                """,
                username_temporal
            ).fetchone()[0]

            self.assertEqual(
                usuarios,
                0
            )

            perfiles = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.PerfilFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                perfiles,
                0
            )

            auditorias = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.AuditoriaFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                auditorias,
                0
            )

            print(
                "OK: Usuario temporal eliminado"
            )

            print(
                "OK: Perfil temporal eliminado"
            )

            print(
                "OK: Auditoria temporal eliminada"
            )

        finally:
            verificacion.close()


if __name__ == "__main__":
    unittest.main()
"""
Prueba de almacenamiento facial en SIGELAB_TEST_FACE.

Utiliza un usuario temporal y revierte la transaccion.
No modifica el perfil facial de Wilson.
"""

import secrets
import struct
import unittest
from uuid import uuid4

import bcrypt

from app.database import get_connection
from app.perfil_storage import (
    guardar_perfil_facial,
    obtener_embedding_facial
)


class TestPerfilStorage(unittest.TestCase):

    def test_almacenamiento_con_rollback(self):

        username_temporal = (
            "faceid.test." + uuid4().hex[:16]
        )

        hash_temporal = bcrypt.hashpw(
            secrets.token_bytes(32),
            bcrypt.gensalt()
        ).decode("ascii")

        conexion = get_connection()
        conexion.autocommit = False

        try:
            cursor = conexion.cursor()

            # 1. Verificar la base autorizada.
            base_actual = cursor.execute(
                "SELECT DB_NAME()"
            ).fetchone()[0]

            self.assertEqual(
                base_actual,
                "SIGELAB_TEST_FACE",
                "La prueba solo permite SIGELAB_TEST_FACE"
            )

            # 2. Obtener el rol ENCARGADO.
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
                "Rollback",
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

            # 4. Comprobar que no tiene perfil.
            perfil_previo = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.PerfilFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                perfil_previo,
                0
            )

            # 5. Crear embedding artificial.
            embedding_original = struct.pack(
                "<512f",
                *([0.01] * 512)
            )

            # 6. Guardar perfil dentro de la transaccion.
            resultado = guardar_perfil_facial(
                id_usuario=id_temporal,
                embedding=embedding_original,
                cantidad_muestras=3,
                calidad=0.95,
                conexion=conexion
            )

            self.assertTrue(resultado)

            # 7. Recuperar embedding.
            embedding_recuperado = obtener_embedding_facial(
                id_temporal,
                conexion=conexion
            )

            self.assertEqual(
                embedding_original,
                embedding_recuperado
            )

            # 8. Verificar cifrado en SQL.
            cifrado_sql = cursor.execute(
                """
                SELECT embedding
                FROM dbo.PerfilFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()

            self.assertIsNotNone(cifrado_sql)

            cifrado = bytes(cifrado_sql[0])

            self.assertNotEqual(
                embedding_original,
                cifrado
            )

            self.assertTrue(
                cifrado.startswith(b"SF1")
            )

            # 9. Verificar auditoria temporal.
            auditorias = cursor.execute(
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
                auditorias,
                1
            )

            print(
                "\nOK: Usuario temporal y perfil cifrado validados"
            )

            print(
                "OK: Embedding descifrado y auditoria verificados"
            )

        finally:
            # Revertir la transaccion incluso si falla.
            try:
                conexion.rollback()

                print(
                    "OK: ROLLBACK aplicado a la transaccion de prueba"
                )

            finally:
                conexion.close()

        # 10. Verificar desde otra conexion que
        #     el usuario temporal no permanece.
        verificacion = get_connection()

        try:
            self.assertEqual(
                verificacion.execute(
                    "SELECT DB_NAME()"
                ).fetchone()[0],
                "SIGELAB_TEST_FACE"
            )

            restante = verificacion.execute(
                """
                SELECT COUNT(*)
                FROM dbo.Usuario
                WHERE username = ?
                """,
                username_temporal
            ).fetchone()[0]

            self.assertEqual(
                restante,
                0,
                "El usuario temporal no fue revertido"
            )

            print(
                "OK: Usuario temporal ausente despues del ROLLBACK"
            )

        finally:
            verificacion.close()


if __name__ == "__main__":
    unittest.main()

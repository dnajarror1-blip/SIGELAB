"""
SIGELAB - Prueba transaccional de eliminacion facial.

Utiliza exclusivamente SIGELAB_TEST_FACE.

Crea un usuario temporal y registra un perfil
cifrado dentro de una transaccion.

Despues elimina el perfil y comprueba
que la auditoria se conserva.

Finalmente ejecuta ROLLBACK y verifica
que no quedaron registros temporales.

No utiliza la camara.
No modifica usuarios existentes.
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

from app.eliminacion_facial_interna import (
    _eliminar_perfil_interno
)


BASE_PRUEBAS = "SIGELAB_TEST_FACE"


class TestEliminacionFacialSqlRollback(
    unittest.TestCase
):

    def test_eliminacion_real_con_rollback(self):

        username_temporal = (
            "faceid.delete." + uuid4().hex[:16]
        )

        hash_temporal = bcrypt.hashpw(
            secrets.token_bytes(32),
            bcrypt.gensalt()
        ).decode("ascii")

        conexion = get_connection()
        id_temporal = None

        try:

            conexion.autocommit = False

            cursor = conexion.cursor()

            # --------------------------------------
            # 1. Verificar base de datos
            # --------------------------------------

            base = cursor.execute(
                "SELECT DB_NAME()"
            ).fetchone()[0]

            self.assertEqual(
                base,
                BASE_PRUEBAS,
                "Base de datos no autorizada"
            )

            print(
                "\nBASE VERIFICADA:",
                base
            )

            # --------------------------------------
            # 2. Obtener rol
            # --------------------------------------

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

            # --------------------------------------
            # 3. Crear usuario temporal
            # --------------------------------------

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
                "Eliminacion",
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

            print(
                "OK: Usuario temporal creado"
            )

            # --------------------------------------
            # 4. Preparar embedding artificial
            # --------------------------------------

            embedding = struct.pack(
                "<512f",
                *([0.01] * 512)
            )

            # --------------------------------------
            # 5. Registrar perfil cifrado
            # --------------------------------------

            guardar_perfil_nuevo(
                id_usuario=id_temporal,
                embedding=embedding,
                cantidad_muestras=6,
                calidad=0.95,
                conexion=conexion
            )

            perfiles_antes = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.PerfilFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                perfiles_antes,
                1
            )

            print(
                "OK: Perfil facial cifrado registrado"
            )

            # --------------------------------------
            # 6. Eliminar perfil
            # --------------------------------------

            resultado = _eliminar_perfil_interno(
                id_temporal,
                conexion=conexion
            )

            self.assertTrue(
                resultado["deleted"]
            )

            self.assertEqual(
                resultado["result"],
                "ELIMINADO"
            )

            print(
                "OK: Eliminacion facial ejecutada"
            )

            # --------------------------------------
            # 7. Verificar ausencia de perfil
            # --------------------------------------

            perfiles_despues = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.PerfilFacial
                WHERE id_usuario = ?
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                perfiles_despues,
                0
            )

            print(
                "OK: Perfil eliminado dentro "
                "de la transaccion"
            )

            # --------------------------------------
            # 8. Verificar auditoria de eliminacion
            # --------------------------------------

            auditorias_eliminacion = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.AuditoriaFacial
                WHERE id_usuario = ?
                  AND accion = 'ELIMINACION'
                  AND resultado = 'COMPLETADO'
                """,
                id_temporal
            ).fetchone()[0]

            self.assertEqual(
                auditorias_eliminacion,
                1
            )

            print(
                "OK: Auditoria de eliminacion registrada"
            )

            # --------------------------------------
            # 9. Verificar auditoria de registro
            # --------------------------------------

            auditorias_registro = cursor.execute(
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
                auditorias_registro,
                1
            )

            print(
                "OK: Auditoria historica conservada"
            )

        finally:

            # --------------------------------------
            # 10. Revertir siempre
            # --------------------------------------

            try:
                conexion.rollback()

                print(
                    "OK: ROLLBACK ejecutado"
                )

            finally:
                conexion.close()

        # ------------------------------------------
        # 11. Verificar con otra conexion
        # ------------------------------------------

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

            print(
                "OK: Usuario temporal ausente"
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

            print(
                "OK: Perfil temporal ausente"
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
                "OK: Auditorias temporales ausentes"
            )

        finally:

            verificacion.close()


if __name__ == "__main__":
    unittest.main()
"""
SIGELAB - Prueba transaccional de auditoria facial.

Verifica que los rechazos por cambio de estado
puedan almacenarse en SQL Server.

Solo permite SIGELAB_TEST_FACE.

Intercepta commit() y close() para controlar
la transaccion desde la prueba.

No modifica usuarios ni perfiles faciales.
No deja registros de auditoria permanentes.
"""

import unittest
import os
from unittest.mock import patch

from app.database import get_connection
from app.main import registrar_rechazo_por_estado


BASE_AUTORIZADA = "SIGELAB_TEST_FACE"


class ConexionSinCommit:
    """
    Adaptador que permite ejecutar SQL real,
    pero impide que el codigo probado confirme
    o cierre la transaccion.
    """

    def __init__(self, conexion):
        self.conexion = conexion
        self.commits_interceptados = 0
        self.cierres_interceptados = 0

    def cursor(self):
        return self.conexion.cursor()

    def commit(self):
        self.commits_interceptados += 1

    def rollback(self):
        self.conexion.rollback()

    def close(self):
        self.cierres_interceptados += 1


@unittest.skipUnless(os.getenv("SIGELAB_RUN_SQL_TESTS") == "1", "SQL real requiere autorización explícita")
class TestAuditoriaRechazoRollback(unittest.TestCase):

    def test_rechazos_con_rollback(self):

        conexion = get_connection()
        conexion.autocommit = False

        id_usuario = None
        auditorias_antes = None

        try:
            cursor = conexion.cursor()

            # 1. Comprobar la base autorizada.
            base = cursor.execute(
                "SELECT DB_NAME()"
            ).fetchone()[0]

            self.assertEqual(
                base,
                BASE_AUTORIZADA,
                "Base de datos no autorizada",
            )

            print("BASE:", base)

            # 2. Seleccionar un usuario existente.
            fila = cursor.execute(
                """
                SELECT TOP (1)
                    id_usuario
                FROM dbo.Usuario
                ORDER BY id_usuario
                """
            ).fetchone()

            if fila is None:
                self.skipTest(
                    "No existen usuarios de prueba"
                )

            id_usuario = int(fila[0])

            print(
                "USUARIO EXISTENTE:",
                id_usuario,
            )

            # 3. Contar auditorias anteriores.
            auditorias_antes = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.AuditoriaFacial
                WHERE id_usuario = ?
                  AND resultado = 'RECHAZADO_ESTADO'
                """,
                id_usuario,
            ).fetchone()[0]

            print(
                "AUDITORIAS ANTES:",
                auditorias_antes,
            )

            # 4. Interceptar commit y close.
            conexion_controlada = ConexionSinCommit(
                conexion
            )

            with patch(
                "app.administracion_repository.get_connection",
                return_value=conexion_controlada,
            ):

                # Rechazo por cuenta desactivada.
                registrar_rechazo_por_estado(
                    id_usuario,
                    "USUARIO_DESACTIVADO",
                )

                # Rechazo por perfil desactivado.
                registrar_rechazo_por_estado(
                    id_usuario,
                    "PERFIL_DESACTIVADO",
                )

            # 5. Confirmar que se intentaron
            # dos escrituras y dos commits.
            self.assertEqual(
                conexion_controlada.commits_interceptados,
                2,
            )

            self.assertEqual(
                conexion_controlada.cierres_interceptados,
                2,
            )

            print(
                "COMMITS INTERCEPTADOS:",
                conexion_controlada.commits_interceptados,
            )

            # 6. Los registros deben ser visibles
            # dentro de la misma transaccion.
            auditorias_durante = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.AuditoriaFacial
                WHERE id_usuario = ?
                  AND resultado = 'RECHAZADO_ESTADO'
                """,
                id_usuario,
            ).fetchone()[0]

            self.assertEqual(
                auditorias_durante,
                auditorias_antes + 2,
            )

            print(
                "AUDITORIAS DENTRO DE TRANSACCION:",
                auditorias_durante,
            )

            print(
                "OK: SQL Server acepto ambos rechazos"
            )

        finally:

            # 7. Revertir siempre la transaccion.
            conexion.rollback()
            conexion.close()

            print(
                "ROLLBACK: EJECUTADO"
            )

        # 8. Verificar desde una conexion nueva.
        verificacion = get_connection()

        try:
            cursor = verificacion.cursor()

            base = cursor.execute(
                "SELECT DB_NAME()"
            ).fetchone()[0]

            self.assertEqual(
                base,
                BASE_AUTORIZADA,
            )

            auditorias_despues = cursor.execute(
                """
                SELECT COUNT(*)
                FROM dbo.AuditoriaFacial
                WHERE id_usuario = ?
                  AND resultado = 'RECHAZADO_ESTADO'
                """,
                id_usuario,
            ).fetchone()[0]

            self.assertEqual(
                auditorias_despues,
                auditorias_antes,
                "Quedaron auditorias permanentes",
            )

            print(
                "AUDITORIAS DESPUES:",
                auditorias_despues,
            )

            print(
                "OK: No quedaron auditorias nuevas"
            )

        finally:
            verificacion.close()

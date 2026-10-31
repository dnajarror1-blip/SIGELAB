import re
from pathlib import Path

from app.database import get_connection


BASE_DESTINO = "SIGELAB_TEST_FACE"

RUTA_SQL = (
    Path(__file__).resolve().parents[2]
    / "database"
    / "sigelab.sql"
)


def preparar_base_face():
    print("SIGELAB - Preparacion de base de pruebas FaceID")
    print("Base de destino:", BASE_DESTINO)

    if not RUTA_SQL.is_file():
        raise FileNotFoundError(
            f"No se encontro el archivo SQL: {RUTA_SQL}"
        )

    contenido = RUTA_SQL.read_text(encoding="utf-8-sig")

    lotes = re.split(
        r"(?im)^[ \t]*GO[ \t]*(?:--[^\r\n]*)?\r?$",
        contenido,
    )

    if len(lotes) < 3:
        raise RuntimeError(
            "La estructura del archivo SQL no es la esperada."
        )

    # Los primeros dos lotes pertenecen a la base SIGELAB original.
    # No deben ejecutarse.
    if not re.search(
        r"CREATE\s+DATABASE\s+SIGELAB\b",
        lotes[0],
        re.IGNORECASE,
    ):
        raise RuntimeError(
            "No se reconocio el encabezado de creacion de SIGELAB."
        )

    if not re.search(
        r"\bUSE\s+SIGELAB\s*;",
        lotes[1],
        re.IGNORECASE,
    ):
        raise RuntimeError(
            "No se reconocio el encabezado USE SIGELAB."
        )

    lotes_tablas = [
        lote.strip()
        for lote in lotes[2:]
        if lote.strip()
    ]

    # Evitar instrucciones que cambien de base o eliminen datos.
    instrucciones_no_permitidas = (
        r"(?im)^\s*(?:"
        r"USE\b|DROP\b|TRUNCATE\b|DELETE\b|UPDATE\b|MERGE\b|"
        r"CREATE\s+DATABASE\b|ALTER\s+DATABASE\b"
        r")"
    )

    for lote in lotes_tablas:
        if re.search(instrucciones_no_permitidas, lote):
            raise RuntimeError(
                "Se encontro una instruccion no permitida. "
                "No se ejecutara el archivo."
            )

    conexion = get_connection()

    try:
        conexion.autocommit = False
        cursor = conexion.cursor()

        existe = cursor.execute(
            "SELECT DB_ID(?)",
            BASE_DESTINO,
        ).fetchone()[0]

        if existe is None:
            raise RuntimeError(
                "La base SIGELAB_TEST_FACE no existe."
            )

        cursor.execute("USE [SIGELAB_TEST_FACE]")

        base_actual = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()[0]

        if base_actual != BASE_DESTINO:
            raise RuntimeError(
                "La conexion no apunta a la base de FaceID."
            )

        print("Conexion verificada:", base_actual)

        for numero, lote in enumerate(lotes_tablas, start=1):
            cursor.execute(lote)
            print(f"Lote de tablas {numero}: OK")

        # Insertar los roles solamente si no existen.
        cursor.execute(
            """
            IF NOT EXISTS (
                SELECT 1 FROM dbo.Rol WHERE nombre = N'ADMIN'
            )
                INSERT INTO dbo.Rol (nombre)
                VALUES (N'ADMIN')
            """
        )

        cursor.execute(
            """
            IF NOT EXISTS (
                SELECT 1 FROM dbo.Rol WHERE nombre = N'ENCARGADO'
            )
                INSERT INTO dbo.Rol (nombre)
                VALUES (N'ENCARGADO')
            """
        )

        tablas_requeridas = [
            "Rol",
            "Usuario",
            "PerfilFacial",
            "AuditoriaFacial",
        ]

        for tabla in tablas_requeridas:
            existe_tabla = cursor.execute(
                """
                SELECT COUNT(*)
                FROM sys.tables
                WHERE name = ?
                  AND schema_id = SCHEMA_ID('dbo')
                """,
                tabla,
            ).fetchone()[0]

            if existe_tabla != 1:
                raise RuntimeError(
                    f"No se encontro la tabla requerida: {tabla}"
                )

        conexion.commit()

        print()
        print("BASE DE DATOS PREPARADA CORRECTAMENTE")
        print("Base:", BASE_DESTINO)
        print("Tablas principales: OK")
        print("Roles: OK")

    except Exception:
        conexion.rollback()
        print("ERROR: Se revirtieron los cambios de esta ejecucion.")
        raise

    finally:
        conexion.close()


if __name__ == "__main__":
    preparar_base_face()
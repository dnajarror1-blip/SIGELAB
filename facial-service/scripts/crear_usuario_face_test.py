from getpass import getpass

import bcrypt

from app.database import get_connection


BASE_PERMITIDA = "SIGELAB_TEST_FACE"

NOMBRE = "Wilson"
APELLIDO = "Integrante08"

USERNAME = "faceid.integrante08"
CORREO = "faceid.integrante08@sigelab.local"

ROL = "ENCARGADO"
ESTADO = "ACTIVO"


def crear_usuario():
    print("SIGELAB - Registro de usuario FaceID")
    print("Base autorizada:", BASE_PERMITIDA)
    print("Usuario:", USERNAME)

    conexion = get_connection()

    try:
        cursor = conexion.cursor()

        base_actual = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()[0]

        if base_actual != BASE_PERMITIDA:
            raise RuntimeError(
                "Registro bloqueado: la conexion no utiliza "
                "SIGELAB_TEST_FACE."
            )

        # Verificar si el usuario ya existe.
        existente = cursor.execute(
            """
            SELECT id_usuario
            FROM dbo.Usuario
            WHERE username = ?
            """,
            USERNAME,
        ).fetchone()

        if existente is not None:
            print()
            print("El usuario ya existe.")
            print("ID asignado:", existente[0])
            print("No se realizaron cambios.")
            return

        # Obtener el rol autorizado.
        fila_rol = cursor.execute(
            """
            SELECT id_rol
            FROM dbo.Rol
            WHERE nombre = ?
            """,
            ROL,
        ).fetchone()

        if fila_rol is None:
            raise RuntimeError(
                "No se encontro el rol ENCARGADO."
            )

        id_rol = fila_rol[0]

        print()
        print("Configura la contrasena de este usuario.")
        print("La contrasena no aparecera en pantalla.")

        password = getpass(
            "Nueva contrasena: "
        )

        confirmacion = getpass(
            "Confirmar contrasena: "
        )

        if password != confirmacion:
            raise ValueError(
                "Las contrasenas no coinciden."
            )

        password_bytes = password.encode("utf-8")

        if len(password_bytes) < 12:
            raise ValueError(
                "La contrasena debe tener al menos "
                "12 bytes."
            )

        if len(password_bytes) > 72:
            raise ValueError(
                "La contrasena no debe superar "
                "72 bytes en UTF-8."
            )

        # BCrypt compatible con jBCrypt de Java.
        password_hash = bcrypt.hashpw(
            password_bytes,
            bcrypt.gensalt(
                rounds=12,
                prefix=b"2a"
            )
        ).decode("ascii")

        if not bcrypt.checkpw(
            password_bytes,
            password_hash.encode("ascii")
        ):
            raise RuntimeError(
                "No se pudo validar el hash generado."
            )

        # Registrar exclusivamente en SIGELAB_TEST_FACE.
        cursor.execute(
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
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            NOMBRE,
            APELLIDO,
            USERNAME,
            password_hash,
            CORREO,
            id_rol,
            ESTADO,
        )

        id_generado = cursor.execute(
            """
            SELECT id_usuario
            FROM dbo.Usuario
            WHERE username = ?
            """,
            USERNAME,
        ).fetchone()[0]

        conexion.commit()

        print()
        print("USUARIO FACEID CREADO CORRECTAMENTE")
        print("ID interno:", id_generado)
        print("Nombre:", NOMBRE, APELLIDO)
        print("Username:", USERNAME)
        print("Correo:", CORREO)
        print("Rol:", ROL)
        print("Estado:", ESTADO)
        print("Contrasena: almacenada mediante BCrypt")

    except Exception:
        conexion.rollback()
        raise

    finally:
        conexion.close()


if __name__ == "__main__":
    crear_usuario()
import pyodbc

from .config import get_database_config


def proteger_valor_odbc(valor):
    """
    Protege los valores utilizados en una cadena ODBC.

    Permite contrasenas que contienen caracteres
    especiales, como puntos y comas o llaves.
    """
    return "{" + str(valor).replace("}", "}}") + "}"


def convertir_opcion_odbc(valor):
    """
    Convierte valores booleanos de configuracion
    al formato yes/no utilizado por ODBC.
    """
    if isinstance(valor, bool):
        return "yes" if valor else "no"

    texto = str(valor).strip().lower()

    if texto in ("true", "yes", "1", "si"):
        return "yes"

    if texto in ("false", "no", "0"):
        return "no"

    raise ValueError(
        "Valor de configuracion ODBC no reconocido."
    )


def get_connection():
    """
    Establece una conexion con Microsoft SQL Server
    utilizando ODBC Driver 18.

    Los parametros se obtienen desde app.config.
    No se almacenan credenciales en este archivo.
    """
    config = get_database_config()

    host = config["host"]
    port = config["port"]
    database = config["database"]
    username = config["username"]
    password = config["password"]

    encrypt = convertir_opcion_odbc(
        config["encrypt"]
    )

    trust_certificate = convertir_opcion_odbc(
        config["trust_certificate"]
    )

    connection_string = (
        "DRIVER={ODBC Driver 18 for SQL Server};"
        f"SERVER={host},{port};"
        f"DATABASE={proteger_valor_odbc(database)};"
        f"UID={proteger_valor_odbc(username)};"
        f"PWD={proteger_valor_odbc(password)};"
        f"Encrypt={encrypt};"
        f"TrustServerCertificate={trust_certificate};"
    )

    return pyodbc.connect(
        connection_string,
        timeout=5
    )


def probar_conexion():
    """
    Comprueba la conexion con SQL Server y
    verifica las tablas del modulo facial.

    Esta prueba es exclusivamente de lectura.

    Bases de pruebas permitidas:
    - SIGELAB_TEST_MAINT
    - SIGELAB_TEST_FACE

    No permite ejecutar el diagnostico
    sobre la base principal SIGELAB.
    """
    config = get_database_config()

    bases_permitidas = {
        "SIGELAB_TEST_MAINT",
        "SIGELAB_TEST_FACE"
    }

    if config["database"] not in bases_permitidas:
        raise RuntimeError(
            "Prueba bloqueada: solamente se permiten "
            "SIGELAB_TEST_MAINT y SIGELAB_TEST_FACE."
        )

    conexion = get_connection()

    try:
        cursor = conexion.cursor()

        # Identificar la base de datos real.
        cursor.execute("SELECT DB_NAME()")

        base_actual = cursor.fetchone()[0]

        # Verificar que coincida con la configuracion.
        if base_actual != config["database"]:
            raise RuntimeError(
                "La base de datos conectada no coincide "
                "con la configuracion esperada."
            )

        # Verificar las tablas del reconocimiento facial.
        cursor.execute(
            """
            SELECT name
            FROM sys.tables
            WHERE schema_id = SCHEMA_ID('dbo')
              AND name IN (
                  'PerfilFacial',
                  'AuditoriaFacial'
              )
            ORDER BY name
            """
        )

        tablas = [
            fila[0]
            for fila in cursor.fetchall()
        ]

        # Verificar que ambas tablas existan.
        tablas_requeridas = {
            "PerfilFacial",
            "AuditoriaFacial"
        }

        if not tablas_requeridas.issubset(set(tablas)):
            raise RuntimeError(
                "La base de datos no contiene todas "
                "las tablas necesarias para FaceID."
            )

        return {
            "conexion": "OK",
            "base_datos": base_actual,
            "tablas_faciales": tablas
        }

    finally:
        conexion.close()
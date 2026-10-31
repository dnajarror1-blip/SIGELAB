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
        f"DRIVER={proteger_valor_odbc(config['driver'])};"
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

def comprobar_esquema():
    """Diagnóstico operativo SELECT, separado de las restricciones de pruebas."""
    conexion = get_connection()
    try:
        verificar_base_configurada(conexion)
        cursor = conexion.cursor()
        try:
            # Valida tablas, columnas y relaciones reales sin leer biometría ni contraseñas.
            cursor.execute("""
                SELECT TOP (0) u.id_usuario, u.username, u.estado, r.nombre,
                    p.id_perfil_facial, p.embedding, p.modelo, p.dimension,
                    p.cantidad_muestras, p.quality_score, p.fecha_registro,
                    p.fecha_actualizacion, p.activo,
                    a.id_usuario, a.fecha_hora, a.accion, a.resultado,
                    a.score, a.threshold, a.detalle, a.dispositivo
                FROM dbo.Usuario u JOIN dbo.Rol r ON r.id_rol = u.id_rol
                LEFT JOIN dbo.PerfilFacial p ON p.id_usuario = u.id_usuario
                LEFT JOIN dbo.AuditoriaFacial a ON a.id_usuario = u.id_usuario
            """)
        finally:
            cursor.close()
        cursor = conexion.cursor()
        try:
            # El perfil canónico debe estar protegido incluso ante procesos concurrentes.
            unique = cursor.execute("""
                SELECT i.index_id FROM sys.indexes i
                JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                WHERE i.object_id = OBJECT_ID('dbo.PerfilFacial') AND i.is_unique = 1
                  AND i.has_filter = 0 AND i.is_disabled = 0 AND ic.key_ordinal > 0
                GROUP BY i.index_id HAVING COUNT(*) = 1 AND MAX(c.name) = 'id_usuario'
            """).fetchone()
            if unique is None:
                raise RuntimeError("Falta unicidad del perfil por usuario")
            rows = cursor.execute("""
                SELECT DISTINCT OBJECT_NAME(f.parent_object_id)
                FROM sys.foreign_key_columns f
                JOIN sys.foreign_keys fk ON fk.object_id = f.constraint_object_id
                JOIN sys.columns p ON p.object_id = f.parent_object_id AND p.column_id = f.parent_column_id
                JOIN sys.columns r ON r.object_id = f.referenced_object_id AND r.column_id = f.referenced_column_id
                WHERE f.referenced_object_id = OBJECT_ID('dbo.Usuario')
                  AND f.parent_object_id IN (OBJECT_ID('dbo.PerfilFacial'), OBJECT_ID('dbo.AuditoriaFacial'))
                  AND p.name = 'id_usuario' AND r.name = 'id_usuario' AND fk.is_disabled = 0
            """).fetchall()
            if {row[0] for row in rows} != {"PerfilFacial", "AuditoriaFacial"}:
                raise RuntimeError("Faltan relaciones faciales con Usuario")
        finally:
            cursor.close()
        return {"base_datos": get_database_config()["database"],
                "tablas_faciales": ["PerfilFacial", "AuditoriaFacial"]}
    finally:
        conexion.close()


def verificar_base_configurada(conexion):
    cursor = conexion.cursor()
    try:
        row = cursor.execute("SELECT DB_NAME()").fetchone()
        if row is None or row[0] != get_database_config()["database"]:
            raise RuntimeError("La conexión no corresponde a la base configurada")
    finally:
        cursor.close()

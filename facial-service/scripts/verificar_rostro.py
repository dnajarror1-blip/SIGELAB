from app.database import get_connection
from app.facial_repository import consultar_estado_facial
from app.perfil_storage import (
    obtener_embedding_facial,
    validar_embedding,
)
from app.facial_matching import calcular_similitud

from scripts.registrar_rostro import cargar_modelo
from scripts.diagnostico_embedding import capturar_rostro
from scripts.probar_similitud_real import obtener_embedding


BASE_PERMITIDA = "SIGELAB_TEST_FACE"
USERNAME = "faceid.integrante08"


def consultar_usuario():
    conexion = get_connection()

    try:
        cursor = conexion.cursor()

        base_actual = cursor.execute(
            "SELECT DB_NAME()"
        ).fetchone()[0]

        if base_actual != BASE_PERMITIDA:
            raise RuntimeError(
                "Verificacion bloqueada: base incorrecta"
            )

        usuario = cursor.execute(
            """
            SELECT id_usuario, estado
            FROM dbo.Usuario
            WHERE username = ?
            """,
            USERNAME,
        ).fetchone()

        if usuario is None:
            raise RuntimeError(
                "Usuario no encontrado"
            )

        if str(usuario[1]).strip().upper() != "ACTIVO":
            raise RuntimeError(
                "El usuario no esta activo"
            )

        return int(usuario[0])

    finally:
        conexion.close()


def verificar():
    print("SIGELAB - Verificacion facial")
    print("Usuario:", USERNAME)

    id_usuario = consultar_usuario()

    estado = consultar_estado_facial(
        id_usuario
    )

    if not estado["perfil_activo"]:
        raise RuntimeError(
            "El usuario no tiene un perfil facial activo"
        )

    print("Perfil facial encontrado.")

    embedding_registrado = obtener_embedding_facial(
        id_usuario
    )

    validar_embedding(
        embedding_registrado
    )

    print(
        "Embedding recuperado y descifrado correctamente."
    )

    print()
    print("Ahora realizaremos una nueva captura.")

    imagen = capturar_rostro()

    if imagen is None:
        print("Verificacion cancelada.")
        return

    analizador = cargar_modelo()

    embedding_actual = obtener_embedding(
        analizador,
        imagen
    )

    similitud = calcular_similitud(
        embedding_registrado,
        embedding_actual
    )

    print()
    print("RESULTADO DE COMPARACION FACIAL")
    print("Usuario:", USERNAME)
    print("Dimensiones:", 512)
    print(f"Similitud del coseno: {similitud:.4f}")
    print()
    print(
        "Comparacion completada correctamente."
    )
    print(
        "Esta prueba no concede acceso ni aplica "
        "un umbral de autenticacion."
    )
    print(
        "No se guardaron nuevas imagenes ni embeddings."
    )


if __name__ == "__main__":
    verificar()
"""
SIGELAB - Pruebas de seguridad del cifrado facial.

Comprueba:
- Cifrado y descifrado AES-GCM.
- Nonces diferentes.
- Deteccion de datos manipulados.
- Asociacion del embedding al usuario.
- Validacion de claves.
- Validacion de identificadores.

Utiliza una clave temporal generada en memoria.

No utiliza la clave real.
No abre la camara.
No consulta SQL Server.
No modifica perfiles faciales.
"""

import base64
import os
import struct
import unittest

from unittest.mock import patch

from app.crypto import (
    PREFIJO,
    TAMANO_NONCE,
    cifrar_embedding,
    descifrar_embedding,
)


class TestCryptoSeguridad(unittest.TestCase):

    def setUp(self):

        # Embedding artificial de 512 dimensiones.
        self.embedding = struct.pack(
            "<512f",
            *([0.01] * 512)
        )

        # Generar una clave temporal de 256 bits.
        clave_temporal = base64.b64encode(
            os.urandom(32)
        ).decode("ascii")

        # Reemplazar temporalmente la variable.
        self.parche_entorno = patch.dict(
            os.environ,
            {
                "FACIAL_ENCRYPTION_KEY": clave_temporal
            },
        )

        self.parche_entorno.start()

        # Restaurar el entorno al terminar.
        self.addCleanup(
            self.parche_entorno.stop
        )

    def test_cifrado_y_descifrado(self):

        cifrado = cifrar_embedding(
            1,
            self.embedding,
        )

        self.assertTrue(
            cifrado.startswith(PREFIJO)
        )

        self.assertNotEqual(
            cifrado,
            self.embedding,
        )

        longitud_esperada = (
            len(PREFIJO)
            + TAMANO_NONCE
            + len(self.embedding)
            + 16
        )

        self.assertEqual(
            len(cifrado),
            longitud_esperada,
        )

        recuperado = descifrar_embedding(
            1,
            cifrado,
        )

        self.assertEqual(
            recuperado,
            self.embedding,
        )

        print(
            "OK: Cifrado y descifrado correctos"
        )

    def test_nonce_diferente(self):

        cifrado_1 = cifrar_embedding(
            1,
            self.embedding,
        )

        cifrado_2 = cifrar_embedding(
            1,
            self.embedding,
        )

        inicio = len(PREFIJO)
        fin = inicio + TAMANO_NONCE

        nonce_1 = cifrado_1[inicio:fin]
        nonce_2 = cifrado_2[inicio:fin]

        self.assertNotEqual(
            nonce_1,
            nonce_2,
        )

        self.assertNotEqual(
            cifrado_1,
            cifrado_2,
        )

        print(
            "OK: Cada cifrado utiliza un nonce diferente"
        )

    def test_contenido_manipulado(self):

        cifrado = cifrar_embedding(
            1,
            self.embedding,
        )

        manipulado = bytearray(cifrado)

        # Alterar un byte de la etiqueta autenticada.
        manipulado[-1] ^= 1

        with self.assertRaisesRegex(
            ValueError,
            "No fue posible autenticar",
        ):
            descifrar_embedding(
                1,
                bytes(manipulado),
            )

        print(
            "OK: Contenido manipulado rechazado"
        )

    def test_embedding_asociado_al_usuario(self):

        cifrado = cifrar_embedding(
            1,
            self.embedding,
        )

        # Intentar descifrarlo como otro usuario.
        with self.assertRaisesRegex(
            ValueError,
            "No fue posible autenticar",
        ):
            descifrar_embedding(
                2,
                cifrado,
            )

        print(
            "OK: No se permite cambiar el usuario asociado"
        )

    def test_clave_invalida_o_ausente(self):

        clave_corta = base64.b64encode(
            os.urandom(16)
        ).decode("ascii")

        with patch.dict(
            os.environ,
            {"FACIAL_ENCRYPTION_KEY": clave_corta},
        ):

            with self.assertRaisesRegex(
                RuntimeError,
                "32 bytes",
            ):
                cifrar_embedding(
                    1,
                    self.embedding,
                )

        with patch.dict(
            os.environ,
            {"FACIAL_ENCRYPTION_KEY": ""},
        ):

            with self.assertRaisesRegex(
                RuntimeError,
                "Falta FACIAL_ENCRYPTION_KEY",
            ):
                cifrar_embedding(
                    1,
                    self.embedding,
                )

        print(
            "OK: Claves invalidas o ausentes rechazadas"
        )

    def test_identificadores_invalidos(self):

        identificadores = [
            None,
            0,
            -1,
            True,
            "1",
        ]

        for identificador in identificadores:

            with self.subTest(
                identificador=identificador
            ):

                with self.assertRaises(ValueError):
                    cifrar_embedding(
                        identificador,
                        self.embedding,
                    )

        print(
            "OK: Identificadores invalidos rechazados"
        )


if __name__ == "__main__":
    unittest.main()
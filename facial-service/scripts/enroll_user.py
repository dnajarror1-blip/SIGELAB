"""Bootstrap del perfil de un ADMIN existente mediante la API operativa; no concede acceso."""
import argparse
from contextlib import closing
import json
import os
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


class ErrorBootstrap(Exception):
    pass


class SinRedireccion(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # La clave HTTP nunca debe reenviarse a otro destino.
        return None


def resolver_admin(username, user_id):
    # Importar solo al ejecutar: leer el archivo no conecta SQL ni carga modelos.
    from app.config import get_database_config
    from app.database import get_connection, verificar_base_configurada

    if get_database_config()['database'] != 'SIGELAB':
        raise ErrorBootstrap('El bootstrap operativo requiere DB_NAME=SIGELAB; no admite bases de prueba.')
    columna, valor = ('u.username', username) if username is not None else ('u.id_usuario', user_id)
    with closing(get_connection()) as conexion:
        verificar_base_configurada(conexion)
        with closing(conexion.cursor()) as cursor:
            # La columna se elige de constantes; el argumento siempre se parametriza.
            row = cursor.execute(f'''
                SELECT u.id_usuario, u.estado, r.nombre
                FROM dbo.Usuario u JOIN dbo.Rol r ON r.id_rol = u.id_rol
                WHERE {columna} = ?
            ''', valor).fetchone()
    if row is None:
        raise ErrorBootstrap('No existe un usuario con rol válido para el identificador indicado.')
    if row[1] != 'ACTIVO':
        raise ErrorBootstrap('El usuario debe estar ACTIVO para registrar su perfil.')
    if row[2] != 'ADMIN':
        raise ErrorBootstrap('El bootstrap requiere un usuario cuyo rol real sea ADMIN.')
    return int(row[0])


def solicitar_enrollment(user_id):
    url = os.getenv('FACIAL_API_URL', 'http://127.0.0.1:8765').strip()
    parsed = urlsplit(url)
    if (parsed.scheme != 'http' or parsed.hostname not in ('127.0.0.1', 'localhost')
            or parsed.username is not None or parsed.password is not None
            or parsed.path not in ('', '/') or parsed.query or parsed.fragment
            or parsed.port == 0):
        raise ErrorBootstrap('FACIAL_API_URL debe ser una URL HTTP local sin credenciales ni ruta.')
    clave = os.getenv('FACIAL_API_KEY', '').strip()
    if len(clave) < 32 or not clave.isascii() or any(ord(c) < 32 or ord(c) == 127 for c in clave):
        raise ErrorBootstrap('Configure FACIAL_API_KEY válida en el entorno o .env del servicio.')
    request = Request(url.rstrip('/') + '/face/enroll',
                      data=json.dumps({'userId': user_id, 'adminUserId': user_id, 'replace': False}).encode('utf-8'),
                      headers={'Content-Type': 'application/json', 'Accept': 'application/json',
                               'X-Facial-Api-Key': clave}, method='POST')
    # Sin proxy ni redirects: el destino es siempre el servicio local validado.
    from urllib.request import ProxyHandler
    opener = build_opener(ProxyHandler({}), SinRedireccion())
    try:
        with opener.open(request, timeout=180) as response:
            if response.status != 200:
                raise ErrorBootstrap('El servicio no confirmó el enrollment.')
            raw = response.read(65537)
            if len(raw) > 65536:
                raise ErrorBootstrap('Respuesta de enrollment no válida.')
            result = json.loads(raw)
        if (not isinstance(result, dict) or type(result.get('userId')) is not int
                or result['userId'] != user_id or result.get('registered') is not True):
            raise ErrorBootstrap('El servicio no confirmó el perfil del usuario solicitado.')
    except HTTPError as error:
        mensajes = {401: 'La clave HTTP fue rechazada.', 403: 'El actor u objetivo ya no está autorizado.',
                    404: 'El usuario ya no existe.', 409: 'Perfil existente, cámara ocupada o captura cancelada.',
                    422: 'El servicio rechazó la solicitud.', 503: 'Servicio, configuración o captura no disponibles.'}
        # No imprimir cuerpo, cabeceras, clave ni excepciones nativas.
        error.close()
        raise ErrorBootstrap(mensajes.get(error.code, 'El servicio rechazó el enrollment.')) from None
    except (URLError, TimeoutError, ValueError):
        raise ErrorBootstrap('No se pudo completar la comunicación o la respuesta del servicio no es válida.') from None


def main():
    if not (3, 12) <= sys.version_info[:2] <= (3, 14):
        print('Este bootstrap requiere Python 3.12–3.14. Prepare .venv con setup-facial-linux.sh.', file=sys.stderr)
        return 2
    parser = argparse.ArgumentParser(description='Enrollment inicial de un ADMIN activo existente en SIGELAB; no inicia sesión.')
    selector = parser.add_mutually_exclusive_group(required=True)
    selector.add_argument('--username', help='Username exacto del ADMIN existente')
    selector.add_argument('--id', type=int, dest='user_id', help='ID del ADMIN existente')
    args = parser.parse_args()
    if (args.username is not None and not args.username.strip()) or (args.user_id is not None and args.user_id <= 0):
        parser.error('Indique un username no vacío o un ID positivo.')
    try:
        # app.config carga .env sin sobrescribir variables del entorno.
        user_id = resolver_admin(args.username, args.user_id)
        print(f'ADMIN activo resuelto: ID {user_id}. Iniciando enrollment; esto no abre una sesión Java.')
        solicitar_enrollment(user_id)
        print(f'Perfil inicial registrado para el ADMIN ID {user_id}. Inicie sesión con contraseña y rostro.')
        return 0
    except ErrorBootstrap as error:
        print(str(error), file=sys.stderr)
    except Exception:
        print('No se pudo preparar el enrollment. Revise dependencias, configuración y acceso a SIGELAB; no se muestran datos privados.', file=sys.stderr)
    return 1


if __name__ == '__main__':
    raise SystemExit(main())

"""Configuración privada interactiva; stdlib, sin SQL, cámara ni sustitución de claves."""
import base64
import getpass
import json
import os
from pathlib import Path
import re
import secrets
import sys
import tempfile

SERVICE = Path(__file__).resolve().parents[1]
ROOT_ENV = SERVICE.parent / '.env'
FACE_ENV = SERVICE / '.env'


class ConfigurationError(Exception):
    pass


def read_env(path):
    if path.is_symlink():
        raise ConfigurationError('No se admiten enlaces simbólicos para .env.')
    if not path.exists():
        return {}
    result = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        key, sep, value = line.partition('=')
        key, value = key.strip(), value.strip()
        if not sep or not re.fullmatch(r'[A-Z][A-Z0-9_]*', key) or key in result:
            raise ConfigurationError('Formato .env inválido; revise claves duplicadas o líneas sin =.')
        if value.startswith('"'):
            value = json.loads(value)
            if not isinstance(value, str):
                raise ConfigurationError('Los valores .env deben ser texto.')
        elif value.startswith("'"):
            if len(value) < 2 or not value.endswith("'"):
                raise ConfigurationError('Comillas .env incompletas.')
            value = value[1:-1]
        result[key] = value
    return result


def save_env(path, values):
    if path.is_symlink():
        raise ConfigurationError('No se reemplazan enlaces simbólicos.')
    fd, temporary = tempfile.mkstemp(prefix='.configure-', dir=path.parent)
    try:
        with os.fdopen(fd, 'w', encoding='utf-8') as stream:
            stream.write('# Configuración privada SIGELAB. No versionar ni ejecutar con source.\n')
            for key, value in values.items():
                if any(ord(c) < 32 for c in value):
                    raise ConfigurationError('No se admiten caracteres de control en configuración.')
                stream.write(key + '=' + json.dumps(value, ensure_ascii=False) + '\n')
        if os.name != 'nt':
            os.chmod(temporary, 0o600)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def main():
    if not sys.stdin.isatty():
        raise ConfigurationError('Ejecute el configurador en una terminal interactiva para ocultar la contraseña.')
    root, face = read_env(ROOT_ENV), read_env(FACE_ENV)
    def existing(key, default=''):
        return os.environ.get(key, root.get(key) or face.get(key) or default)
    # No reconciliar claves distintas silenciosamente, aunque alguna provenga del entorno.
    for key in ('FACIAL_API_KEY', 'FACIAL_ENCRYPTION_KEY'):
        candidates = {v for v in (os.getenv(key), root.get(key), face.get(key)) if v}
        if len(candidates) > 1:
            raise ConfigurationError('Hay claves distintas entre entorno y archivos; reconcílielas privadamente antes de continuar.')
    shared = ('DB_HOST','DB_PORT','DB_NAME','DB_USER','DB_PASSWORD','DB_ENCRYPT',
              'DB_TRUST_SERVER_CERTIFICATE','FACIAL_API_KEY','FACIAL_API_URL')
    for key, default in (('DB_HOST','127.0.0.1'),('DB_PORT','1433'),('DB_NAME','SIGELAB'),('DB_USER','sa')):
        current = existing(key, default)
        root[key] = input(f'{key} [{current}]: ').strip() or current
        if key in os.environ and root[key] != os.environ[key]:
            raise ConfigurationError('Existe una variable de entorno diferente; ajústela antes de configurar.')
    if not root['DB_PORT'].isdigit() or not 1 <= int(root['DB_PORT']) <= 65535:
        raise ConfigurationError('DB_PORT debe estar entre 1 y 65535.')
    password = existing('DB_PASSWORD')
    if password in ('CAMBIAR_CONTRASENA', 'COLOCA_AQUI_TU_CONTRASENA'):
        password = ''
    entered = getpass.getpass('DB_PASSWORD (oculta; Enter conserva la existente): ')
    if entered and 'DB_PASSWORD' in os.environ and entered != os.environ['DB_PASSWORD']:
        raise ConfigurationError('DB_PASSWORD ya está definida en el entorno; ajústela primero.')
    root['DB_PASSWORD'] = entered or password
    if not root['DB_PASSWORD']:
        raise ConfigurationError('Falta contraseña SQL.')
    root['DB_ENCRYPT'] = existing('DB_ENCRYPT', 'true')
    root['DB_TRUST_SERVER_CERTIFICATE'] = input('Confiar en certificado SQL local autofirmado (true/false) [' + existing('DB_TRUST_SERVER_CERTIFICATE','false') + ']: ').strip() or existing('DB_TRUST_SERVER_CERTIFICATE','false')
    if root['DB_TRUST_SERVER_CERTIFICATE'] not in ('true','false'):
        raise ConfigurationError('Use true o false para el certificado.')
    if os.getenv('FACIAL_API_URL', 'http://127.0.0.1:8765').rstrip('/') != 'http://127.0.0.1:8765':
        raise ConfigurationError('Ajuste FACIAL_API_URL del entorno a http://127.0.0.1:8765.')
    root['FACIAL_API_URL'] = 'http://127.0.0.1:8765'
    root['FACIAL_API_KEY'] = existing('FACIAL_API_KEY') or secrets.token_urlsafe(48)
    if len(root['FACIAL_API_KEY']) < 32 or not root['FACIAL_API_KEY'].isascii():
        raise ConfigurationError('La clave HTTP existente no es válida; no se reemplazó.')
    aes = existing('FACIAL_ENCRYPTION_KEY')
    if not aes:
        print('No se encontró clave AES. Si ya hay perfiles debe recuperar la clave original.')
        if input('Solo si la BD NO tiene perfiles, escriba SIN PERFILES para generar la clave: ') != 'SIN PERFILES':
            raise ConfigurationError('No se generó ni reemplazó ninguna clave AES.')
        aes = base64.b64encode(secrets.token_bytes(32)).decode('ascii')
    if len(base64.b64decode(aes, validate=True)) != 32:
        raise ConfigurationError('La clave AES existente no es válida; no se reemplazó.')
    face['FACIAL_ENCRYPTION_KEY'] = aes
    for key, default in (('DB_DRIVER','ODBC Driver 18 for SQL Server'),
                         ('FACIAL_MATCH_THRESHOLD','0.50'),('FACIAL_LIVENESS_THRESHOLD','0.80'),
                         ('FACIAL_MODEL','buffalo_l'),('FACIAL_CAMERA_INDEX','0'),
                         ('FACIAL_CAMERA_BACKEND','AUTO'),('FACIAL_CAPTURE_TIMEOUT','120')):
        face[key] = existing(key, default)
    # Migrar variables comunes de configuraciones antiguas a una sola fuente.
    root.pop('FACIAL_ENCRYPTION_KEY', None)
    for key in shared:
        face.pop(key, None)
    save_env(FACE_ENV, face)  # Conservar primero la clave de perfiles existentes.
    save_env(ROOT_ENV, root)
    if os.name == 'nt':
        print('Configuración guardada. Windows conserva las ACL del directorio; no se aplican permisos POSIX 0600.')
    else:
        print('Configuración guardada con permisos 0600.')
    print('Claves conservadas/generadas sin mostrarlas.')
    print('Threshold inicial 0.50 solo si faltaba: requiere calibración real; no omite FaceID/liveness.')


if __name__ == '__main__':
    try:
        main()
    except ConfigurationError as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(1)
    except (Exception, KeyboardInterrupt):
        print('Configuración no completada. Revise entradas, claves existentes y permisos; no se muestran datos privados.', file=sys.stderr)
        raise SystemExit(1)

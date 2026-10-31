"""Diagnóstico de solo lectura; SQL, modelo y cámara únicamente con flags explícitos."""
import argparse
import importlib
import importlib.metadata as metadata
import os
import platform
import sys
from pathlib import Path

SERVICE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SERVICE))
PACKAGES = {'numpy':'numpy','onnxruntime':'onnxruntime','opencv-python':'cv2',
            'insightface':'insightface','pyodbc':'pyodbc','bcrypt':'bcrypt',
            'cryptography':'cryptography','fastapi':'fastapi','uvicorn':'uvicorn',
            'pydantic':'pydantic','python-dotenv':'dotenv'}


def dependencies():
    from packaging.requirements import Requirement
    from packaging.specifiers import SpecifierSet
    for line in (SERVICE / 'requirements.txt').read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        required = Requirement(line)
        if required.marker and not required.marker.evaluate():
            continue
        if metadata.version(required.name) not in required.specifier:
            raise RuntimeError('Versión instalada fuera de requirements')
    for package, module in PACKAGES.items():
        installed = metadata.metadata(package)
        if platform.python_version() not in SpecifierSet(installed.get('Requires-Python') or ''):
            raise RuntimeError('Dependencia incompatible con este Python')
        importlib.import_module(module)  # No captura, carga de modelos ni SQL.
        print(package, metadata.version(package), 'Requires-Python:', installed.get('Requires-Python') or 'sin restricción')


def configuration():
    from app.config import (get_database_config, facial_threshold, liveness_threshold,
                            model_name, capture_timeout)
    from app.crypto import obtener_clave
    db = get_database_config()
    if not 1 <= db['port'] <= 65535:
        raise RuntimeError('Puerto inválido')
    obtener_clave()
    facial_threshold()
    liveness_threshold()
    model_name()
    capture_timeout()
    key = os.getenv('FACIAL_API_KEY', '')
    if len(key) < 32 or not key.isascii() or any(ord(c) < 32 or ord(c) == 127 for c in key):
        raise RuntimeError('Clave API inválida')
    if os.getenv('FACIAL_API_URL', 'http://127.0.0.1:8765').rstrip('/') != 'http://127.0.0.1:8765':
        raise RuntimeError('Este arranque requiere 127.0.0.1:8765')
    import pyodbc
    if db['driver'] not in pyodbc.drivers():
        raise RuntimeError('Falta el driver ODBC configurado')


def main():
    if not (3, 12) <= sys.version_info[:2] <= (3, 14):
        print('Se requiere Python 3.12–3.14.', file=sys.stderr)
        return 2
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--dependencies-only', action='store_true')
    mode.add_argument('--configuration-only', action='store_true')
    parser.add_argument('--database', action='store_true', help='SELECT reales, sin escritura')
    parser.add_argument('--camera', action='store_true', help='Abre/libera cámara sin guardar frames')
    parser.add_argument('--model', action='store_true', help='Carga modelo/liveness; puede requerir descarga del complemento')
    args = parser.parse_args()
    if (args.dependencies_only or args.configuration_only) and (args.database or args.camera or args.model):
        parser.error('Los modos only no admiten diagnósticos interactivos.')
    print('Plataforma:', platform.system(), platform.machine(), 'Python:', platform.python_version())
    step = 'dependencias (ejecute setup-facial-linux.sh)'
    try:
        if not args.configuration_only:
            dependencies()
        if args.dependencies_only:
            return 0
        step = 'configuración/ODBC (ejecute configure_local.py y compruebe ODBC Driver 18)'
        configuration()
        print('Configuración y driver: OK; no implica conectividad ni calibración.')
        if args.database:
            step = 'SQL (conexión, permisos y tablas Usuario/Rol/PerfilFacial/AuditoriaFacial)'
            from app.database import comprobar_esquema
            comprobar_esquema()
            print('SQL: conexión y esquema compatibles, solo SELECT.')
        if args.model:
            step = 'modelo local buffalo_l y complemento liveness'
            print('Preparando modelo/liveness; el motor puede descargar archivos faltantes.')
            from app.facial_enrollment import cargar_modelo_registro
            cargar_modelo_registro()
            print('Modelo inicializado; liveness real y calibración requieren validación posterior.')
        if args.camera:
            step = 'cámara (permisos, sesión gráfica, índice y dispositivo libre)'
            if platform.system() == 'Linux' and not (os.getenv('DISPLAY') or os.getenv('WAYLAND_DISPLAY')):
                raise RuntimeError('La captura necesita sesión gráfica')
            from app.camara import abrir_camara
            camera = abrir_camara()
            try:
                ok, frame = camera.read()
                if not ok or frame is None:
                    raise RuntimeError('Sin frames')
                print('Cámara: entrega frames; no se guardaron imágenes.')
            finally:
                camera.release()
    except Exception:
        print('NO LISTO: fallo en ' + step + '. No se muestran datos privados.', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

"""Diagnóstico optativo. Por defecto no conecta SQL ni abre cámara/carga modelos."""
import argparse
import importlib.metadata
import os
import platform
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def main():
    if sys.version_info[:2] != (3, 12):
        print(f'Se requiere Python 3.12 para SIGELAB facial; versión detectada: {platform.python_version()}. '
              'Prepare .venv con Python 3.12; no cambie las dependencias para usar otra versión.', file=sys.stderr)
        return 2
    parser = argparse.ArgumentParser()
    parser.add_argument('--database', action='store_true', help='SELECT reales: requiere autorización')
    parser.add_argument('--camera', action='store_true', help='Abre cámara: requiere autorización')
    parser.add_argument('--model', action='store_true', help='Carga modelo/liveness: requiere autorización')
    args = parser.parse_args()
    from app.config import BASE_DIR, facial_threshold
    print('Plataforma:', platform.system(), platform.machine(), 'Python:', platform.python_version())
    for package in ('fastapi','pydantic','pyodbc','cryptography','numpy','opencv-python','onnxruntime','insightface'):
        try:
            print(package, importlib.metadata.version(package))
        except importlib.metadata.PackageNotFoundError:
            print(package, 'NO INSTALADO')
    for key in ('DB_HOST','DB_NAME','DB_USER','DB_PASSWORD','FACIAL_API_KEY','FACIAL_ENCRYPTION_KEY'):
        print(key, 'configurado' if os.getenv(key) else 'ausente/default')
    try:
        from app.crypto import obtener_clave
        obtener_clave()
        facial_threshold()
        print('Clave y threshold: formato válido; no implica calibración')
    except Exception:
        print('Clave o threshold: falta configuración válida')
    print('Carpeta modelos:', 'presente' if (BASE_DIR/'models').is_dir() else 'ausente')
    try:
        if args.database:
            from app.database import comprobar_esquema
            comprobar_esquema()
            print('SQL: esquema compatible')
        if args.camera:
            from app.camara import abrir_camara
            camera = abrir_camara()
            try:
                ok, frame = camera.read()
                if not ok or frame is None:
                    raise RuntimeError('Sin frames')
                print('Cámara: entrega frames; no se guardaron imágenes')
            finally:
                camera.release()
        if args.model:
            from app.facial_enrollment import cargar_modelo_registro
            cargar_modelo_registro()
            print('Modelo inicializado; confirmar liveness mediante captura autorizada')
    except Exception:
        print('Diagnóstico solicitado falló; revise driver, permisos y dependencias. No se muestran datos privados.')
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

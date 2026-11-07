"""Autorización y mutaciones sobre las tablas reales de SIGELAB."""
from contextlib import contextmanager, closing

from .database import get_connection, verificar_base_configurada
from .config import liveness_threshold
from .perfil_storage import guardar_perfil_facial


class RechazoFacial(Exception):
    def __init__(self, codigo, mensaje):
        super().__init__(mensaje)
        self.codigo = codigo


@contextmanager
def transaccion():
    conexion = get_connection()
    try:
        conexion.autocommit = False
        verificar_base_configurada(conexion)
        yield conexion
        conexion.commit()
    except Exception:
        conexion.rollback()
        raise
    finally:
        conexion.close()


def exigir_admin(conexion, admin_id):
    with closing(conexion.cursor()) as cursor:
        row = cursor.execute("""
            SELECT u.estado, r.nombre
            FROM dbo.Usuario u WITH (UPDLOCK, HOLDLOCK)
            JOIN dbo.Rol r WITH (HOLDLOCK) ON r.id_rol = u.id_rol
            WHERE u.id_usuario = ?
        """, admin_id).fetchone()
    if row is None or row[0] != "ACTIVO" or row[1] != "ADMIN":
        raise RechazoFacial(403, "Se requiere un administrador activo")


def comprobar_administrador(admin_id):
    with transaccion() as conexion:
        exigir_admin(conexion, admin_id)


def exigir_objetivo(conexion, user_id, activo=True):
    with closing(conexion.cursor()) as cursor:
        row = cursor.execute("""
            SELECT estado FROM dbo.Usuario WITH (UPDLOCK, HOLDLOCK) WHERE id_usuario = ?
        """, user_id).fetchone()
    if row is None:
        raise RechazoFacial(404, "Usuario no encontrado")
    if activo and row[0] != "ACTIVO":
        raise RechazoFacial(403, "El usuario está inactivo")


def auditar(conexion, user_id, accion, resultado, detalle, score=None, threshold=None):
    with closing(conexion.cursor()) as cursor:
        cursor.execute("""
            INSERT INTO dbo.AuditoriaFacial
                (id_usuario, fecha_hora, accion, resultado, score, threshold, detalle, dispositivo)
            VALUES (?, SYSUTCDATETIME(), ?, ?, ?, ?, ?, 'SERVICIO_LOCAL')
        """, user_id, accion, resultado, score, threshold, detalle)


def registrar_evento(user_id, accion, resultado, detalle, score=None, threshold=None):
    with transaccion() as conexion:
        # AuditoriaFacial exige un usuario real; no inventar IDs para rechazos inexistentes.
        exigir_objetivo(conexion, user_id, activo=False)
        auditar(conexion, user_id, accion, resultado, detalle, score, threshold)


def comprobar_registro(user_id, admin_id, replace):
    with transaccion() as conexion:
        exigir_admin(conexion, admin_id)
        exigir_objetivo(conexion, user_id)
        exigir_perfil_disponible(conexion, user_id, replace)


def exigir_perfil_disponible(conexion, user_id, replace):
    with closing(conexion.cursor()) as cursor:
        row = cursor.execute("""
            SELECT id_perfil_facial FROM dbo.PerfilFacial WITH (UPDLOCK, HOLDLOCK)
            WHERE id_usuario = ?
        """, user_id).fetchone()
    if row is not None and not replace:
        raise RechazoFacial(409, "El usuario ya tiene perfil; reemplazar requiere replace=true")


def guardar_registro(user_id, admin_id, replace, perfil):
    with transaccion() as conexion:
        exigir_admin(conexion, admin_id)
        exigir_objetivo(conexion, user_id)
        exigir_perfil_disponible(conexion, user_id, replace)
        # Cifrado y auditoría REGISTRO/EXITO pertenecen a esta misma transacción.
        guardar_perfil_facial(user_id, perfil["embedding"], perfil["cantidad_muestras"],
                              perfil["confianza_deteccion_media"], conexion=conexion, registrar_auditoria=False)
        auditar(conexion, user_id, "ENROLL", "EXITO", f"Actor ADMIN id={admin_id}; replace={replace}")
        auditar(conexion, user_id, "LIVENESS", "EXITO", "Presencia real en enrollment",
                perfil["liveness_promedio"], liveness_threshold())


def eliminar_registro(user_id, admin_id):
    with transaccion() as conexion:
        exigir_admin(conexion, admin_id)
        # También se puede retirar biometría de una cuenta inactiva.
        exigir_objetivo(conexion, user_id, activo=False)
        with closing(conexion.cursor()) as cursor:
            row = cursor.execute("""
                DELETE FROM dbo.PerfilFacial OUTPUT DELETED.id_perfil_facial WHERE id_usuario = ?
            """, user_id).fetchone()
        if row is None:
            raise RechazoFacial(404, "Perfil facial no encontrado")
        auditar(conexion, user_id, "ELIMINACION", "EXITO", f"Actor ADMIN id={admin_id}")
    return {"userId": user_id, "deleted": True}


def version_perfil(user_id):
    """Instantánea interna; nunca se expone a HTTP ni logs."""
    conexion = get_connection()
    try:
        with closing(conexion.cursor()) as cursor:
            row = cursor.execute("""
                SELECT p.id_perfil_facial, p.embedding, p.activo, u.estado
                FROM dbo.Usuario u LEFT JOIN dbo.PerfilFacial p ON p.id_usuario = u.id_usuario
                WHERE u.id_usuario = ?
            """, user_id).fetchone()
        if row is None:
            raise RechazoFacial(404, "Usuario no encontrado")
        if row[3] != "ACTIVO":
            raise RechazoFacial(403, "El usuario está inactivo")
        if row[0] is None or not row[2]:
            raise RechazoFacial(409, "El usuario no tiene un perfil facial activo")
        return (row[0], bytes(row[1]))
    finally:
        conexion.close()

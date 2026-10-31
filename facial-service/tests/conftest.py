"""La colección habitual no importa módulos que escriben en SQL Server."""
import os

collect_ignore = [] if os.getenv("SIGELAB_RUN_SQL_TESTS") == "1" else [
    "test_registro_seguridad.py", "test_registro_facial_nuevo.py", "test_perfil_storage.py",
    "test_auditoria_rechazo_rollback.py", "test_eliminacion_facial_sql_rollback.py",
]

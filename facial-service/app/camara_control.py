"""
SIGELAB FaceID - Control compartido de camara.

Evita que dos operaciones faciales intenten
utilizar simultaneamente la misma camara.

Este modulo no abre la camara.
No accede a SQL Server.
No registra informacion biometrica.
"""

import threading


BLOQUEO_CAMARA = threading.Lock()
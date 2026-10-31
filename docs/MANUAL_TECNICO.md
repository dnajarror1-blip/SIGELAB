# Manual técnico — SIGELAB

## Arquitectura y herramientas

JDK 21, Swing y Maven Wrapper. `sigelab.Main` abre el login. Flujo: View → Controller → Service → Repository → JDBC → SQL Server 2022. View captura/presenta; Controller coordina; Service valida reglas, permisos y transacciones; Repository parametriza SQL y convierte modelos. `model`/`enums` son contratos compartidos; `config` centraliza JDBC; `integration/qr` encapsula ZXing y los servicios faciales encapsulan HTTP/JSON.

Dependencias y versiones están en `pom.xml`. No se requiere un servidor de aplicación Java. Swing necesita entorno gráfico al operar; `headless=true` se utiliza en pruebas aisladas.

## Base de datos y datos iniciales

Seguir el [README, arranque desde cero](../README.md#arranque-desde-una-base-limpia-orden-obligatorio): iniciar SQL Server, ejecutar `database/sigelab.sql`, `001_roles.sql`, `002_estudiante.sql`, `003_datos_demo.sql`, en ese orden. Ejecutar scripts con soporte `GO`. La base se llama SIGELAB; las tablas están en dbo. Roles por nombre, IDs identity; nunca fijar IDs de roles en código.

El seed demo incluye dos usuarios BCrypt, laboratorio, tipo y cuatro equipos. Inserta faltantes sin alterar datos previos. No incluye perfiles faciales. En BD existente comprobar migraciones; el script inicial no modifica tablas preexistentes. UNIQUE de código de inventario y de devolución por detalle protegen integridad en SQL. El seed aún requiere validación ejecutándolo y repitiéndolo contra SQL Server de pruebas.

## Variables y secretos

Java lee `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_ENCRYPT`, `DB_TRUST_SERVER_CERTIFICATE`, `FACIAL_API_URL`, `FACIAL_API_KEY` del entorno del proceso. `DatabaseConfig` obtiene y cierra conexiones mediante los repositorios/servicios; no carga `.env`.

Python carga `facial-service/.env`, con prioridad del entorno. Usa las mismas variables DB y `DB_DRIVER`, más las variables faciales enumeradas en el README y `.env.example`. Driver requerido: Microsoft ODBC Driver 18; en Linux requiere unixODBC. Python debe ser 3.12.x.

`FACIAL_API_KEY` protege HTTP local y se comparte con Java. `FACIAL_ENCRYPTION_KEY` cifra embeddings con AES-GCM, Base64 de 32 bytes, solo Python. Conservarla privadamente: cambiarla impide descifrar los perfiles existentes. No persistir fotos ni exponer embeddings, hashes o claves en respuestas/logs. Los archivos `.env` y secretos no se versionan.

## Transacciones

- Préstamos: validar actor/solicitante y equipos; encabezado, detalles, cambio a PRESTADO y auditoría comparten conexión y commit. Una excepción provoca rollback.
- Devoluciones: validar lote pendiente, pertenencia y no duplicación; insertar devoluciones, cambiar a DISPONIBLE o DANADO y auditar en una conexión. Finalizar préstamo solo sin pendientes. Dos de tres mantiene ACTIVO y un detalle pendiente.
- Mantenimiento: apertura/cierre, cambio de estado y auditoría comparten transacción; REPARADO deja DISPONIBLE, NO_REPARABLE deja BAJA. Fallos revierten cambios.
- Usuarios: activación/desactivación y auditoría conservan su sobrecarga transaccional.
- Login (RN-18): `registrarObligatorio` usa la persistencia existente con conexión propia y propaga `DataAccessException` controlada. La sesión se entrega después de persistir LOGIN_EXITOSO. UsuarioService audita fallos de credenciales; AutenticacionService audita rechazos posteriores. No se duplica el evento al fallar la persistencia del éxito. El registro general de mejor esfuerzo y el registro con Connection conservan sus contratos.

## QR y FaceID

ZXing genera/lee imágenes QR a partir del código de un equipo existente; identifica y consulta disponibilidad, sin cambiar estados.

Java valida contraseña BCrypt y ACTIVO antes de `POST /face/verify`; FastAPI recibe userId y valida rostro/liveness. Java valida la respuesta y reconsulta usuario/rol antes de auditar y abrir menú. HTTP 200 solo no permite acceso. FastAPI usa ODBC para PerfilFacial/AuditoriaFacial y un proceso de captura OpenCV/InsightFace. Mutaciones del perfil y su auditoría se confirman juntas.

Arranque exacto: **BD → seeds → ADMIN demo existente → Python 3.12/configuración/modelos → FastAPI → enrollment ADMIN → GET /face/status/{id} → Java → login**. Los comandos están en README. Enrollment utiliza `scripts/enroll_user.py --username admin.demo`, no crea Usuario ni concede sesión. Administrar perfiles posteriores desde Usuarios con ADMIN activo. API local en 127.0.0.1:8765, un worker, sin reload. Más contratos: [integración facial](FACIAL_INTEGRACION.md).

## Pruebas y diagnóstico

Desde raíz:

```sh
./mvnw -Djava.awt.headless=true '-Dtest=*Test,!*ManualTest' test
./mvnw -Djava.awt.headless=true '-Dtest=AutenticacionServiceTest,UsuarioAuditoriaTest,AuditoriaServiceTest,AuditServiceTransaccionalTest,EquipoCp03Test,DevolucionTransaccionTest,DatosDemoSeedTest' test
```

Las pruebas unitarias/aisladas usan dobles JDBC, repositorios en memoria y HTTP simulado; no prueban bloqueos reales de SQL ni biometría. El POM excluye `**/*IT.java`. Los `*IT` requieren selección expresa, revisión de efectos y autorización sobre BD dedicada. No ejecutar contra datos reales. Surefire escribe evidencia en `target/surefire-reports`. No sumar reejecuciones como casos únicos.

FaceID simulado requiere socket local en `FacialAuthClientTest` y `FacialContratoTest`. Si falla exclusivamente con `SocketException: Operation not permitted`, repetir solo esa clase con permiso local; no cambiar el cliente para el sandbox. Python/pytest queda fuera de la regresión Java y pendiente en Python 3.12.

| Síntoma | Comprobación |
|---|---|
| Java no conecta | Variables en la configuración de ejecución, puerto y base SIGELAB, permisos SQL/TLS |
| Python no conecta | ODBC Driver 18 instalado y nombre DB_DRIVER, mismas variables y base que Java |
| 401 facial | Misma FACIAL_API_KEY en los dos procesos |
| Sin perfil/409 | Consultar status antes de repetir enrollment; cerrar otro proceso de cámara |
| Captura/modelo no disponible | Python 3.12, modelos y liveness, permisos de webcam y sesión gráfica |
| Perfil no descifrable | Recuperar la clave AES original; no sustituirla al reiniciar |
| Auditoría no disponible | Comprobar conexión y permiso INSERT en dbo.Auditoria; acceso debe permanecer denegado |
| Resultado incierto por timeout | Consultar estado persistido antes de reintentar la operación |

Pendiente: instalación limpia SQL, repetición del seed, integridad y rollback reales, cámara/enrollment/liveness/calibración y CP-01/02 biométricos. Ver [matriz](MATRIZ_PRUEBAS.md).

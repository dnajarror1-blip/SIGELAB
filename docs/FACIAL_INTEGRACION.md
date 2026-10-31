# Integración facial de SIGELAB

## Arranque vigente

Seguir el [README](../README.md#arranque-desde-una-base-limpia-orden-obligatorio): BD existente → seed demo si falta → comprobar ADMIN demo existente → configurar Python 3.12–3.14/ODBC/claves/modelos → iniciar FastAPI → enrollment del ADMIN demo → comprobar GET /face/status/{id} → iniciar Java → probar login. El bootstrap **no crea usuarios**; solo registra el perfil de un ADMIN ACTIVO ya existente. Los resultados históricos al final de este documento no sustituyen la [matriz actual](MATRIZ_PRUEBAS.md).

## Arquitectura y límites

Java Swing → LoginController → AutenticacionService → UsuarioService/Repository → JDBC → SQL Server.
AutenticacionService → FacialAuthClient → HTTP/JSON local → FastAPI → auth_service → Repository/ODBC y proceso interactivo OpenCV/InsightFace.

Java valida username, BCrypt y ACTIVO antes de llamar verify. La API recibe únicamente userId en el cuerpo de verify; no recibe contraseña, hash ni imágenes. Solo la aplicación Java decide abrir el menú después del resultado facial satisfactorio y de reconsultar usuario/rol. SwingWorker evita bloquear el EDT y evita intentos simultáneos en la misma ventana.

No hay backend de sesiones, JWT ni firmas HMAC. La protección HTTP existente X-Facial-Api-Key se conserva y no tiene relación con el cifrado de embeddings. La comparación constante de esa clave no es un protocolo de firma HMAC.

El cliente administrativo Java reconsulta actor ACTIVO y Rol.nombre=ADMIN. Python vuelve a comprobarlos antes de capturar y antes del commit. Esto valida al actor declarado contra la BD: **no constituye autenticación criptográfica de procesos locales**. Cualquier proceso local con acceso a la clave HTTP sigue dentro del límite de confianza académico.

## Primera ejecución

1. Base SIGELAB existente: ejecutar `database/seed/003_datos_demo.sql` si faltan datos, con roles de 001/002 disponibles. No recrear BD. El seed solo inserta faltantes; un ADMIN demo preexistente inactivo o con otro rol debe revisarse, no se cambia automáticamente.
2. Raíz del proyecto: `python3 facial-service/scripts/configure_local.py`. Contraseña oculta y archivos privados 0600. Preserva claves existentes; sin AES exige confirmar ausencia de perfiles antes de generarla. Si existen perfiles, recuperar clave original. Threshold inicial 0.50, no calibrado; liveness inicial 0.80.
3. `bash facial-service/prepare-demo-linux.sh`. Reutiliza setup y doctor: Python/venv, requirements, ODBC Driver 18, configuración, SELECT de esquema/conexión, modelo y cámara. El motor puede descargar modelos/complemento al inicializarse; necesita red y permisos de disco. No modifica datos de negocio ni hace enrollment. `READY FOR DEMO` acredita preflight, no reconocimiento real.
4. `bash facial-service/start-facial.sh`: valida configuración antes de Uvicorn en 127.0.0.1:8765, un worker, sin reload.
5. Otra terminal: `bash facial-service/enroll-user.sh --username admin.demo`. Resuelve ADMIN ACTIVO por username; llama POST /face/enroll con la clave HTTP, sin crear usuarios ni pedir IDs. Si ya existe perfil, no repetir. GET /face/status/{id}, con la clave en cabecera configurada privadamente, permite confirmar registrado/activo. /health no acredita captura ni liveness.
6. Ejecutar `sigelab.Main`, JDK 21, directorio de trabajo raíz del proyecto.
7. Iniciar sesión con contraseña DEMO del README y rostro registrado. Se revalida usuario/rol y persiste LOGIN_EXITOSO antes de devolver sesión. Perfil ausente o servicio apagado no permite entrar.

## Configuración compartida

Entorno real > `.env` raíz > `facial-service/.env`. Java lee solo entorno y `.env` raíz; Python lee ambos archivos sin interpolación. El configurador migra variables comunes antiguas y se detiene si encuentra claves distintas; no rota claves. No ejecutar `.env` con shell ni versionarlo (ya excluido por las reglas existentes).

- Raíz: DB_HOST/PORT/NAME/USER/PASSWORD, DB_ENCRYPT/TRUST_SERVER_CERTIFICATE, FACIAL_API_URL y FACIAL_API_KEY.
- Python: DB_DRIVER, FACIAL_ENCRYPTION_KEY, FACIAL_MATCH_THRESHOLD, FACIAL_LIVENESS_THRESHOLD, FACIAL_MODEL, FACIAL_CAMERA_INDEX/BACKEND y FACIAL_CAPTURE_TIMEOUT.
- API_KEY aleatoria, al menos 32 caracteres; AES Base64 de 32 bytes, privada de Python. Mantener AES tras enrollment: una nueva no descifra perfiles antiguos. Nunca imprimir secretos.
- El umbral 0.50 es solo un punto de partida configurable; debe calibrarse con el mismo usuario y otras personas. No elimina la exigencia de rostro y liveness válidos.

## Python y dependencias

Se acepta **Python 3.12–3.14** en setup, doctor y bootstrap. No se cambiaron versiones. `PYTHON_BIN` permite escoger intérprete al crear venv; los entornos existentes no se reemplazan. La aceptación del rango no garantiza instalación en todas las arquitecturas: setup verifica versiones/imports y doctor informa el paquete instalado y su Requires-Python.

| Dependencia | Declaración actual | Evidencia publicada |
|---|---|---|
| numpy | 2.5.4 | Requires-Python >=3.12 |
| onnxruntime | 1.31.1 | >=3.11; clasifica 3.12/3.13/3.14 |
| opencv-python | 5.0.0.93 | >=3.6; clasifica 3.12/3.13/3.14 |
| insightface | 2.1 | >=3.10; complemento liveness soportado |
| bcrypt | 5.0.0 | No impone exclusividad 3.12 |
| pyodbc, cryptography, fastapi, uvicorn | Sin pin (FastAPI >=0.115,<1) | Comprobar versión resuelta/Requires-Python con doctor al instalar |

Fuentes oficiales de metadatos: [NumPy 2.5.4](https://pypi.org/project/numpy/2.5.4/), [ONNX Runtime 1.31.1](https://pypi.org/project/onnxruntime/1.31.1/), [OpenCV 5.0.0.93](https://pypi.org/project/opencv-python/5.0.0.93/), [InsightFace 2.1](https://pypi.org/project/insightface/2.1/), [bcrypt 5.0.0](https://pypi.org/project/bcrypt/5.0.0/), [pyodbc](https://pypi.org/project/pyodbc/), [cryptography](https://pypi.org/project/cryptography/), [FastAPI](https://pypi.org/project/fastapi/), [Uvicorn](https://pypi.org/project/uvicorn/). Metadatos compatibles no sustituyen la prueba del motor/modelo/cámara en esta PC.

En el workspace inspeccionado: Python global 3.14.8 y NumPy 2.5.4; las demás dependencias anteriores no estaban instaladas, y faltaban venv, modelos y los dos .env. No se instaló ni se ejecutó cámara, SQL, enrollment o biometría durante esta preparación estática.

Doctor por defecto revisa dependencias y configuración, sin SQL/cámara/modelo. Flags `--database`, `--camera`, `--model` activan esos diagnósticos expresamente; `--configuration-only` valida arranque sin abrir cámara ni SQL. La preparación Linux usa los tres diagnósticos y por ello debe ejecutarla el operador cuando esté listo. Requiere ODBC Driver 18/unixODBC, sesión gráfica, webcam libre y modelos/complemento liveness disponibles. No usar preparar_bd_face.py ni scripts TEST_FACE sobre SIGELAB.

## API y contratos Java

Todos los endpoints salvo health requieren X-Facial-Api-Key.

| Endpoint | Entrada | Resultado |
|---|---|---|
| GET /health | Sin captura | Estado de BD/configuración y presencia de carpeta de modelo. ready=false, cameraReady=false y modelLoaded=false reflejan que health no los comprueba. |
| GET /face/status/{userId} | ID positivo | Metadatos existentes: id_usuario, usuario_existe, usuario_activo, perfil_registrado, perfil_activo, modelo, dimension, cantidad_muestras. Sin embedding. |
| POST /face/verify | {"userId":7} | userId, matched, score, threshold, livenessPassed, livenessScore. |
| POST /face/enroll | {"userId":7,"adminUserId":42,"replace":false} | userId, registered. |
| DELETE /face/profile/{userId}?adminUserId=42 | ID objetivo y actor | userId, deleted. |

IDs de los ejemplos son ilustrativos, no IDs de rol. Los cuerpos JSON rechazan campos extra y tipos incorrectos. Usuario inexistente: 404; estado/actor no permitido: 403; perfil ausente para verify, perfil existente sin replace, cámara ocupada o cancelación: 409; datos inválidos: 422; configuración, modelo, cámara, comunicación o persistencia fallida: 503. Perfil ausente al eliminar: 404. Coincidencia negativa devuelve 200 con matched=false; Java deniega el acceso.

FacialAuthClient conserva consultarSalud(), consultarEstado(id) y verificarDiagnostico(id) como adaptadores para el panel existente. Los nuevos métodos son verificar(id), consultarPerfil(id), registrar(id, adminUserId, reemplazar) y eliminar(id, adminUserId), con resultados tipados. El método llamado verificarDiagnostico ahora consulta la ruta operativa; no debe usarse como decisión final de sesión. HTTP 200 no basta para entrar. El cliente valida JSON estricto, ID, tipos, finitud, liveness y coherencia de score/threshold.

## Persistencia, concurrencia y auditoría

PerfilFacial conserva UNIQUE(id_usuario), FK a Usuario, embedding VARBINARY cifrado, modelo/dimensión/muestras/calidad, fechas y activo. Reemplazar actualiza la fila canónica; replace=false rechaza también un perfil inactivo existente. Objetivo de enrollment: usuario ACTIVO. Eliminar borra solo PerfilFacial, permite objetivo inactivo y conserva Usuario/AuditoriaFacial.

AES-GCM conserva SF1 + nonce aleatorio de 12 bytes + ciphertext/tag, con datos asociados al ID del usuario. Clave Base64 de 32 bytes. No hay fotos persistentes. El proceso interactivo intercambia embeddings únicamente mediante pipes en memoria, nunca por argumentos, archivos o logs; no los devuelve por HTTP.

El servidor mantiene exclusión de cámara durante toda la operación. Antes/después de captura se compara la versión del perfil (ID y ciphertext, solo en memoria) para rechazar reemplazos/eliminaciones concurrentes. El guardado revalida ADMIN y objetivo bajo bloqueos transaccionales y la unicidad SQL evita duplicados. La auditoría de mutación exitosa se confirma con el perfil; un error revierte ambos.

La auditoría de login es obligatoria mediante `AuditService.registrarObligatorio`: persiste con el repositorio existente y propaga un error controlado si falla. LOGIN_FALLIDO de credenciales pertenece a UsuarioService; los rechazos posteriores pertenecen a AutenticacionService. LOGIN_EXITOSO se persiste después de FaceID y de revalidar usuario/rol, antes de devolver sesión. Si falla su persistencia, no se entrega sesión ni se solicita otro evento por el mismo fallo. Las sobrecargas general y transaccional existentes conservan sus contratos. No se inventan IDs para usuarios desconocidos. Ver [manual técnico](MANUAL_TECNICO.md).

AuditoriaFacial registra inicio, éxito/fallo y causas controladas sin datos biométricos. Score/threshold se guardan al comparar. No se inventan usuarios para eventos que no pueden satisfacer su FK; errores previos a configuración/BD válida tampoco pueden garantizar auditoría SQL. Una caída de persistencia impide completar el resultado facial satisfactoriamente.

## Administración facial desde Usuarios

La capa Java se implementó directamente sobre la integración actual, sin integrar la rama antigua feature/facial-admin ni recuperar su LoginService. El flujo es:

`UsuariosPanel → FacialAdminController → FacialAdminService → FacialAuthClient → FastAPI`

El constructor público de UsuariosPanel recibe el usuario autenticado. FacialAdminController captura su ID estable y sus acciones solo reciben el ID objetivo, obtenido de la fila seleccionada de la tabla (con conversión al índice del modelo si hay ordenación). No hay campos editables para introducir el actor o el ID objetivo facial. Los campos de creación de usuarios conservan su función original.

FacialAdminService valida actor no nulo, ID del actor y objetivo positivos, y llama a UsuarioService.exigirAdministradorActivo antes de cada operación, incluida la consulta de estado. No usa IDs fijos de rol ni confía en el rol/estado guardado en la sesión. La autorización existente del cliente y las comprobaciones ADMIN en Python se conservan. ENCARGADO, ESTUDIANTE y ADMIN inactivo no están autorizados.

- **Estado facial:** consulta consultarPerfil y muestra si el perfil está registrado y activo.
- **Registrar rostro:** comprueba el estado para detectar un perfil existente y sugerir Reemplazar rostro; llama registrar(targetUserId, adminUserId, false). No reemplaza implícitamente.
- **Reemplazar rostro:** exige confirmación en la interfaz y llama registrar(targetUserId, adminUserId, true).
- **Eliminar rostro:** exige confirmación, informa de la necesidad de registrar un nuevo perfil para volver a iniciar sesión y llama eliminar(targetUserId, adminUserId).

FacialAdminController devuelve resultados tipados y mensajes controlados ante permisos denegados, errores de comunicación/respuesta y problemas de acceso/configuración de datos. No muestra stack traces ni respuestas HTTP crudas. El cliente se configura al ejecutar una operación, no al construir la pantalla.

Las operaciones faciales y el refresco posterior se ejecutan mediante SwingWorker. Mientras una esté en curso, se deshabilitan los cuatro botones faciales, la tabla y las acciones de crear/cambiar estado para evitar duplicados y cambios de objetivo. Al terminar se rehabilitan; sin selección los botones faciales quedan deshabilitados. Una mutación confirmada sigue mostrándose como exitosa aunque falle la consulta posterior del estado, y se solicita actualizarlo antes de reintentar.

RegistroFacialPanel se conserva como herramienta de diagnóstico, con título y texto que remiten a Usuarios para administrar perfiles. No se añadió al menú ni se convirtió en una segunda administración. Los contratos HTTP, el login, la auditoría de login y Python no se modificaron en esta ampliación.

Se ejecutaron y aprobaron las **25 pruebas nuevas de administración facial**, con dobles en memoria y Swing headless para autorización, operaciones, manejo de errores, selección/actor, confirmaciones, bloqueo de doble clic y refresco:

- `FacialAdminServiceTest`: **13 passed**.
- `FacialAdminControllerTest`: **6 passed**.
- `UsuariosPanelFacialTest`: **6 passed**.

La administración facial Java queda **validada de forma aislada**. Esto no implica validación biométrica real ni FaceID end-to-end.

## Pruebas ejecutadas y validaciones pendientes

La ejecución más reciente de Maven abarcó **198 pruebas Java aisladas**, ejecutadas clase por clase para detenerse ante un fallo. Resultado agregado de las 31 clases: **198 passed, 0 failures, 0 errors, 0 skipped**. Todas las invocaciones finalizaron con **BUILD SUCCESS**. Se verificaron los reportes: **ninguna clase `*IT` fue ejecutada**. No se accedió a SQL Server ni se ejecutaron cámara, enrollment real o migraciones.

Como antecedente, una ejecución anterior a la ampliación administrativa tuvo 14 errores `java.net.SocketException: Operation not permitted` al crear los servidores HTTP simulados, debido a la restricción de sockets del sandbox. Al repetir con permiso para servidores HTTP locales, esa incidencia se resolvió **sin cambios de código**. La ejecución más reciente de las 198 pruebas también contó con permiso para esos servidores locales y no presentó errores.

El POM contiene una única declaración de Surefire **3.5.5** para JUnit 5, con exclusión explícita `**/*IT.java`. Las regresiones ejecutadas de **login, usuarios, auditoría y permisos no mostraron fallos**. Las pruebas Java validaron de forma aislada la coordinación del login en dos factores, la solicitud de LOGIN_EXITOSO después del facial y la revalidación, los rechazos de acceso, el cliente HTTP simulado, status/enroll/delete y permisos ADMIN. Esto no verifica persistencia SQL ni reconocimiento biométrico real.

**Pytest continúa pendiente:** la preparación actual acepta Python 3.12–3.14; el bloqueo histórico exclusivo a 3.12 se retiró tras revisar los metadatos de dependencias. No se ejecutó pytest ni se instalaron dependencias durante esta preparación estática. Instalar `requirements-dev.txt` en el venv antes de ejecutar pytest. Las pruebas que escriben SQL siguen requiriendo autorización y BD dedicada; `SIGELAB_RUN_SQL_TESTS=1` no constituye autorización por sí solo.

La inspección previa al commit confirmó que `.env.docker` es un archivo **local, ignorado y no versionado**. Debe permanecer fuera del commit; no se reproduce su contenido.

Siguen pendientes **pytest con Python compatible, SQL Server real, cámara, liveness, calibración/configuración real del threshold y enrollment real**. La **validación FaceID end-to-end sigue pendiente**; los resultados Java aislados no permiten declararla completada.

Comandos para pruebas aisladas: desde facial-service y con un entorno Python 3.12–3.14 preparado, `.venv/bin/python -m pytest`; desde la raíz, `./mvnw test`. Ejecutarlos únicamente dentro de la autorización correspondiente. No usar selección de IT ni ejecutar scripts de registro/preparación sin revisar sus efectos. LoginControllerIT tiene FaceID simulado explícitamente y crea usuarios reales de prueba; está protegido por opt-in y no valida biometría real.

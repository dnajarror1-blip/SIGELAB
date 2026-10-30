# Integración facial de SIGELAB

## Arquitectura y límites

Java Swing → LoginController → AutenticacionService → UsuarioService/Repository → JDBC → SQL Server.
AutenticacionService → FacialAuthClient → HTTP/JSON local → FastAPI → auth_service → Repository/ODBC y proceso interactivo OpenCV/InsightFace.

Java valida username, BCrypt y ACTIVO antes de llamar verify. La API recibe únicamente userId en el cuerpo de verify; no recibe contraseña, hash ni imágenes. Solo la aplicación Java decide abrir el menú después del resultado facial satisfactorio y de reconsultar usuario/rol. SwingWorker evita bloquear el EDT y evita intentos simultáneos en la misma ventana.

No hay backend de sesiones, JWT ni firmas HMAC. La protección HTTP existente X-Facial-Api-Key se conserva y no tiene relación con el cifrado de embeddings. La comparación constante de esa clave no es un protocolo de firma HMAC.

El cliente administrativo Java reconsulta actor ACTIVO y Rol.nombre=ADMIN. Python vuelve a comprobarlos antes de capturar y antes del commit. Esto valida al actor declarado contra la BD: **no constituye autenticación criptográfica de procesos locales**. Cualquier proceso local con acceso a la clave HTTP sigue dentro del límite de confianza académico.

## Preparación Linux (comandos documentados, no ejecutados por Codex)

1. Instalar **Python 3.12.x**, soporte venv y dependencias nativas de compilación/GUI de OpenCV según la distribución. Para InsightFace pueden requerirse compilador y cabeceras Python. Usar sesión gráfica y permisos de lectura sobre la cámara.
2. Instalar unixODBC y Microsoft ODBC Driver 18 siguiendo las instrucciones de Microsoft para la distribución. `odbcinst -q -d` debe mostrar el driver configurado en DB_DRIVER. Verificar conectividad de red con la instancia SQL Server, puerto, usuario y permisos mediante herramientas locales autorizadas.
3. Desde la raíz ejecutar `bash facial-service/setup-facial-linux.sh`. El script usa `python3.12` (o `PYTHON_BIN` apuntando a Python 3.12), valida su versión antes de crear .venv e instalar requirements-dev; requiere autorización para instalar y descargar. No copia ni sobrescribe .env. Si .venv ya existe con otra versión de Python, se detiene sin recrearla ni instalar dependencias. El operador debe revisar ese entorno y preparar uno nuevo con 3.12.
4. Copiar manualmente `facial-service/.env.example` a `facial-service/.env` y completar DB_USER/DB_PASSWORD, FACIAL_API_KEY, FACIAL_ENCRYPTION_KEY y FACIAL_MATCH_THRESHOLD. El ejemplo no contiene secretos ni un umbral supuestamente calibrado. Las variables de entorno prevalecen sobre .env.
5. Generar FACIAL_ENCRYPTION_KEY una sola vez como Base64 de 32 bytes aleatorios; FACIAL_API_KEY debe ser otra clave aleatoria de al menos 32 caracteres. Puede usarse Python secrets/base64 localmente. No pegar sus salidas en tickets, commits, logs o capturas. Conservar la clave AES de forma privada después del enrollment. Una clave diferente no descifra los perfiles previos; no se implementa rotación automática.
6. En SIGELAB ya deben existir Usuario, Rol, PerfilFacial y AuditoriaFacial según `database/sigelab.sql`. No usar preparar_bd_face.py para la BD operativa: es una herramienta exclusiva de TEST_FACE. Si faltan tablas, revisar y ejecutar manualmente, con autorización, los bloques faciales oficiales sobre la base existente. No crear otra base ni otro Usuario. El servicio no ejecuta DDL ni migraciones.
7. Desde facial-service ejecutar `.venv/bin/python scripts/doctor.py` para información de dependencias/configuración; no abre cámara ni consulta SQL. `--database` realiza SELECT reales, `--camera` abre y libera cámara, y `--model` carga el motor. Cada uno requiere autorización separada. La carga del modelo no demuestra por sí sola que liveness funcione: verificarlo en captura real autorizada.
8. Coordinar con Integrante 8 la disponibilidad/licencia y versiones del modelo buffalo_l y del complemento liveness soportado por el motor existente. Se esperan modelos locales en facial-service/models y, si corresponde al motor, addons; no versionarlos. La carga del motor puede descargarlos si faltan. No se ha validado instalación ni descarga en esta estabilización.
9. Iniciar con `bash facial-service/start-facial.sh`. Usa `.venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8765 --workers 1`. No usar 0.0.0.0, múltiples workers ni --reload: la exclusión de cámara es por proceso.
10. Configurar Java con FACIAL_API_URL=http://127.0.0.1:8765 y la misma FACIAL_API_KEY. Java lee variables del proceso, no el archivo .env de Python. No configurar FACIAL_ENCRYPTION_KEY en Java.

FACIAL_MATCH_THRESHOLD es obligatorio, finito y en (0,1]. No tiene default. El Integrante 8 debe calibrarlo con capturas del mismo usuario y de personas distintas. FACIAL_LIVENESS_THRESHOLD tiene valor inicial 0.80, pendiente de validación práctica. Modelo compatible: buffalo_l, 512 float32 little-endian. La captura conserva seis muestras válidas, calidad y promedio normalizado existentes; no se cambian por los valores particulares del proyecto de referencia.

FACIAL_CAMERA_BACKEND=AUTO selecciona V4L2/Linux, DSHOW/Windows o AVFOUNDATION/macOS, con fallback ANY. FACIAL_CAMERA_INDEX selecciona cámara. FACIAL_CAPTURE_TIMEOUT limita captura (10–140 segundos; default 120), y el proceso tiene un límite adicional de 30 segundos para carga. Una carga lenta también deniega acceso, nunca omite el factor.

Windows: `py -3.12 -m venv .venv`, `.venv\Scripts\python -m pip install -r requirements-dev.txt`, configurar .env e iniciar `.venv\Scripts\python -m uvicorn app.main:app --host 127.0.0.1 --port 8765 --workers 1` desde facial-service. Instalar ODBC Driver y conceder permisos de cámara. macOS: usar Python 3.12 y los comandos equivalentes de Linux, el driver compatible con su arquitectura y permisos de cámara; OpenCV se ejecuta en MainThread de un proceso separado. Estas plataformas no han sido probadas en esta revisión.

## Versión Python del equipo

Esta integración se estandariza en **Python 3.12.x**, no en cualquier versión superior a 3.11. `setup-facial-linux.sh`, `scripts/doctor.py` y el bootstrap rechazan otras versiones con un mensaje explícito. Doctor comprueba la versión antes de importar la configuración o dependencias del servicio. Los requirements se mantienen intactos, incluido numpy==2.5.4; no se adapta esta tarea a Python 3.11.

## Bootstrap del primer perfil ADMIN

`enroll-user.sh` permite registrar el primer perfil facial de un ADMIN **ya existente y ACTIVO** en la base operativa SIGELAB mientras facial-admin todavía no está integrada. Es una operación administrativa local de enrollment, **no un bypass de autenticación**: no abre sesión, no devuelve una sesión y no cambia el login obligatorio de contraseña más rostro.

Prerrequisitos: entorno Python 3.12 preparado, ODBC y configuración del servicio correctos, las tablas oficiales existentes, una cuenta ADMIN activa creada por el proceso normal de usuarios, clave AES persistente, FACIAL_API_KEY y threshold configurados, y FastAPI iniciado con `start-facial.sh`. El operador debe estar autorizado para administrar esa instalación y tener acceso a su configuración privada. Esta operación consulta SQL Server y activa la captura mediante el servicio: ejecutarla solo con autorización explícita.

Desde la raíz del proyecto, elegir exactamente una forma:

```bash
bash facial-service/enroll-user.sh --username nombre_admin
bash facial-service/enroll-user.sh --id 7
```

Los valores son ejemplos: no se supone que un rol o usuario tenga un ID fijo. `--username` también permite desambiguar usernames numéricos. No pasar contraseñas ni claves como argumentos. El script usa FACIAL_API_KEY del entorno o de `facial-service/.env` cargado por app.config, con prioridad del entorno. FACIAL_API_URL tiene default `http://127.0.0.1:8765` y solo admite HTTP local; no se siguen redirecciones ni se utiliza proxy.

El helper `scripts/enroll_user.py` resuelve el usuario con un SELECT parametrizado, relaciona Usuario.id_rol con Rol.id_rol y exige Usuario.estado=ACTIVO y Rol.nombre=ADMIN. Comprueba DB_NAME=SIGELAB y la base conectada; no admite TEST_FACE. Solo consulta ID, estado y nombre del rol, sin leer hashes. No inserta usuarios, no ejecuta DDL, no migra ni escribe directamente perfiles.

Después envía `POST /face/enroll` con `userId` y `adminUserId` iguales al ID del ADMIN resuelto y `replace=false`, usando la cabecera X-Facial-Api-Key. El endpoint conserva todas sus comprobaciones, incluyendo revalidación administrativa antes del commit, calidad/liveness, cifrado y auditoría. Si ya existe perfil, se rechaza el intento; este script no ofrece reemplazo automático. El rol/estado pueden cambiar entre el SELECT y la llamada: el endpoint vuelve a validarlos y puede denegar la operación.

La salida muestra únicamente estados e ID del usuario, sin secretos ni cuerpos HTTP. Un timeout o error no confirma registro; revisar status antes de reintentar. Una respuesta satisfactoria confirma el perfil persistido y permite intentar el login normal con contraseña y verificación facial. No repetir enrollment en cada login. La validación de un actor declarado mantiene el límite de confianza local documentado; no constituye autenticación criptográfica del proceso que ejecuta el script.

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

AuditService conserva su política existente: registro general no transaccional de login es de mejor esfuerzo. LOGIN_FALLIDO de contraseña pertenece a UsuarioService; LOGIN_FALLIDO después de credenciales pertenece al servicio completo. LOGIN_EXITOSO solo se solicita después de facial y revalidación. Un fallo al persistir auditoría general puede dejar el evento ausente; no se cambió esa política compartida.

AuditoriaFacial registra inicio, éxito/fallo y causas controladas sin datos biométricos. Score/threshold se guardan al comparar. No se inventan usuarios para eventos que no pueden satisfacer su FK; errores previos a configuración/BD válida tampoco pueden garantizar auditoría SQL. Una caída de persistencia impide completar el resultado facial satisfactoriamente.

## Conexión de feature/facial-admin

No se crearon FacialAdminService, FacialAdminController ni una View administrativa nueva. El responsable debe:

- Tomar adminUserId del usuario autenticado, no de un campo editable.
- Consumir consultarPerfil, registrar(...,false), registrar(...,true) y eliminar; mostrar reemplazo como acción explícita.
- Mantener la revalidación Java incorporada en registrar/eliminar y manejar BusinessException/ErrorComunicacion.
- Ejecutar las operaciones con SwingWorker y refrescar status después del resultado.
- No enviar password/hash ni solicitar la clave AES. No interpretar consultarEstado o HTTP 200 como autenticación.

El panel RegistroFacialPanel existente sigue siendo diagnóstico y no sustituye esa administración.

## Pruebas ejecutadas y validaciones pendientes

Maven fue ejecutado con pruebas unitarias/aisladas. Resultado final: **173 pruebas, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. Se verificaron los reportes de 28 suites: **ninguna clase `*IT` fue ejecutada**. No se accedió a SQL Server ni se ejecutaron cámara, enrollment real o migraciones.

La primera ejecución tuvo 14 errores `java.net.SocketException: Operation not permitted` al crear los servidores HTTP simulados, debido a la restricción de sockets del sandbox. Al repetir con permiso para servidores HTTP locales, las 173 pruebas pasaron **sin cambios de código**. El resultado inicial no confirmó una regresión del contrato HTTP.

El POM contiene una única declaración de Surefire **3.5.5** para JUnit 5, con exclusión explícita `**/*IT.java`. Las pruebas Java validaron de forma aislada la coordinación del login en dos factores, la solicitud de LOGIN_EXITOSO después del facial y la revalidación, los rechazos de acceso, el cliente HTTP simulado, status/enroll/delete y permisos ADMIN. Esto no verifica persistencia SQL ni reconocimiento biométrico real.

**Pytest continúa pendiente:** el entorno disponible solo tiene Python **3.14.8**, mientras que la integración se estandarizó en **Python 3.12.x**; además faltan pytest y varias dependencias. No se ejecutaron pruebas Python ni se preparó un entorno incompatible. Python conserva pruebas de cifrado, pipeline, similitud indirecta/enrollment, coordinación interna y agrega contratos operativos, autorización, transacciones, configuración y proceso simulado. pytest excluye por defecto los cinco módulos que escriben en SQL, antes de importarlos. Esos módulos también tienen skipUnless para unittest. SIGELAB_RUN_SQL_TESTS=1 es una activación deliberada, no autorización automática: solo habilitar tras permiso y con una BD de pruebas dedicada.

La inspección previa al commit confirmó que `.env.docker` es un archivo **local, ignorado y no versionado**. Debe permanecer fuera del commit; no se reproduce su contenido.

La validación real con **SQL Server, cámara, liveness, calibración del threshold y enrollment real sigue pendiente**. FaceID no está completamente validado de extremo a extremo.

Comandos para pruebas aisladas: desde facial-service y con un entorno Python 3.12 preparado, `.venv/bin/python -m pytest`; desde la raíz, `./mvnw test`. Ejecutarlos únicamente dentro de la autorización correspondiente. No usar selección de IT ni ejecutar scripts de registro/preparación sin revisar sus efectos. LoginControllerIT tiene FaceID simulado explícitamente y crea usuarios reales de prueba; está protegido por opt-in y no valida biometría real.

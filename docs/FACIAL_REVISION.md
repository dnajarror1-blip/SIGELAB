# Revisión y estabilización facial del workspace

## Pull Request revisado

- PR y título: no identificados; esta tarea revisa la integración actual del workspace.
- Rama origen informada: origin/feature/facial-auth.
- Rama destino/workspace informada: integration/revision-prs.
- Módulo: autenticación facial e integración del login.
- Referencia facial comunicada por el usuario: 7e7584b; no se comprobó su diff individual. La inspección posterior previa al commit comparó los cambios pendientes con HEAD y consultó el historial reciente.
- Archivos modificados: inventario de esta intervención más abajo.
- La revisión inicial no utilizó Git. Posteriormente se inspeccionaron estado, diferencias e historial mediante operaciones Git de solo lectura. No se realizaron operaciones Git de escritura ni se modificó .git. No se dispone de una revisión completa del diff del merge; no se atribuyen defectos ni archivos nuevos al autor sin evidencia.

## Resumen

Se sustituyó la decisión diagnóstica por un contrato biométrico operativo conectado al login obligatorio. Se conservan Java como aplicación principal, BCrypt, permisos existentes, tablas SIGELAB y formato AES-GCM. FastAPI sigue siendo un servicio local especializado. No se crearon sesiones adicionales ni vistas administrativas.

### 1. Inventario facial encontrado antes de modificar

Esta lista identifica archivos presentes, **no demuestra cuáles introdujo facial-auth**:

- Java: service/FacialAuthClient.java; view/RegistroFacialPanel.java; model/PerfilFacial.java y AuditoriaFacial.java.
- Login y auditoría inspeccionados: controller/LoginController.java, view/LoginForm.java, service/UsuarioService.java, AuditService.java y repository/AuditoriaRepository.java, UsuarioRepository.java, RolRepository.java. El login encontrado no llamaba al cliente facial.
- No se encontró Repository facial Java ni FacialAdminService/FacialAdminController.
- Python app: __init__, main, config, database, crypto, camara_control, facial_repository, perfil_storage, facial_matching, facial_pipeline, facial_enrollment, registro_facial_nuevo, registro_facial_interno y eliminacion_facial_interna.
- Scripts Python: verificar_rostro, registrar_rostro, probar_similitud_real, probar_modelo, probar_liveness, preview_camara, preparar_bd_face, diagnostico_embedding y crear_usuario_face_test.
- Configuración: requirements.txt, requirements-dev.txt y .env.example de raíz; faltaban ejemplo privado del servicio y scripts de setup/arranque Linux. No había migración facial independiente; las tablas ya están en database/sigelab.sql.
- Java tests: FacialAuthClientTest, RegistroFacialPanelTest/Preview, LoginControllerTest/IT, UsuarioAuditoriaTest y pruebas relacionadas de usuarios/auditoría. Python: 14 módulos existentes sobre cifrado, enrollment, almacenamiento, coordinación, eliminación, estados y auditoría.
- No se encontraron .env, .venv, __pycache__, pyc, fotos, embeddings externos, modelos grandes ni claves privadas en las búsquedas realizadas. Hay vectores sintéticos en pruebas, no biometría real. La inspección previa al commit confirmó que .env.docker, que contiene una contraseña local configurada, está ignorado y no versionado; debe permanecer fuera del commit. Su valor nunca se reproduce aquí.

### 2. Archivos cambiados por esta estabilización

Existentes modificados:

- Raíz: pom.xml, .env.example.
- Java principal: service/FacialAuthClient.java, service/UsuarioService.java, repository/UsuarioRepository.java, controller/LoginController.java, view/LoginForm.java.
- Java pruebas: controller/LoginControllerTest.java, controller/LoginControllerIT.java, service/UsuarioAuditoriaTest.java.
- Python app: main.py, config.py, database.py, perfil_storage.py, facial_pipeline.py, facial_enrollment.py.
- Python configuración: requirements.txt, requirements-dev.txt.
- Python script: scripts/crear_usuario_face_test.py (corrección de indentación que impedía analizarlo).
- Python pruebas: test_verificacion_mensajes.py, test_verificacion_estados_api.py, test_verificacion_cambio_estado.py, test_facial_enrollment.py, test_registro_seguridad.py, test_registro_facial_nuevo.py, test_perfil_storage.py, test_auditoria_rechazo_rollback.py, test_eliminacion_facial_sql_rollback.py.

Nuevos:

- Java principal: service/AutenticacionService.java.
- Java pruebas: service/AutenticacionServiceTest.java, FacialContratoTest.java, AdministradorFacialPermisosTest.java.
- Python app: auth_service.py, administracion_repository.py, camara.py, capture_client.py, capture_worker.py.
- Servicio: .env.example, pytest.ini, setup-facial-linux.sh, start-facial.sh, enroll-user.sh, scripts/doctor.py y scripts/enroll_user.py.
- Python pruebas: conftest.py, test_administracion_operativa.py, test_configuracion_operativa.py, test_similitud_operativa.py.
- Documentación: este informe y FACIAL_INTEGRACION.md.

No se cambiaron modelos/Enums compartidos, SQL oficial, permisos, auditoría general, .gitignore, configuración privada ni módulos de otros integrantes.

### 3–7. Arquitectura, cliente, servicio, endpoints y esquema

- Arquitectura: Swing → Controller → AutenticacionService → UsuarioService/Repository/JDBC; biometría mediante FacialAuthClient → HTTP/JSON → FastAPI → Service → Repository/ODBC y proceso interactivo.
- Cliente: resultados tipados y parsing JSON con Gson 2.11.0; valida ID, tipos, HTTP, finitud y coherencia de coincidencia/liveness/score. Conserva adaptadores diagnósticos para consumidores existentes.
- Servicio: operativo sobre la BD configurada, default SIGELAB. Las herramientas históricas TEST_FACE mantienen sus restricciones. No se crean bases ni tablas de usuarios alternativas.
- Endpoints definidos en código: GET /health, GET /face/status/{userId}, POST /face/verify, POST /face/enroll y DELETE /face/profile/{userId}. El cliente Java se verificó contra servidores HTTP simulados; los endpoints Python no se ejercitaron mediante pytest ni contra infraestructura real.
- SQL: usa id_usuario, username, id_rol, JOIN Rol y estado ACTIVO/INACTIVO. No se encontró una incompatibilidad que exigiera cambiar el esquema Usuario. El campo activo de PerfilFacial sí existe y no debe confundirse con Usuario.estado.

### 8–10. Login, fallos y AuditService

1. Swing obtiene credenciales e inicia SwingWorker, sin bloquear el EDT.
2. UsuarioService consulta SQL, valida BCrypt y ACTIVO; los fallos no invocan FaceID.
3. El servicio completo comprueba rol y solicita verify solo con userId.
4. Python exige perfil activo, descifra en memoria y captura seis muestras válidas con calidad y liveness en un proceso independiente.
5. Compara similitud con un threshold explícito; vuelve a comprobar usuario y versión del perfil.
6. Java valida la respuesta y reconsulta estado/rol actuales.
7. Solo entonces solicita LOGIN_EXITOSO y devuelve los datos para abrir el menú en el EDT.

Rostro distinto, perfil ausente, servicio apagado, cámara fallida, modelo/liveness ausente, timeout, cancelación, respuesta inválida o usuario desactivado deniegan acceso. No hay bypass operativo ni enrollment obligatorio en cada login.

UsuarioService conserva LOGIN_FALLIDO de credenciales/estado. AutenticacionService registra LOGIN_FALLIDO después de credenciales válidas y LOGIN_EXITOSO después del segundo factor. AuditService mantiene su política de mejor esfuerzo y sus contratos transaccionales originales. AuditoriaRepository ya consultaba el rol real con JOIN y no requirió cambios.

### 11–12. Tablas faciales y clave

PerfilFacial mantiene un único perfil por usuario mediante UNIQUE, FK real y embedding cifrado. AuditoriaFacial conserva su FK, eventos y métricas. Guardado/eliminación y auditoría exitosa comparten transacción. No se aplicaron migraciones ni se comprobó el contenido de la BD real.

FACIAL_ENCRYPTION_KEY se obtiene únicamente en Python; se conserva el formato AES-GCM SF1 con nonce aleatorio y AAD ligado al usuario. Java solo conoce FACIAL_API_KEY, que es una clave HTTP distinta. Cambiar la clave AES rompe el descifrado de perfiles previos; conservarla privadamente. No se generaron ni guardaron claves durante la tarea.

### 13–16. Arranque, enrollment, eliminación y facial-admin

Procedimiento detallado en FACIAL_INTEGRACION.md: instalar prerrequisitos, crear .venv, instalar requirements, configurar .env, comprobar ODBC/esquema con autorización, diagnosticar modelo/cámara e iniciar FastAPI en 127.0.0.1:8765 con un worker.

Enrollment exige actor ADMIN activo y objetivo ACTIVO, rechaza perfil existente salvo replace=true, revalida antes de persistir y guarda solo el embedding cifrado. Eliminar exige ADMIN activo, permite retirar perfil de objetivo inactivo y conserva auditoría. Las rutas están implementadas, sin ejercicio sobre datos reales.

El bootstrap administrativo enroll-user.sh está preparado para resolver un ADMIN activo existente y llamar a /face/enroll con replace=false, sin crear usuarios ni omitir el segundo factor. No se ejecutó enrollment real.

FacialAdminService/Controller no se inventaron. El Integrante 2 debe consumir consultarPerfil, registrar(id, actor, false/true) y eliminar(id, actor), pasar el actor de la sesión real, usar SwingWorker y refrescar estado. El cliente ya verifica permisos actuales en Java y Python los vuelve a verificar; esto no autentica criptográficamente procesos locales.

### 17. Pruebas existentes y nuevas

Se conservaron pruebas existentes de cifrado, pipeline, preparación de muestras, coordinación y persistencia. Se actualizaron los contratos diagnósticos de verify a matched. Se agregaron pruebas aisladas de login completo, HTTP tipado, autorización Java/Python, similitud, configuración, cambios concurrentes, timeout y rollback.

Las cinco suites Python que escriben en SQL se excluyen de la colección pytest habitual y requieren opt-in también bajo unittest. LoginControllerIT usa un factor facial simulado y requiere opt-in: no demuestra integración biométrica real.

Además del análisis de sintaxis Python mediante AST sin importar módulos, XML del POM y bash -n de los scripts, se ejecutó Maven con pruebas unitarias/aisladas. Resultado final: **173 pruebas, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. Los reportes de 28 suites confirman que **ninguna clase `*IT` fue ejecutada**. Se detectó y corrigió previamente la indentación inválida de crear_usuario_face_test.py:145.

La primera ejecución Maven tuvo 14 errores `java.net.SocketException: Operation not permitted` al crear servidores HTTP simulados por la restricción de sockets del sandbox. Al repetir con permiso para servidores HTTP locales, las 173 pruebas pasaron sin cambios de código. No se confirmó una regresión del contrato HTTP.

Pytest continúa pendiente: el entorno disponible solo tiene Python **3.14.8**, mientras que esta integración requiere **Python 3.12.x**; también faltan pytest y varias dependencias. No se ejecutaron pytest/unittest, SQL Server real, cámara, enrollment real ni migraciones.

## Bloqueantes

### 18. Pendientes para declarar la integración lista

- **Validación de extremo a extremo pendiente.** Archivos: AutenticacionService.java:40, auth_service.py:23 y facial_enrollment.py:51. Evidencia: Java compiló y las 173 pruebas Maven aisladas pasaron, pero pytest no se ejecutó y no se validaron SQL Server, cámara, liveness, threshold ni enrollment real. Impacto: no puede declararse FaceID completamente validado de extremo a extremo. Corrección mínima: disponer de Python 3.12 y sus dependencias para las pruebas aisladas Python; después validar el entorno real por etapas y con autorización específica.
- **Threshold/calibración pendiente.** Archivo: config.py:62 y .env.example del servicio. Evidencia: no existe default ni configuración privada generada. Impacto: verify deniega con error controlado hasta suministrar un valor válido. Corrección mínima: Integrante 8 debe calibrar y configurar FACIAL_MATCH_THRESHOLD; no sustituirlo por un bypass.

Los bloqueantes funcionales iniciales (login sin facial, éxito anticipado, restricción del servicio a TEST_FACE y respuesta exclusivamente diagnóstica) tienen correcciones implementadas. Las pruebas Java aisladas validan la coordinación del login y los contratos del cliente; siguen pendientes las pruebas Python y la validación biométrica real.

La posible inclusión de .env.docker dejó de ser una incertidumbre del commit actual: se confirmó como archivo local ignorado y no versionado. Debe mantenerse excluido; no se modificó ni se reprodujo su contenido. Esta comprobación no es una auditoría de exposiciones históricas.

## Importantes

### 19. Limitaciones y coordinación técnica

- **Dependencias y liveness no verificados.** Archivo: requirements.txt:1 y facial_enrollment.py:51. Se conservan versiones del motor aportadas y su API addons/liveness; no hay evidencia de instalación compatible en este equipo. Impacto: si el motor o complemento no están disponibles, la captura falla y el acceso se deniega. Corrección mínima: Integrante 8 debe comprobar instalación reproducible y liveness real antes de la demostración.
- **Health no prueba cámara.** Archivo: main.py:44. Evidencia: cameraReady/modelLoaded/ready permanecen false por diseño, sin activar dispositivos. Impacto: HTTP 200 o carpeta de modelo presente no demuestran preparación biométrica. Corrección mínima: ejecutar doctor y una captura autorizada.
- **Auditoría general de mejor esfuerzo.** Archivo: AuditService.java:50 aproximadamente, conservado. Evidencia: la persistencia no transaccional captura errores. Impacto: puede faltar un evento general si falla su almacenamiento. Corrección mínima para esta tarea: mantener el contrato y coordinar cualquier cambio de política con #6; no presentarlo como garantía de persistencia.
- **Límite de confianza local.** Archivos: main.py:29 y administracion_repository.py:29. La clave HTTP y el actor declarado no constituyen sesiones ni prueba criptográfica del proceso local. Impacto: procesos locales con acceso a esa clave están dentro del límite de confianza. Corrección mínima: mantener localhost, permisos de archivos y el alcance académico documentado.

### 20. Coordinación con Integrantes 2 y 8

- Integrante 2: adaptar facial-admin a firmas/JSON documentados; conservar LOGIN_EXITOSO después de FaceID; revisar que ninguna integración futura use autenticar de UsuarioService como login completo; conectar UI administrativa sin crear sesiones Python.
- Integrante 8: confirmar versiones de Python/motor/ODBC, modelos y complemento liveness, calibración de threshold, captura Linux y validaciones reales. Conservar clave AES y formato persistido.
- Integrantes 2/8 e integración: validar enrollment del primer usuario desde administración autorizada; el login no registra automáticamente ni permite entrar sin perfil. Verificar el flujo inicial con un actor ADMIN existente sin añadir un acceso de excepción.

## Opcionales

No se proponen cambios de estilo ni rediseño global. El panel diagnóstico existente puede actualizar sus textos cuando se integre la administración; sus respuestas ya no deben presentarse como autenticación final.

## Pruebas

- Encontradas: suites Java y 14 módulos Python originales; pruebas nuevas descritas arriba.
- Verificadas: compilación Java y 173 pruebas Maven aisladas, con 0 failures, 0 errors, 0 skipped y BUILD SUCCESS; también sintaxis estática Python/XML y sintaxis Bash. Ninguna clase *IT fue ejecutada.
- Cobertura Java aislada: login en dos factores con dobles, LOGIN_EXITOSO después del facial y la revalidación, rechazo de matched=false, error de comunicación simulado, perfil ausente mediante HTTP simulado, status/enroll/delete y ADMIN activo/no ADMIN/ADMIN inactivo. No verifica persistencia SQL ni biometría real.
- Incidencia de ejecución: 14 errores iniciales de sockets del sandbox; repetición con permiso para servidores HTTP locales satisfactoria, sin cambios de código.
- No verificadas: pruebas Python; el entorno disponible tiene Python 3.14.8 y se requiere Python 3.12.
- Pendientes: preparar el entorno Python 3.12 y ejecutar pytest aislado dentro de la autorización correspondiente; validar SQL Server, cámara, modelos, liveness, threshold y enrollment real con permisos específicos. FaceID no está completamente validado de extremo a extremo.
- SQL de pruebas: algunas suites crean/modifican registros y hacen rollback; otras IT Java crean usuarios. No ejecutarlas sobre datos reales.

## Riesgos de integración

El cambio de significado de UsuarioService.autenticar exige que futuros consumidores utilicen el servicio completo para iniciar sesión. Se revisaron los consumidores Java principales: LoginController ahora lo hace. Los records/métodos nuevos de FacialAuthClient y el cuerpo con adminUserId son los contratos a coordinar con facial-admin. El POM utiliza Gson 2.11.0 y una sola declaración de Surefire **3.5.5**, con exclusión explícita `**/*IT.java`; la compilación y las pruebas Maven aisladas fueron satisfactorias. No se modificaron tablas, modelos compartidos, auditoría general ni otros módulos.

## Veredicto

**NO FUSIONAR TODAVÍA.** La compilación Java y las 173 pruebas Maven aisladas fueron satisfactorias. Continúan pendientes pytest con Python 3.12, la validación de dependencias Python y la validación real con SQL Server, cámara, liveness, threshold y enrollment. FaceID no está completamente validado de extremo a extremo. .env.docker está confirmado como local, ignorado y no versionado, y debe permanecer fuera del commit. La decisión final es del usuario.

## Comentarios sugeridos para GitHub

- «La integración exige ambos factores y registra LOGIN_EXITOSO después del resultado facial. Maven finalizó con 173 pruebas, 0 failures, 0 errors, 0 skipped y BUILD SUCCESS, sin ejecutar *IT. Continúan pendientes pytest con Python 3.12 y la validación real de cámara, liveness, threshold, enrollment y SQL Server.»
- «facial-admin debe enviar adminUserId de la sesión Java en enroll/delete y utilizar replace=true solo tras una acción explícita. Ambas capas verifican ADMIN activo por la relación real con Rol.»
- «Confirmar con el responsable facial las dependencias/liveness y el threshold calibrado. La ausencia o el fallo de biometría mantienen el acceso denegado.»

Comentarios redactados para publicación manual; no se publicó ninguno.

# SIGELAB — Desarrollo y pruebas de reconocimiento facial (Integrante 8)

| Dato | Información |
| --- | --- |
| Proyecto | Sistema de Gestión de Laboratorios — SIGELAB |
| Responsable | **Wilson Valey** |
| Integrante | **8** |
| Usuario Git | `wilgiof` |
| Rama de desarrollo | `feature/facial-auth` |
| Último commit de la rama revisada | `94a7854` |
| Período de aportaciones identificado | 26 al 30 de octubre de 2026 |
| Versión de integración consultada | `8ac0a02` (`integration/revision-prs`) |
| Fecha del informe | 30 de octubre de 2026 |

## 1. Propósito y alcance

Este documento registra las aportaciones técnicas identificadas en la rama `feature/facial-auth` y las pruebas de seguridad realizadas por **Wilson Valey, Integrante 8**, para el reconocimiento facial de SIGELAB. Incluye el servicio Python, su integración con Java, controles de seguridad, scripts de diagnóstico, resultados de pruebas y pendientes.

Se diferencian tres niveles de evidencia:

1. **Implementación identificada:** archivos presentes y mensajes de commits del integrante.
2. **Prueba ejecutada:** resultado observable de una ejecución realizada durante la revisión.
3. **Pendiente:** funcionalidad que requiere validación adicional, especialmente con dispositivos y SQL Server reales.

Los 47 archivos citados más adelante son diferencias entre `feature/facial-auth` y el ancestro común con `origin/develop`. Por sí sola, esa comparación **no certifica la autoría línea por línea** ni implica que todos los archivos hayan sido creados desde cero.

## 2. Objetivos

- Incorporar componentes de captura, registro y verificación facial.
- Añadir mecanismos de liveness y comparación biométrica.
- Proteger perfiles con cifrado y controles de almacenamiento.
- Verificar el estado de usuarios y evitar operaciones no autorizadas.
- Mantener registros de auditoría y controlar errores transaccionales.
- Facilitar la comunicación entre el servicio Python y la aplicación Java.
- Crear pruebas repetibles y herramientas de diagnóstico.

## 3. Tecnologías y componentes

- **Python 3.12.10:** intérprete comprobado en el entorno de pruebas de Wilson.
- **FastAPI:** API HTTP del servicio facial.
- **Java/Swing:** cliente y panel para la aplicación principal.
- **SQL Server/ODBC:** almacenamiento previsto de usuarios, perfiles y auditoría.
- **AES-GCM:** mecanismo de cifrado utilizado para proteger datos biométricos.
- **`unittest`, `unittest.mock` y `TestClient`:** pruebas de unidades y solicitudes HTTP simuladas.
- **Git/GitHub:** control de versiones y coordinación con la integración del equipo.

### 3.1. Componentes Python del servicio

Se identificaron en la rama los archivos `app/__init__.py`, `camara_control.py`, `config.py`, `crypto.py`, `database.py`, `eliminacion_facial_interna.py`, `facial_enrollment.py`, `facial_matching.py`, `facial_pipeline.py`, `facial_repository.py`, `main.py`, `perfil_storage.py`, `registro_facial_interno.py` y `registro_facial_nuevo.py`.

Sus responsabilidades, según los nombres y el historial de cambios, se agrupan en:

| Área | Componentes relevantes | Propósito |
| --- | --- | --- |
| API y configuración | `main.py`, `config.py` | Exponer operaciones y parámetros del servicio |
| Persistencia y cifrado | `database.py`, `facial_repository.py`, `perfil_storage.py`, `crypto.py` | Proteger y recuperar información facial |
| Reconocimiento | `facial_matching.py`, `facial_pipeline.py`, `facial_enrollment.py` | Comparación, procesamiento y preparación de muestras |
| Operaciones internas | `registro_facial_interno.py`, `registro_facial_nuevo.py`, `eliminacion_facial_interna.py` | Coordinación de registro y eliminación |
| Cámara | `camara_control.py` | Control del recurso de captura |

El historial registra mejoras de rendimiento para reutilizar el modelo facial y proteger inferencias, así como cambios para revalidar estados durante la verificación.

### 3.2. Integración Java

Se identificaron `src/main/java/sigelab/service/FacialAuthClient.java` y `src/main/java/sigelab/view/RegistroFacialPanel.java`. Los commits describen la creación del cliente Java, un panel de diagnóstico y la posibilidad de seleccionar usuarios en dicho panel.

También se identificaron `FacialAuthClientTest.java`, `FaceIdDatabaseIsolationIT.java`, `RegistroFacialPanelTest.java` y `RegistroFacialPanelPreview.java`. La presencia de estas clases no equivale a haber ejecutado todas las pruebas Java en el equipo de Wilson.

### 3.3. Scripts de diagnóstico

La rama incluye `crear_usuario_face_test.py`, `diagnostico_embedding.py`, `preparar_bd_face.py`, `preview_camara.py`, `probar_liveness.py`, `probar_modelo.py`, `probar_similitud_real.py`, `registrar_rostro.py` y `verificar_rostro.py`.

Estas herramientas facilitan diagnósticos del modelo, de liveness, cámara, similitud y operaciones faciales. Los scripts que consultan o modifican SQL deben ejecutarse **solo con una base de pruebas aislada y autorización**.

## 4. Seguridad y confiabilidad

Los commits registran trabajo relacionado con:

- Cifrado AES-GCM y pruebas de seguridad criptográfica.
- Registro facial protegido y pruebas transaccionales.
- Auditoría de rechazos ante cambios de estado.
- Revalidación del estado del usuario después de la captura.
- Validación de identificadores y estados inválidos.
- Coordinación de cámara y prevención de captura sin autorización.
- Tratamiento de mensajes de liveness y resultados de verificación.
- Eliminación facial interna y pruebas de reversión.

La rama también modifica `.gitignore` e incorpora archivos de dependencias (`requirements.txt` y `requirements-dev.txt`). Las claves de cifrado, credenciales y archivos `.env` privados deben permanecer fuera del repositorio.

## 5. Inventario de pruebas incorporadas

La comparación con `develop` identifica quince módulos de pruebas Python:

| Categoría | Archivos |
| --- | --- |
| Cifrado y seguridad | `test_crypto_seguridad.py`, `test_seguridad_api_face.py`, `test_registro_seguridad.py`, `test_registrar_rostro_seguro.py` |
| Registro facial | `test_facial_enrollment.py`, `test_registro_facial_interno.py`, `test_registro_facial_nuevo.py` |
| Eliminación y auditoría | `test_eliminacion_facial_interna.py`, `test_eliminacion_facial_sql_rollback.py`, `test_auditoria_rechazo_rollback.py` |
| Procesamiento y almacenamiento | `test_facial_pipeline_cache.py`, `test_perfil_storage.py` |
| Verificación y estados | `test_verificacion_cambio_estado.py`, `test_verificacion_estados_api.py`, `test_verificacion_mensajes.py` |

## 6. Resultados verificados

### 6.1. Suite Python de `feature/facial-auth`

En la revisión previa del entorno local se informó una ejecución del conjunto habitual de `unittest` con **70 pruebas satisfactorias**. Este resultado describe aquella ejecución y **no significa** que se haya validado una instalación completa con SQL Server y cámara física.

### 6.2. Suite administrativa de la versión integrada

Desde el worktree de revisión de `integration/revision-prs` se ejecutó:

```powershell
& "C:\Users\Wilson\Proyectos\SIGELAB\facial-service\.venv\Scripts\python.exe" -m unittest discover -s tests -p "test_administracion_operativa.py" -v
```

**Resultado observado:** `Ran 7 tests in 0.020s` y `OK`.

Las pruebas abarcaron requisitos de administrador activo, rechazo antes de captura, uso de `replace`, parámetros de registro, revalidación para eliminación y rollback ante fallos simulados de auditoría. **Este archivo pertenece a la integración revisada; no se atribuye su autoría a Wilson sin evidencia adicional.**

### 6.3. Acceso sin clave y con clave incorrecta

Se usó FastAPI `TestClient` con una clave ficticia configurada únicamente en el proceso de prueba. Se interceptó el ejecutor para comprobar que no se invocaran operaciones sensibles.

| Solicitud | Condición | HTTP obtenido |
| --- | --- | --- |
| `POST /face/enroll` | Sin clave API | 401 |
| `DELETE /face/profile/8` | Sin clave API | 401 |
| `POST /face/enroll` | Clave API incorrecta | 401 |
| `DELETE /face/profile/8` | Clave API incorrecta | 401 |

**Resultado:** cuatro solicitudes rechazadas; no se ejecutaron las operaciones simuladas.

### 6.4. Restricciones administrativas

Se simuló el repositorio SQL con `unittest.mock.MagicMock`, sin conexión a una base real:

| Rol y estado simulados | Registro facial | Eliminación facial |
| --- | --- | --- |
| `ENCARGADO` / `ACTIVO` | 403 | 403 |
| `ADMIN` / `INACTIVO` | 403 | 403 |

**Resultado:** cuatro rechazos `403`; cero capturas invocadas; cero commits simulados; cuatro rollbacks simulados.

Estas pruebas verifican las restricciones implementadas en el código integrado evaluado. No validan por sí solas la autenticidad criptográfica de la identidad administrativa.

### 6.5. Protección de cámara

Los commits `33fcc90` y `94a7854` incorporaron verificaciones de acceso no autorizado y de protección de cámara. Durante la revisión se confirmó mediante simulación que una operación administrativa rechazada no invocaba la captura. **No se abrió la cámara físicamente** en estas pruebas.

## 7. Historial de commits del Integrante 8

Se identificaron **19 commits** bajo el autor Git `wilgiof`:

| Fecha | Commit | Aportación registrada |
| --- | --- | --- |
| 2026-10-26 | `68fd75b` | Diagnóstico facial con liveness y auditoría |
| 2026-10-26 | `5c11b98` | Registro facial protegido y pruebas transaccionales |
| 2026-10-26 | `8053ad0` | Pruebas de usuarios inactivos e identificadores inválidos |
| 2026-10-26 | `8829532` | Pruebas HTTP y dependencias de desarrollo |
| 2026-10-26 | `d99ffd8` | Revalidación de estado después de captura |
| 2026-10-26 | `0e00b3a` | Restauración de auditoría y mensajes de liveness |
| 2026-10-26 | `1f9de27` | Auditoría de rechazo por cambios de estado |
| 2026-10-26 | `378c9e7` | Uso de registro facial protegido |
| 2026-10-26 | `0c74515` | Pruebas de cifrado AES-GCM |
| 2026-10-26 | `6b0e65f` | Cliente Java para el servicio facial |
| 2026-10-27 | `41bd127` | Reutilización de modelo y protección de inferencias |
| 2026-10-27 | `c4864f8` | Pruebas de preparación de registro facial |
| 2026-10-27 | `7e0076d` | Registro facial interno seguro |
| 2026-10-27 | `524951c` | Panel Java de diagnóstico y pruebas |
| 2026-10-27 | `08bb55a` | Selección de usuario en panel Java |
| 2026-10-27 | `939e4bd` | Eliminación facial interna y pruebas |
| 2026-10-27 | `7e7584b` | Prueba de eliminación transaccional en SQL Server |
| 2026-10-30 | `33fcc90` | Pruebas de acceso no autorizado a endpoints |
| 2026-10-30 | `94a7854` | Prueba para impedir cámara sin autorización |

La lista procede del comando de Git `log --format="%h | %an | %ad | %s" --date=short origin/develop..HEAD` ejecutado en `feature/facial-auth`.

## 8. Coordinación con el equipo

Los cambios de `feature/facial-auth` se revisaron también en `integration/revision-prs`, versión `8ac0a02`, que contiene trabajo posterior de estabilización e integración de otros integrantes. En esa versión existen los documentos `docs/FACIAL_REVISION.md` y `docs/FACIAL_INTEGRACION.md`.

El presente informe **complementa** esos documentos; no pretende sustituirlos ni atribuir a Wilson cambios de terceros. Durante la revisión se utilizó un worktree separado y comandos Git de lectura, sin modificar la rama de integración.

## 9. Limitaciones y pruebas pendientes

- **Permisos temporales:** verificar si existen permisos firmados, con vencimiento y alcance por operación y usuario. En el código `8ac0a02` revisado se comprobó clave API compartida y validación de ADMIN activo; no se verificó un permiso temporal con alcance limitado.
- **SQL Server real:** no se ejecutaron las pruebas que escriben perfiles o auditoría sobre datos reales en la revisión aquí descrita.
- **Cámara y liveness:** requieren pruebas físicas autorizadas, con modelo y complementos disponibles.
- **Calibración:** falta registrar resultados que justifiquen el umbral de similitud utilizado.
- **Integración extremo a extremo:** queda por comprobar el flujo completo de Java, FastAPI, cámara y SQL Server en un entorno controlado.
- **Pruebas Java del integrante:** los archivos están presentes, pero no se adjunta en este informe una ejecución propia confirmada de esas clases.

No se debe considerar una función completamente validada solo porque exista su código o sus pruebas.

## 10. Conclusiones

El trabajo identificado de **Wilson Valey, Integrante 8**, comprende desarrollo Python y Java, protección de información biométrica, operaciones de registro y eliminación, validaciones de estados, auditoría, optimización del procesamiento y una colección de pruebas automatizadas.

El historial de Git evidencia **19 commits** de trabajo entre el 26 y el 30 de octubre de 2026. La comparación con `develop` identifica **47 archivos diferentes** en la rama; su atribución individual debe interpretarse junto con los commits.

Los resultados de las pruebas ejecutadas fueron satisfactorios dentro del alcance descrito. Se observaron rechazos `401` y `403` apropiados, siete pruebas administrativas satisfactorias en la integración y una ejecución previa de 70 pruebas Python en la rama de desarrollo.

**Estado final:** aportaciones desarrolladas y pruebas parciales satisfactorias. Permanecen pendientes la validación física y de extremo a extremo, la calibración y la confirmación de los permisos temporales.

---

**Elaborado por:** Wilson Valey — Integrante 8  
**Proyecto:** SIGELAB  
**Fecha:** 30 de octubre de 2026

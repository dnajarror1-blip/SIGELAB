# Estabilización de Mantenimiento y Auditoría

Fecha: 2026-10-29. Se inspeccionó y corrigió el estado actual del workspace; no se ejecutó ninguna operación Git. La autorización expresa de esta tarea permite modificar archivos, como excepción a la revisión de solo lectura de AGENTS.md.

## Pull Request revisado

- PR y título: no proporcionados; esta tarea revisa la implementación integrada, no un diff de PR.
- Rama de trabajo: `integration/revision-prs`, según el usuario, sin verificar mediante Git.
- Rama origen/destino de PR, autor y commits: no determinados ni revisados.
- Módulo: Mantenimiento y Auditoría; integración puntual de eventos con Usuarios.
- Responsabilidad: integrante #6; Usuarios corresponde al #2 según `RESPONSABILIDADES.md`.

### Archivos editados o agregados por esta tarea

Producción, bajo `src/main/java/sigelab/`:

- `service/MantenimientoService.java`
- `service/AuditService.java`
- `service/UsuarioService.java`
- `repository/MantenimientoRepository.java`
- `repository/AuditoriaRepository.java`
- `repository/UsuarioRepository.java`
- `controller/MantenimientoController.java` (nuevo)
- `controller/AuditoriaController.java` (nuevo)
- `view/MantenimientoView.java`
- `view/AuditoriaView.java`
- `view/MenuPrincipalForm.java`
- `view/OperacionModulo.java` (nuevo)

Pruebas, bajo `src/test/java/sigelab/`:

- `service/MantenimientoServiceTest.java`
- `service/MantenimientoTransaccionTest.java` (nuevo)
- `service/MantenimientoAuditoriaPermisosTest.java` (nuevo)
- `service/UsuarioAuditoriaTest.java` (nuevo)
- `service/AuditoriaServiceTest.java`
- `service/AuditServiceTransaccionalTest.java`
- `service/MantenimientoSqlIT.java`
- `service/MantenimientoRollbackSqlIT.java`
- `repository/MantenimientoPersistenciaTest.java` (nuevo)
- `view/MantenimientoAuditoriaViewTest.java` (nuevo)

Documentación: este archivo. La lista identifica las escrituras de esta tarea, no atribuye cambios anteriores del workspace.

## Resumen

Fuentes consultadas: `TEAM_CONTRACT.md`, `RESPONSABILIDADES.md`, `DECISIONES.md`, modelo ER, diccionario de datos, esquema SQL, guía oficial SIGELAB (especialmente 6.9, 6.10, RN-14, RN-15 y RN-18) y guía facial. Se conserva la arquitectura View → Controller → Service → Repository → JDBC.

La apertura permite DANADO y el preventivo desde DISPONIBLE; rechaza PRESTADO, MANTENIMIENTO, BAJA y órdenes abiertas preexistentes, incluso si el estado del equipo estuviera desincronizado. La creación de la orden, cambio específico de estado y auditoría utilizan una misma conexión con commit/rollback.

El cierre requiere ABIERTO, un actor activo autorizado y equipo actualmente en MANTENIMIENTO. REPARADO devuelve a DISPONIBLE; NO_REPARABLE pasa a BAJA. Se guarda fecha de salida, resultado, estado y observaciones. Un cierre ya confirmado no puede repetirse. El UPDATE exige además `estado = 'ABIERTO'` y verifica filas afectadas.

Las operaciones de mantenimiento utilizan SERIALIZABLE, igual que la creación de préstamos. Las lecturas del equipo y de las órdenes abiertas quedan protegidas hasta completar la transacción. Dos aperturas/cierres en competencia pueden provocar que SQL Server aborte una transacción por interbloqueo; el servicio revierte el intento y presenta un mensaje seguro. No hay reintento automático de escrituras. Las pruebas aisladas verifican la configuración y la reversión, no simulan el gestor de bloqueos real.

Se comprueban los límites SQL de descripción y observaciones acumuladas. No se activa autoCommit en finally: se cierra la conexión con try-with-resources y se preservan fallos de rollback como excepciones suprimidas.

Los controladores reciben el usuario de la sesión. Las mutaciones revalidan el rol real en BD dentro de la transacción. ADMIN y ENCARGADO pueden gestionar mantenimiento; solo ADMIN consulta auditoría; ESTUDIANTE no recibe esos accesos. Se reutiliza PermisoService sin modificar el contrato de roles ni asumir IDs de rol.

El menú incorpora ambas pantallas según sus permisos. Las cargas y escrituras JDBC se ejecutan mediante SwingWorker, con actualizaciones de componentes en EDT y botones deshabilitados durante la operación. No se introdujo CardLayout ni un rediseño general.

## Bloqueantes

No se encontraron bloqueantes pendientes mediante inspección y pruebas aisladas. Se corrigieron estos riesgos funcionales:

| Archivo y línea actual aproximada | Evidencia previa e impacto | Corrección aplicada |
|---|---|---|
| `service/MantenimientoService.java:73`, `:138`, `:201` | Lecturas sin protección transaccional frente a concurrencia; dos aperturas/cierres podían validar el mismo estado y sobrescribirse. | SERIALIZABLE, consulta de orden abierta, verificación del estado real al cerrar y rollback con la misma conexión. |
| `repository/MantenimientoRepository.java:76`, `:120` | UPDATE por ID sin comprobar estado ni filas; faltaba detección explícita de otra orden abierta. | Cierre condicionado a ABIERTO, filas afectadas y consulta parametrizada de orden abierta. |
| `service/AuditService.java:26`, controladores nuevos | Vistas invocaban servicios sin comprobar permisos del actor. | Controladores con sesión y validación del usuario activo/rol real reutilizando el mapa actual de permisos. |

## Importantes

Corregidos:

- `view/MenuPrincipalForm.java:82`: las pantallas existían pero faltaban accesos desde el menú. Se agregaron según permisos.
- `service/UsuarioService.java:101`, `:128`, `:141`: faltaban eventos de acceso y cambio de estado. Se agregaron; estado del usuario y auditoría se confirman o revierten conjuntamente.
- `service/AuditService.java:119`: un error de consulta se convertía en historial vacío. Ahora propaga DataAccessException y la pantalla informa del fallo.
- `view/MantenimientoView.java:113`, `view/AuditoriaView.java:117`: JDBC se ejecutaba en EDT. Ahora utiliza trabajadores de Swing; los errores técnicos no se copian a los diálogos.
- `repository/MantenimientoRepository.java:28`: se aceptaba una inserción sin obtener su ID. Ahora se rechaza para que el servicio revierta y no registre una referencia incorrecta.

Pendiente de validación: comportamiento del aislamiento en SQL Server 2022 y comprobación visual interactiva. No se presentan como pruebas ejecutadas.

## Opcionales

No se proponen refactorizaciones adicionales de interfaz.

## Eventos y dependencias

| Evento | Estado |
|---|---|
| LOGIN_EXITOSO | Registrado al validar las credenciales actuales en UsuarioService. |
| LOGIN_FALLIDO | Registrado para credenciales rechazadas o usuario inactivo. Identidad no validada: actor y referencia nulos. No se registra contraseña, hash ni el texto introducido. |
| PRESTAMO_CREADO | Existía y se mantiene en la transacción de PrestamoService. |
| EQUIPO_DEVUELTO | Pendiente del módulo funcional de Devoluciones. No se implementó ese flujo. |
| EQUIPO_ENVIADO_MANTENIMIENTO | Sustituye MANTENIMIENTO_INICIO para nuevos eventos. Incluye ID de orden, equipo y actor. |
| MANTENIMIENTO_FINALIZADO | Sustituye MANTENIMIENTO_REPARADO/EQUIPO_BAJA; descripción con resultado y equipo. |
| USUARIO_DESACTIVADO | Nombre exacto exigido por la guía oficial §6.10 para el evento llamado USUARIO_DESHABILITADO en la solicitud. Incluye actor y usuario afectado; usa transacción compartida. |

Todos los eventos se guardan con fecha/hora y entidad/referencia. Los históricos con nombres anteriores permanecen intactos y se pueden consultar por su nombre original.

Se conserva el contrato existente de registro no crítico de AuditService: si falla la persistencia del log de acceso, emite advertencia genérica y no cambia el resultado de autenticación. No se garantiza persistencia durante una caída de auditoría. El registro transaccional de mantenimiento, préstamos y cambios de estado sí propaga el fallo para rollback.

FaceID permanece pendiente. Al integrar el segundo factor, LOGIN_EXITOSO deberá emitirse después del éxito de ambos factores, evitando duplicar el evento actualmente emitido tras validar credenciales. No se implementó biometría ni se registran datos faciales.

## Pruebas

Resultado final ejecutado: **138 pruebas, 0 fallos, 0 errores, 0 omitidas**, Maven offline y Java 21, con Swing headless. Se seleccionaron explícitamente clases aisladas; ninguna prueba ejecutada abrió conexiones SQL Server.

```sh
mvn -o -Djava.awt.headless=true -Dtest=MantenimientoServiceTest,MantenimientoTransaccionTest,MantenimientoPersistenciaTest,MantenimientoAuditoriaPermisosTest,UsuarioAuditoriaTest,AuditoriaServiceTest,AuditServiceTransaccionalTest,PrestamoServiceTest,PrestamoTransaccionTest,PermisoServiceTest,UsuarioServiceTest,LoginControllerTest,ReporteServiceTest,QrEquipoServiceTest,PrestamoQrTest,QrReportesPermisosTest,PrestamoPermisosTest,DashboardPanelTest,MantenimientoAuditoriaViewTest test
```

Cobertura agregada: apertura correctiva y preventiva; estados inválidos; segundo mantenimiento abierto; ambos cierres; cierre repetido; estado real inconsistente; rollback ante fallo de orden, equipo, auditoría o commit; conexión compartida; permisos por nombre con IDs distintos; IDs generados; UPDATE de cero filas; cierre de recursos; límites de columnas; eventos de login y desactivación; rollback de desactivación; apertura/carga de paneles sin bloquear EDT.

Regresiones verificadas: CP-07, pruebas seleccionadas de Préstamos, QR, roles, Usuarios, Reportes y Dashboard. Compilaron también las clases IT, pero no se ejecutaron.

Las pruebas SQL de mantenimiento existentes usan datos mutables de `SIGELAB_TEST_MAINT`. Se actualizaron los nombres de eventos y la inyección de fallo de auditoría del cierre: un actor inexistente ahora falla por autorización antes de llegar al INSERT, por lo que ya no demuestra rollback después de actualizar las tablas.

Pendiente, exclusivamente en base de pruebas y con autorización: ejecutar las IT, competir con dos conexiones al abrir/cerrar/prestar, verificar una sola orden abierta y un solo cierre auditado, validar rollback tras fallo real de JDBC y confirmar conteos e historiales con datos persistidos. Pendiente además inspeccionar visualmente los diálogos de apertura/cierre y los accesos del menú por rol.

## Riesgos de integración

EquipoRepository permanece intacto: la edición general no escribe estado y mantenimiento usa actualizarEstado con la conexión transaccional. PrestamoService y Reportes no se modificaron. Los conteos e historial de mantenimiento consultan los estados/tablas canónicos; el historial de equipo existente muestra estado actual y préstamos/devoluciones, mientras las intervenciones se consultan en el historial de mantenimiento separado.

No se añadieron dependencias Maven, enums, tablas ni migraciones. La exclusión de órdenes duplicadas se protege en el servicio con aislamiento; el esquema actual no contiene un índice único filtrado que impida escrituras SQL externas fuera del flujo controlado.

Los cambios de Usuarios son necesarios para trazabilidad y deben revisarse con su responsable. La API heredada EquipoService.actualizarEstado no es utilizada por el controller/vista actuales ni por mantenimiento; una futura reutilización deberá coordinar sus transiciones con los servicios transaccionales.

## Veredicto

**NO FUSIONAR TODAVÍA** como recomendación de validación: los problemas identificados están corregidos y las pruebas aisladas pasan, pero falta comprobar las transacciones concurrentes con SQL Server y el flujo visual completo. No se evaluó un diff de PR ni se afirma aprobación global de la rama.

## Comentarios sugeridos para GitHub

«Se corrigieron concurrencia, permisos y auditoría transaccional de mantenimiento. Pasan 138 pruebas aisladas, incluida CP-07. Antes de fusionar, falta validar concurrencia/rollback en SQL Server de pruebas y recorrer los diálogos por rol. EQUIPO_DEVUELTO y el registro del éxito después del segundo factor quedan sujetos a integrar Devoluciones y FaceID, respectivamente.»

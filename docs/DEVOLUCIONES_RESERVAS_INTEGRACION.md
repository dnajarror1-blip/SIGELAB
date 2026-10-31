# Integrante 5 — Devoluciones y Reservas

Fecha: 2026-10-30. Implementación compilada con `javac` 21 y validada mediante Maven offline autorizado: 278 casos únicos aprobados, incluidos 84 casos nuevos y 194 de regresión. No se utilizó SQL Server real ni se realizaron operaciones Git. No se verificó la rama mediante Git; su procedencia es la indicada por el usuario.

## Estado inicial

Se inspeccionaron los contratos y documentación disponibles, fuentes Java de producción/pruebas, esquema y seeds, navegación y fronteras con el servicio facial. Se utilizó el prompt y sus ajustes aprobados como contrato funcional; no se buscaron referencias privadas ausentes.

- Existían `Devolucion`, `Reserva`, `Prestamo`, `DetallePrestamo` y sus tablas. No existían Repository/Service/Controller/View de Devoluciones o Reservas.
- No existe `DetalleDevolucion` ni columna de devolución en `DetallePrestamo`. El detalle pendiente se representa mediante ausencia de fila en `Devolucion`.
- `UQ_Devolucion_DetallePrestamo` ya impide devoluciones repetidas. `UQ_DetallePrestamo_PrestamoEquipo` impide repetir equipo dentro del mismo préstamo.
- Condiciones: `BUENO`, `DANADO`. Equipo: `DISPONIBLE`, `PRESTADO`, `DANADO`, `MANTENIMIENTO`, `BAJA`. Préstamo: `ACTIVO`, `FINALIZADO`. Reserva: `ACTIVA`, `CANCELADA`, `COMPLETADA`.
- Reserva referencia un usuario y un equipo, no un laboratorio ni una cantidad; sus intervalos son fechas `DATE`. No hay relación persistida Reserva → Préstamo, estado APROBADA ni contrato para la transición a COMPLETADA.
- `AuditService.registrar(..., Connection)` ya proporciona auditoría transaccional. Las acciones se identifican por cadenas; no existe enum de acciones que haya que ampliar.
- `PermisoService.modulosDe(Rol)` centraliza el mapa por nombre de rol. `RESERVAS_PRESTAMOS` está permitido para ADMIN y ENCARGADO; ESTUDIANTE solo consulta catálogos.
- Préstamos consulta el rol activo mediante su Repository y decide mediante PermisoService. Mantenimiento usa un auxiliar vigente de AuditService que delega en ese mismo mapa. Usuarios revalida identidad y rol mediante sus contratos actuales.
- Reportes calcula detalles pendientes e historial con las tablas canónicas y `NOT EXISTS Devolucion`. Dashboard usa esas consultas. El menú solo incluía Préstamos para este permiso.

## Archivos de producción

Creados, bajo `src/main/java/sigelab/`:

- `repository/DevolucionRepository.java`
- `service/DevolucionService.java`
- `controller/DevolucionController.java`
- `view/DevolucionView.java`
- `repository/ReservaRepository.java`
- `service/ReservaService.java`
- `controller/ReservaController.java`
- `view/ReservaView.java`

Modificados:

- `repository/PrestamoRepository.java`: lectura transaccional con bloqueo para devolución y cierre condicionado a ACTIVO sin pendientes; se conservan métodos previos.
- `repository/DetallePrestamoRepository.java`: consulta por detalle y consulta de pendientes con conexión proporcionada.
- `repository/RolRepository.java`: consulta mínima del rol de un usuario ACTIVO con conexión proporcionada, sin cargar contraseñas.
- `service/PermisoService.java`: validación de acceso usando esa consulta y el mapa de permisos existente, sin duplicar nombres o IDs de roles.
- `view/MenuPrincipalForm.java`: entradas Devoluciones y Reservas bajo `RESERVAS_PRESTAMOS`, recibiendo la identidad de sesión.

Documentación nueva: este informe. No se modificaron modelos, enums, esquema, seeds, Maven, configuración JDBC, PrestamoService, MantenimientoService, AuditService, Reportes, Dashboard, QR ni FaceID.

## Devoluciones

Arquitectura: `DevolucionView → DevolucionController → DevolucionService → Repository → JDBC`.

Contrato de escritura: `registrar(idPrestamo, Map<idDetalle, CondicionDevolucion>, observaciones, actor)`. Una entrada por detalle; un lote puede combinar condiciones. La identidad proviene del Controller de sesión; la fecha se toma del reloj del servicio, no de campos editables. El constructor de prueba permite inyectar reloj, repositorios y fábrica de conexión.

Cada operación de escritura:

1. Abre una conexión explícita con aislamiento SERIALIZABLE y autoCommit=false.
2. Revalida el actor ACTIVO y autorizado mediante PermisoService usando esa misma conexión.
3. Lee el encabezado con `UPDLOCK, HOLDLOCK`; exige ACTIVO. Este bloqueo coordina también devoluciones concurrentes de distintos detalles del mismo préstamo.
4. Comprueba existencia y pertenencia de todos los detalles, ausencia de devolución y equipos actualmente PRESTADO. Valida todo el lote antes de escribir y ordena los equipos de forma consistente.
5. Inserta una devolución por detalle y actualiza su equipo: BUENO → DISPONIBLE; DANADO → DANADO.
6. Registra EQUIPO_DEVUELTO por devolución mediante AuditService con conexión compartida.
7. Reconsulta pendientes y finaliza solamente cuando no queda ninguno. El UPDATE de cierre comprueba nuevamente estado ACTIVO, ausencia de pendientes y filas afectadas.
8. Confirma al terminar. Cualquier excepción SQL o de ejecución revierte inserciones, estados, encabezado y auditorías.

El orden de auditoría y cierre no cambia la atomicidad: ambos participan antes del único commit. No se agrega marca al detalle; la nueva fila Devolucion es su marca canónica. La doble devolución se rechaza en Service y la restricción UNIQUE existente actúa como defensa adicional.

Las devoluciones parciales conservan ACTIVO y los demás equipos PRESTADO. El último detalle devuelto finaliza el préstamo. No se crea mantenimiento; el correctivo posterior sigue correspondiendo a Mantenimiento.

Concurrencia: SERIALIZABLE más bloqueo del encabezado y restricción de unicidad. Una competencia con otros módulos puede abortar por interbloqueo; se revierte y se exige comprobar el estado antes de reintentar. No hay reintentos automáticos ni infraestructura distribuida. Las conexiones directas se cierran con try-with-resources, como en los servicios estabilizados; no se activa autoCommit en un finally que pudiera confirmar cambios pendientes. Fallos de rollback se preservan como excepciones suprimidas.

UI: selección de préstamo con solicitante y fechas; tabla únicamente de pendientes, código y nombre de equipo, casillas y condición por fila; observaciones comunes y confirmación del lote. Los IDs permanecen asociados a los objetos seleccionados y no se escriben manualmente. Tras confirmar o fallar se vuelve a consultar. Se reutiliza OperacionModulo/SwingWorker, con JDBC fuera del EDT, actualización visual en EDT y controles bloqueados durante operaciones.

## Reservas

Arquitectura: `ReservaView → ReservaController → ReservaService → ReservaRepository → JDBC`.

Contratos: crear mediante solicitante, equipo, fechas y observaciones; listar, buscar por ID y cancelar. También consultas de selección de solicitantes activos y equipos. Todas las entradas del servicio verifican actor activo y permiso; el Repository no decide autorizaciones.

Reglas implementadas:

- Solicitante existente y ACTIVO, equipo existente e IDs positivos.
- Fechas obligatorias, años 1–9999, final igual o posterior a inicial; máximo 250 caracteres de observaciones persistidas. No se inventó una prohibición de fechas pasadas.
- Nueva reserva siempre ACTIVA; fecha de creación determinada por el reloj del servicio.
- Solapamientos inclusivos del mismo equipo: `fecha_inicio <= nuevaFin AND fecha_fin >= nuevoInicio`, solo para ACTIVA. Intervalos que comparten un día se solapan. CANCELADA y COMPLETADA no bloquean otro intervalo.
- Creación y cancelación con SERIALIZABLE, conexión única, commit final y rollback completo. La consulta de intervalos participa en la transacción para impedir que dos creaciones solapadas confirmen simultáneamente; sin índice específico puede bloquear rangos amplios.
- Cancelación con lectura `UPDLOCK, HOLDLOCK`, exigencia de ACTIVA y UPDATE condicionado con comprobación de filas. Cancelar dos veces o cancelar COMPLETADA se rechaza.
- Auditoría RESERVA_CREADA y RESERVA_CANCELADA mediante el contrato existente, dentro de la misma transacción.

Disponibilidad: TEAM_CONTRACT §11 y DEC-005 exigen consultar disponibilidad, pero no definen expresamente que el equipo deba estar físicamente DISPONIBLE al registrar una reserva futura. EquipoRepository ofrece listarDisponibles, sin que ello establezca una regla obligatoria de reserva. Siguiendo el ajuste aprobado, las nuevas reservas comprueban existencia y solapamiento ACTIVO; no filtran por estado físico actual ni prometen disponibilidad histórica/futura. El estado se muestra como información.

COMPLETADA conserva lectura y persistencia del enum actual. No se agregó botón, método de servicio, automatismo ni evento RESERVA_COMPLETADA: su transición queda pendiente de integración.

Reserva ≠ Préstamo. Reservar/cancelar no escribe Equipo.estado, Prestamo ni DetallePrestamo. No existe conversión automática ni manual a préstamo. PrestamoService permanece intacto y vuelve a exigir DISPONIBLE en su transacción real; una reserva nunca concede una excepción.

UI: tabla con solicitante, equipo, intervalo, estado y observaciones; selectores de usuario/equipo con nombres y códigos; fechas, creación y cancelación confirmadas. La selección se traduce correctamente entre filas ordenadas y modelo. La consulta muestra también estados históricos, incluida COMPLETADA. JDBC se ejecuta mediante SwingWorker.

ESTUDIANTE puede ser solicitante registrado por un operador autorizado; no recibe acceso administrativo ni autoservicio. Todos sus intentos directos de listar, consultar por ID o cancelar se rechazan, también cambiando IDs. Reservas propias queda pendiente de un contrato de sesión/permiso específico, sin bypass.

## Pruebas nuevas validadas

Se ejecutaron y aprobaron las seis clases nuevas: 57 métodos de prueba, equivalentes a 84 casos contando las entradas de las pruebas parametrizadas. Resultado: **84/84 aprobados, 0 failures, 0 errors y 0 skipped**.

| Clase | Métodos | Casos aprobados |
|---|---:|---:|
| service/DevolucionTransaccionTest | 18 | 25 |
| repository/DevolucionPersistenciaTest | 6 | 6 |
| service/ReservaTransaccionTest | 21 | 41 |
| repository/ReservaPersistenciaTest | 8 | 8 |
| controller/DevolucionReservaSesionTest | 2 | 2 |
| view/DevolucionReservaViewTest | 2 | 2 |
| Total | 57 | 84 |

**D1–D15 aprobados.** Devoluciones cubre BUENO → DISPONIBLE, DANADO → DANADO sin mantenimiento automático, devolución parcial con préstamo ACTIVO, finalización al devolver el último pendiente, doble devolución rechazada, pertenencia, inexistentes, condición inválida, rollback por inserción/equipo/préstamo/auditoría, lote mixto confirmado o revertido y permisos. Además, aprobaron commit fallido, fecha/actor del sistema, préstamo finalizado, estado físico inconsistente, validación completa antes de escribir, entradas vacías/IDs/longitudes y conexión compartida SERIALIZABLE.

Reservas aprobó creación, estado y fecha iniciales, todos los estados físicos sin modificación, solicitante inexistente/inactivo, equipo inexistente, fechas nulas/invertidas/fuera de rango, un día, fechas pasadas, solapamientos incluidos límites, intervalos separados, equipos diferentes, cancelación y rechazo de doble cancelación, COMPLETADA histórica, permisos, cambio manual de IDs y rollback de creación/cancelación/auditoría/commit. No se crearon pruebas de autoservicio no implementado; se prueba su denegación.

Una prueba parametrizada usa ReservaService y PrestamoService reales con colaboradores en memoria: después de crear/cancelar una reserva, el préstamo sigue rechazando PRESTADO/DANADO/MANTENIMIENTO/BAJA. No se infiere una relación entre filas de reserva y préstamo.

Los dobles transaccionales separan estado provisional de persistido y comprueban identidad de conexión. Persistencia usa proxies JDBC para parámetros, claves generadas, condiciones SQL, filas afectadas y cierre de recursos. Swing usa controllers simulados y sincronización para verificar consultas fuera del EDT y cambios visuales en EDT; no usa base de datos ni cámara.

## Verificación y compatibilidad

Compilación directa previa con `javac --release 21`, dependencias exactas ya instaladas en la caché local y destino temporal fuera del repositorio: 141 fuentes de producción y pruebas, sin errores. Después se ejecutó la selección autorizada mediante Maven offline, manteniendo `java.awt.headless=true`.

### Resultado de Maven y restricción del sandbox

| Ejecución | Casos | Passed | Failures | Errors | Skipped | Build |
|---|---:|---:|---:|---:|---:|---|
| Selección completa dentro del sandbox | 278 | 264 | 0 | 14 | 0 | BUILD FAILURE |
| Repetición de las dos clases HTTP con permiso local | 14 | 14 | 0 | 0 | 0 | BUILD SUCCESS |
| Consolidado por caso único tras la repetición | 278 | 278 | 0 | 0 | 0 | Dos ejecuciones; no una segunda selección completa |

**Resultado funcional consolidado: 278 casos únicos validados, 278 passed, 0 failures, 0 errors funcionales y 0 skipped.** Incluye 84/84 casos nuevos y 194/194 casos de regresión aprobados. Se realizaron 292 ejecuciones contando los 14 reintentos; estos no son casos únicos adicionales.

Los 14 errores de la primera ejecución ocurrieron exclusivamente al crear servidores HTTP locales simulados en `FacialAuthClientTest.iniciarServidorSimulado` (6 casos, línea 41) y `FacialContratoTest.setup` (8 casos, línea 18). El error fue `java.net.SocketException: Operation not permitted`. Fue una restricción del entorno, no un fallo funcional del código. El error secundario de limpieza de FacialContratoTest se produjo porque el servidor no llegó a crearse.

Esas mismas 14 pruebas se repitieron con permiso para servidores HTTP locales, conservando Maven offline y headless. **Pasaron 14/14 sin modificaciones de código**, con BUILD SUCCESS. No se detectó regresión funcional en FaceID dentro de los contratos simulados validados; no se ejecutó FaceID real.

Se intentó detener la primera ejecución con `-Dsurefire.skipAfterFailureCount=1`, pero Surefire informó que la versión actual de JUnit Platform no admite esa cancelación; por ello continuó la selección tras los errores del entorno. No hubo failures funcionales.

Confirmaciones de la validación:

- Ninguna clase `*IT` fue ejecutada. Maven compiló fuentes IT como parte de testCompile, sin ejecutarlas.
- No se utilizó SQL Server real ni se ejecutaron migraciones.
- No se utilizó cámara, no se ejecutó Python/pytest ni FaceID real.
- `java.awt.headless=true` en todos los reportes de la selección.
- No se modificó código para resolver los errores del sandbox ni se realizaron operaciones Git.
- Evidencia: reportes XML y TXT de las 35 clases seleccionadas en `target/surefire-reports`; los reportes de las dos clases HTTP reflejan su repetición aprobada.

Los 194 casos de regresión aprobaron la selección de Préstamos, Mantenimiento, Auditoría, Reportes (incluido CP-18), Dashboard, QR, contratos faciales simulados, autenticación, usuarios, permisos y catálogos. Estas pruebas aisladas no demuestran el comportamiento real de SQL Server ni de sus bloqueos concurrentes.

- Préstamos: se conservó PrestamoService y sus validaciones CP-04–CP-08. Los cambios de Repository son aditivos.
- Mantenimiento: no se modificó. La devolución DANADO deja preparada la transición correctiva existente.
- Auditoría: no se modificó AuditService ni su tabla; se utiliza su sobrecarga transaccional.
- Equipos: no se modificó EquipoRepository ni la edición general; se usa actualizarEstado con conexión compartida.
- Reportes y Dashboard: no se modificaron. Las escrituras cumplen sus JOIN y consultas de pendientes. CP-18 conserva ACTIVO, fecha esperada y existencia de pendientes.
- Permisos: el mapa de roles y módulos no cambió. El nuevo método de validación y consulta transaccional son aditivos.
- QR y FaceID: sin cambios, sin llamadas al servicio facial, cámaras o procesos biométricos.

## Pendientes

### BLOQUEANTES de la implementación Java aislada actual

- No se detectaron bloqueantes funcionales en la selección aislada validada. La ejecución Maven autorizada quedó completada con 278 casos únicos aprobados. Los pendientes siguientes corresponden a validación visual o integración futura y no bloquean la implementación Java aislada actual.

### Validación e integración pendientes, no bloqueantes para la implementación aislada

- Comprobar interactivamente ambos paneles y menú por rol; las pruebas Swing preparadas no sustituyen el recorrido visual.
- Con autorización separada, validar SQL parametrizado, restricciones, rollback y concurrencia real en una base desechable de SQL Server. No se prepararon ni ejecutaron nuevas IT.
- Integración Reservas/Préstamos: el contrato compartido solicita consultar reservas aplicables, pero PrestamoService aún no lo hace. Falta acordar con el módulo de Préstamos qué conflictos impiden un préstamo y cómo tratar reservas del mismo solicitante; no se inventó ni activó esa política. La protección implementada excluye solapamientos entre reservas, no conflictos entre reserva y préstamo.
- Definir posteriormente la semántica y transición a COMPLETADA y el contrato de autoservicio de reservas propias para ESTUDIANTE.

### OPCIONALES

- Integrar notificaciones cuando exista NotificacionService y un contrato acordado. No se creó un servicio paralelo.
- Optimizar índices de consultas de reservas si la medición real lo justifica; no es necesario alterar el esquema para esta entrega.

## Comandos de la validación autorizada

La selección completa se ejecutó con autorización explícita, offline y headless; no se ejecutó una batería general ni clases `*IT`:

```sh
bash ./mvnw -o -B -ntp -Djava.awt.headless=true -Dsurefire.skipAfterFailureCount=1 -Dtest=DevolucionTransaccionTest,DevolucionPersistenciaTest,ReservaTransaccionTest,ReservaPersistenciaTest,DevolucionReservaSesionTest,DevolucionReservaViewTest,PrestamoServiceTest,PrestamoTransaccionTest,PrestamoPermisosTest,PrestamoQrTest,MantenimientoServiceTest,MantenimientoTransaccionTest,MantenimientoPersistenciaTest,MantenimientoAuditoriaPermisosTest,AuditoriaServiceTest,AuditServiceTransaccionalTest,UsuarioAuditoriaTest,PermisoServiceTest,UsuarioServiceTest,LoginControllerTest,AutenticacionServiceTest,ReporteServiceTest,DashboardPanelTest,QrEquipoServiceTest,QrServiceTest,QrReportesPermisosTest,CatalogosConsultaTest,MantenimientoAuditoriaViewTest,FacialContratoTest,FacialAuthClientTest,FacialAdminServiceTest,FacialAdminControllerTest,AdministradorFacialPermisosTest,RegistroFacialPanelTest,UsuariosPanelFacialTest test
```

Repetición autorizada de las dos clases HTTP, con permiso para servidores locales:

```sh
bash ./mvnw -o -B -ntp -Djava.awt.headless=true -Dtest=FacialAuthClientTest,FacialContratoTest test
```

Las pruebas HTTP faciales usaron servidores simulados locales, no el servicio facial real. SQL Server real y pruebas IT continúan sujetos a autorización independiente. Esta actualización documental no ejecutó nuevas pruebas ni modificó código.

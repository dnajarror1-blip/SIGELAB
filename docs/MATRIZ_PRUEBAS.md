# Matriz de aceptación — SIGELAB

Fuente: guía oficial SIGELAB, sección 10 (CP-01..CP-18), disponible en las referencias locales; contratos y esquema del proyecto. Fecha de actualización: 2026-10-30.

**Ninguna prueba manual, SQL real o biométrica se declara aprobada.** Los estados automatizados corresponden exclusivamente a pruebas Java aisladas con dobles. CP-01 y CP-02 siguen pendientes de validación biométrica real.

| Caso | Requisito | Resultado esperado | Prueba automatizada existente | Estado automatizado | Prueba manual | Estado manual |
|---|---|---|---|---|---|---|
| CP-01 | Credenciales válidas + rostro correcto | Sesión solo tras FaceID, revalidación y LOGIN_EXITOSO persistido | `AutenticacionServiceTest.exitoSoloDespuesDeFacialYRevalidacion` | Aprobado con FaceID simulado; biometría pendiente | Ingresar admin.demo con su rostro registrado | PENDIENTE (no ejecutada) |
| CP-02 | Credenciales válidas + rostro incorrecto | Acceso denegado y LOGIN_FALLIDO | `AutenticacionServiceTest.rostroDistintoDeniegaYAUDITA` | Aprobado con FaceID simulado; biometría pendiente | Usar credenciales válidas y rostro de otra persona | PENDIENTE (no ejecutada) |
| CP-03 | Segundo equipo con código X existente | Rechazo BusinessException sin SQLException; UNIQUE conservado | `EquipoCp03Test (2 casos)` | Aprobado aislado; UNIQUE inspeccionado, no ejecutado | Intentar registrar otro equipo con DEMO-EQ-001; comprobar restricción SQL | PENDIENTE (no ejecutada) |
| CP-04 | Prestar equipo DISPONIBLE | Préstamo/detalle creados, equipo PRESTADO | `PrestamoTransaccionTest.cp04ConfirmaDisponibleConDetalleEstadoYAuditoria` | Aprobado aislado | Prestar DEMO-EQ-001 al solicitante demo y consultar BD | PENDIENTE (no ejecutada) |
| CP-05 | Prestar nuevamente el mismo equipo | Rechazo sin segundo préstamo | `PrestamoTransaccionTest.cp05RechazaSegundoPrestamoDelMismoEquipo` | Aprobado aislado | Repetir préstamo del equipo ya prestado | PENDIENTE (no ejecutada) |
| CP-06 | Prestar equipo DANADO | Rechazo sin cambios | `PrestamoTransaccionTest.cp06RechazaDanadoAlConfirmar` | Aprobado aislado | Devolver DANADO e intentar prestarlo | PENDIENTE (no ejecutada) |
| CP-07 | Prestar equipo MANTENIMIENTO | Rechazo sin cambios | `PrestamoTransaccionTest.cp07RechazaMantenimientoAlConfirmar` | Aprobado aislado | Ingresar a taller e intentar prestarlo | PENDIENTE (no ejecutada) |
| CP-08 | Prestar equipo BAJA | Rechazo sin cambios | `PrestamoTransaccionTest.cp08RechazaBajaAlConfirmar` | Aprobado aislado | Cerrar NO_REPARABLE e intentar prestarlo | PENDIENTE (no ejecutada) |
| CP-09 | Leer QR de DISPONIBLE | Identificado y disponible para agregar | `QrServiceTest, QrEquipoServiceTest, PrestamoQrTest` | Aprobado aislado | Generar/leer QR del cuarto equipo disponible y consultar disponibilidad | PENDIENTE (no ejecutada) |
| CP-10 | Leer QR de PRESTADO | Identificado y bloqueado | `QrEquipoServiceTest, PrestamoQrTest` | Aprobado aislado | Leer QR de equipo ya prestado; comprobar bloqueo | PENDIENTE (no ejecutada) |
| CP-11 | Devolver BUENO | Equipo DISPONIBLE | `DevolucionTransaccionTest.d1BuenoFinalizaConFechaRealYAuditoria` | Aprobado aislado | Devolver BUENO y comprobar equipo/devolución/auditoría | PENDIENTE (no ejecutada) |
| CP-12 | Devolver DANADO | Equipo DANADO | `DevolucionTransaccionTest.d2DanadoNoSeVuelveDisponibleNiEntraAMantenimiento` | Aprobado aislado | Devolver DANADO y comprobar estado | PENDIENTE (no ejecutada) |
| CP-13 | Devolver detalle ya devuelto | Rechazo sin devolución duplicada | `DevolucionTransaccionTest.d5DobleDevolucionSeRechazaEnServicioAunquePrestamoSigaActivo` | Aprobado aislado | Intentar repetir la devolución de un detalle | PENDIENTE (no ejecutada) |
| CP-14 | Devolver DOS de TRES pendientes en una operación | Dos devoluciones; estados por condición; tercero PRESTADO; ACTIVO y exactamente un pendiente | `DevolucionTransaccionTest.cp14DevuelveDosDeTresEnUnaOperacionYConservaUnPendiente` | Aprobado aislado, escenario exacto 2/3 | Prestar tres equipos; devolver dos juntos (BUENO/DANADO); verificar filas y único pendiente | PENDIENTE (no ejecutada) |
| CP-15 | DANADO → mantenimiento | Equipo MANTENIMIENTO y orden abierta | `MantenimientoTransaccionTest.abreDanadoConAuditoriaYEstado` | Aprobado aislado | Enviar equipo DANADO a taller | PENDIENTE (no ejecutada) |
| CP-16 | Cerrar mantenimiento REPARADO | Equipo DISPONIBLE | `MantenimientoTransaccionTest.cierraConResultadoYAuditoria (REPARADO)` | Aprobado aislado | Cerrar orden como REPARADO y consultar equipo | PENDIENTE (no ejecutada) |
| CP-17 | Cerrar mantenimiento NO_REPARABLE | Equipo BAJA | `MantenimientoTransaccionTest.cierraConResultadoYAuditoria (NO_REPARABLE)` | Aprobado aislado | Cerrar orden como NO_REPARABLE y consultar equipo | PENDIENTE (no ejecutada) |
| CP-18 | Fecha esperada vencida + detalle pendiente | Préstamo aparece vencido en reporte | `ReporteServiceTest` | Aprobado aislado | Preparar préstamo ACTIVO con fecha pasada y pendiente; consultar reportes | PENDIENTE (no ejecutada) |

## Evidencia de esta preparación

- Regresión final: **291 casos únicos, 0 failures, 0 errors, 0 skipped**, sumando los últimos resultados de 39 clases en `target/surefire-reports/TEST-*.xml` (solo nombres canónicos `TEST-<clase>.xml`, excluyendo copias históricas con sufijos; sin IT). 277 casos sin sockets + 6 FacialAuthClientTest + 8 FacialContratoTest.
- Autenticación/auditoría, CP-03, CP-14 exacto y hash del seed: selección afectada de 76 casos aprobados, incluida también en la regresión; no sumar esos casos dos veces.
- CP-03 verifica rechazo desde Service sin inserción y presencia de UNIQUE en el esquema. No demuestra ejecución del constraint en SQL Server.
- CP-14 conserva las pruebas anteriores 1/2 y 2/2; agrega explícitamente 2/3 en una operación con condiciones BUENO y DANADO, estado del tercer equipo y un solo pendiente.
- RN-18: éxito facial con auditoría válida da sesión; fallo al auditar éxito no la da. Contraseña/rostro incorrectos registran LOGIN_FALLIDO; fallo de esa auditoría propaga error controlado y no da acceso. Eventos sin usuario válido conservan referencias nulas.
- Primer intento de la selección afectada: 75 casos, 1 failure en aserción nueva de conteo de commits CP-14 (incluía commits de consultas), 0 errors. Se corrigió solo esa aserción, sin modificar DevolucionService; reejecución afectada 76/76 aprobada.
- Primer intento de cada clase HTTP: 6 y 8 errors exclusivamente por `java.net.SocketException: Operation not permitted` del sandbox. Ambas se repitieron individualmente con permiso de socket local, sin cambios de código, y aprobaron.
- Total de ejecuciones de casos, incluyendo reintentos y selecciones solapadas: 456; no equivale a casos únicos. Resultado acumulado de intentos: 1 failure de aserción de prueba y 14 errors de sandbox, todos resueltos en la ejecución final.
- Antecedente declarado por el usuario: **278/278 casos únicos** de la regresión anterior, CP-04..08, CP-11..14 y CP-18 aislados, FaceID simulado. Es evidencia histórica; no se presenta como ejecución de esta tarea ni como validación real.

## Comandos y límites

```sh
./mvnw -Djava.awt.headless=true '-Dtest=*Test,!FacialAuthClientTest,!FacialContratoTest,!*ManualTest' test
./mvnw -Djava.awt.headless=true -Dtest=FacialAuthClientTest test
./mvnw -Djava.awt.headless=true -Dtest=FacialContratoTest test
```

Se mantiene la exclusión del POM `**/*IT.java`. Las dos últimas clases usan únicamente HTTP simulado local. No se inició Python ni se abrió cámara ni se conectó SQL Server. Informes generados por Maven no son evidencia de pruebas manuales. Las clases `*ManualTest` son lanzadores gráficos, no aceptación automatizada.

## Validación real pendiente

1. SQL Server 2022 limpio: esquema, seeds en orden, repetir 003 sin duplicar, comprobar BCrypt/login y UNIQUE real.
2. Probar transacciones, rollback y concurrencia reales de préstamos, devoluciones, mantenimiento y usuarios en BD de pruebas autorizada.
3. Python 3.12, ODBC 18, modelos, claves y umbral configurados; ejecutar pytest aislado por separado.
4. Enrollment real del ADMIN ya existente; status registrado/activo; cámara, liveness y calibración con personas distintas.
5. CP-01 y CP-02 reales y recorrido manual CP-03..CP-18, confirmando auditoría persistida y permisos por rol.

Registrar fecha, entorno, responsable y evidencia antes de cambiar un estado manual a aprobado.

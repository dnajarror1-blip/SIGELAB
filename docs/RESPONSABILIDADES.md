# SIGELAB — Responsabilidades del equipo

**Proyecto:** Programación II — UMG  
**Integrantes:** 8  
**Plazo de trabajo:** 14 días

## Integrante 1 — Fundación e integración

**Ramas:** `feature/project-foundation`, `feature/database-model`

Responsabilidades:
- Configuración Maven y estructura del proyecto.
- Modelos y enumeraciones compartidos.
- Diseño entidad-relación y diccionario de datos.
- Scripts SQL, migraciones y datos iniciales.
- Configuración JDBC y Docker Compose.
- Mantenimiento de TEAM_CONTRACT.md.
- Integración y preparación de versiones estables.
- Revisión del esquema de reservas y notificaciones.

## Integrante 2 — Usuarios y autenticación

**Rama:** `feature/auth-users`

- Usuarios y roles.
- Contraseñas mediante BCrypt.
- Inicio de sesión y permisos.
- Validación de usuarios activos.
- Integración Java con reconocimiento facial.
- Interfaz administrativa de registro facial.
- Notificaciones internas (extensión).

**Rama adicional:** `feature/notifications`

## Integrante 3 — Laboratorios y equipos

**Rama:** `feature/labs-equipment`

- CRUD de laboratorios.
- CRUD de tipos de equipo.
- CRUD de equipos.
- Validación de códigos únicos.
- Gestión del catálogo y estados de equipos.
- Consultas compartidas con otros módulos.

## Integrante 4 — Préstamos

**Rama:** `feature/loans`

- Creación de préstamos.
- Detalles de préstamo.
- Validaciones de disponibilidad.
- Prevención de préstamos duplicados.
- Transacciones SQL.
- Actualización automática del estado de equipos.
- Integración con reservas (extensión).

## Integrante 5 — Devoluciones y reservas

**Rama:** `feature/returns`

- Devoluciones completas y parciales.
- Validación de detalles pendientes.
- Prevención de devoluciones duplicadas.
- Actualización de estados.
- Transacciones de devolución.
- Reservas simples (extensión).

**Rama adicional:** `feature/reservations`

## Integrante 6 — Mantenimiento y auditoría

**Rama:** `feature/maintenance-audit`

- Mantenimiento preventivo y correctivo.
- Apertura y cierre de mantenimientos.
- Actualización de estados del equipo.
- Registro de auditoría.
- Consulta del historial de operaciones.
- Servicio de auditoría compartido.

## Integrante 7 — QR, dashboard y reportes

**Rama:** `feature/qr-reports`

- Generación de códigos QR.
- Lectura de códigos QR.
- Identificación de equipos.
- Dashboard.
- Préstamos vencidos.
- Reportes de equipos, préstamos y mantenimiento.

## Integrante 8 — Reconocimiento facial

**Rama:** `feature/facial-auth`

- Servicio Python con FastAPI.
- OpenCV e InsightFace/ArcFace.
- ONNX Runtime.
- Captura mediante webcam.
- Registro facial (enrollment).
- Verificación facial.
- Cifrado AES-GCM.
- Persistencia de perfiles y auditoría facial.
- Integración HTTP/JSON con Java.

## Reglas de colaboración

1. Todo integrante trabaja en su rama `feature/*`.
2. Las nuevas ramas parten de `develop`.
3. Los cambios se integran mediante Pull Requests hacia `develop`.
4. No se modifican modelos, tablas, enumeraciones ni contratos compartidos sin aprobación.
5. Cada integrante prueba y documenta su módulo.
6. Los requisitos oficiales tienen prioridad sobre las extensiones.
7. El código debe funcionar en Windows y Linux.
8. Cada integrante debe comprender y defender su implementación.

## Coordinación

- Integrante 1: coordinación con todos.
- Integrantes 2 y 8: autenticación facial.
- Integrantes 2 y 6: auditoría de autenticación.
- Integrantes 2 y 5: notificaciones de reservas.
- Integrantes 3 y 4: disponibilidad de equipos.
- Integrantes 4 y 5: préstamos, devoluciones y reservas.
- Integrantes 3 y 7: identificación QR.
- Integrante 6: auditoría compartida con todos.

**Documento sujeto a decisiones aprobadas y registradas en TEAM_CONTRACT.md.**
# SIGELAB — Responsabilidades del equipo

**Proyecto:** Sistema de Gestión y Control de Laboratorios Universitarios  
**Curso:** Programación II — Universidad Mariano Gálvez de Guatemala  
**Integrantes:** 8  
**Plazo de referencia:** 14 días

## 1. Objetivo

Distribuir las responsabilidades de implementación para desarrollar SIGELAB en paralelo, respetando los contratos y evitando duplicación de trabajo.

Todos los integrantes deberán partir de la rama `develop` y reutilizar los modelos, Enums y estructura existentes.

## 2. Integrante #1 — Fundación e integración

**Ramas:**

- `feature/project-foundation` — integrada.
- `feature/database-model` — integrada.
- `feature/team-documentation` — en desarrollo.

### Responsabilidades

- Preparar Maven, Java 21 y estructura del proyecto.
- Crear y mantener los modelos y Enums compartidos.
- Preparar el esquema SQL.
- Configurar Docker Compose y JDBC.
- Mantener los contratos del equipo.
- Elaborar el diagrama entidad-relación y el diccionario de datos.
- Coordinar modificaciones compartidas.
- Revisar Pull Requests e integración.
- Preparar y verificar versiones estables.
- Coordinar compatibilidad entre Linux y Windows.
- Apoyar la integración de la pantalla principal con los responsables de navegación.

### Estado

La fundación inicial ya está integrada en `develop`.

### Pendiente

- Documentación definitiva del equipo.
- Diagrama entidad-relación y diccionario de datos.
- Coordinación de integración y revisión de módulos.
- Verificación final desde una instalación limpia.

El integrante #1 no debe implementar los módulos completos de sus compañeros.

## 3. Integrante #2 — Usuarios, autenticación y notificaciones

**Ramas:**

- `feature/auth-users`
- `feature/notifications`

### Responsabilidades

- Usuarios y roles.
- Contraseñas con BCrypt.
- Inicio de sesión.
- Validación de usuarios ACTIVO.
- Permisos según rol.
- Coordinación con el servicio facial.
- Interfaz administrativa para registro facial.
- Implementación de notificaciones internas.
- Consulta y marcado de notificaciones como leídas.

### Clases principales previstas

- `UsuarioRepository`
- `UsuarioService`
- `LoginController`
- `LoginForm`
- `UsuarioView`
- `NotificacionRepository`
- `NotificacionService`

Las vistas y controladores de notificaciones se definirán dentro del módulo del integrante.

### Coordinación

- Con #8 para la API facial.
- Con #6 para auditoría de autenticación.
- Con #4 y #5 para eventos que generen notificaciones.
- Con #1 para integración y navegación.

### Pruebas oficiales principales

- CP-01.
- CP-02.

## 4. Integrante #3 — Laboratorios y equipos

**Rama:** `feature/labs-equipment`

### Responsabilidades

- CRUD de laboratorios.
- CRUD de tipos de equipo.
- CRUD de equipos.
- Validación de códigos únicos.
- Validación de campos obligatorios.
- Gestión y consulta de estados de equipos.
- Consultas de disponibilidad.
- Métodos compartidos definidos en `EquipoRepository`.

### Clases principales previstas

- `LaboratorioRepository`
- `TipoEquipoRepository`
- `EquipoRepository`
- `LaboratorioService`
- `TipoEquipoService`
- `EquipoService`
- Controladores y vistas correspondientes.

### Coordinación

- Con #4 para disponibilidad durante préstamos.
- Con #5 para consultas de reservas y devoluciones.
- Con #6 para mantenimiento.
- Con #7 para identificación mediante QR.

### Prueba oficial principal

- CP-03.

Los cambios de estado derivados de préstamos, devoluciones y mantenimiento deberán ser gestionados por sus procesos correspondientes, no mediante cambios arbitrarios en la interfaz de equipos.

## 5. Integrante #4 — Préstamos

**Rama:** `feature/loans`

### Responsabilidades

- Creación de préstamos.
- Encabezado `Prestamo`.
- Registros `DetallePrestamo`.
- Validación de uno o varios equipos.
- Comprobación de disponibilidad.
- Prevención de duplicados.
- Validación de fechas.
- Transacciones SQL.
- Actualización de equipos a PRESTADO.
- Auditoría de préstamos.
- Coordinación con reservas.
- Integración de QR para identificar equipos.

### Clases principales previstas

- `PrestamoRepository`
- `DetallePrestamoRepository`
- `PrestamoService`
- `PrestamoController`
- `PrestamoView`

### Coordinación

- Con #3 para consultar equipos.
- Con #5 para validar reservas.
- Con #6 para auditoría.
- Con #7 para QR.
- Con #2 para las notificaciones correspondientes.

### Pruebas oficiales principales

- CP-04.
- CP-05.
- CP-06.
- CP-07.
- CP-08.

La creación del préstamo debe ser una operación transaccional.

## 6. Integrante #5 — Devoluciones y reservas

**Ramas:**

- `feature/returns`
- `feature/reservations`

### Responsabilidades de devoluciones

- Devoluciones completas.
- Devoluciones parciales.
- Validación de detalles pendientes.
- Prevención de devoluciones duplicadas.
- Actualización de equipos según condición.
- Cierre automático del préstamo cuando no existan pendientes.
- Transacciones de devolución.
- Auditoría correspondiente.

### Responsabilidades de reservas

- Registro de reservas por usuario y equipo.
- Consulta de reservas.
- Validación de fechas.
- Prevención de reservas incompatibles.
- Cancelación de reservas.
- Gestión de `EstadoReserva`.
- Coordinación con préstamos y disponibilidad.
- Integración con las notificaciones internas.

### Clases principales previstas

- `DevolucionRepository`
- `DevolucionService`
- `DevolucionController`
- `DevolucionView`
- `ReservaRepository`
- `ReservaService`
- `ReservaController`
- `ReservaView`

### Coordinación

- Con #3 para equipos.
- Con #4 para préstamos y reservas.
- Con #6 para auditoría.
- Con #2 para notificaciones.

### Pruebas oficiales principales

- CP-11.
- CP-12.
- CP-13.
- CP-14.

Las reservas tendrán pruebas adicionales identificadas como EXT-CP.

## 7. Integrante #6 — Mantenimiento y auditoría

**Rama:** `feature/maintenance-audit`

### Responsabilidades

- Mantenimiento preventivo.
- Mantenimiento correctivo.
- Apertura de mantenimientos.
- Cierre de mantenimientos.
- Actualización de estados de equipo.
- Registro de auditoría de operaciones importantes.
- Consulta del historial.
- Implementación del servicio de auditoría compartido.

### Clases principales previstas

- `MantenimientoRepository`
- `MantenimientoService`
- `MantenimientoController`
- `MantenimientoView`
- `AuditoriaRepository`
- `AuditService`
- `AuditoriaView`

### Coordinación

El servicio de auditoría será utilizado por los módulos que registren operaciones relevantes.

### Pruebas oficiales principales

- CP-15.
- CP-16.
- CP-17.

## 8. Integrante #7 — QR, dashboard y reportes

**Rama:** `feature/qr-reports`

### Responsabilidades

- Generación de códigos QR.
- Lectura de códigos QR desde imágenes.
- Identificación de equipos.
- Consultas de disponibilidad mediante QR.
- Dashboard con información real.
- Reportes de equipos.
- Reportes de préstamos.
- Reportes de mantenimiento.
- Identificación de préstamos vencidos.

### Clases principales previstas

- `QrService`
- `ReportesView`
- Los servicios y repositorios de reportes necesarios.

La integración del dashboard con la pantalla principal se coordinará con #1 y #2.

### Coordinación

- Con #3 para datos de equipos.
- Con #4 para préstamos.
- Con #5 para devoluciones.
- Con #6 para mantenimiento.

### Pruebas oficiales principales

- CP-09.
- CP-10.
- CP-18.

Los QR solamente identifican equipos; no modifican sus estados.

## 9. Integrante #8 — Reconocimiento facial

**Rama:** `feature/facial-auth`

### Responsabilidades

- Servicio Python con FastAPI.
- Captura mediante webcam.
- OpenCV.
- InsightFace/ArcFace.
- ONNX Runtime.
- Registro de perfiles faciales.
- Verificación facial.
- Embeddings cifrados mediante AES-GCM.
- Persistencia de perfiles faciales.
- Auditoría facial.
- Endpoints definidos en el contrato compartido.
- Integración HTTP/JSON con Java.

### Coordinación

- Con #2 para autenticación.
- Con #6 para auditoría.
- Con #1 para configuración e integración general.

El servicio facial no debe recibir contraseñas ni almacenar fotografías permanentemente.

## 10. Funcionalidades adicionales aprobadas

### Reservas

**Responsable principal:** #5.  
**Coordinación:** #3 y #4.

La implementación debe utilizar los modelos, Enums y tablas existentes.

No desarrollar calendarios avanzados ni reservas recurrentes.

### Notificaciones internas

**Responsable principal:** #2.  
**Coordinación:** #4 y #5.

Las notificaciones se almacenarán en SQL Server y se mostrarán dentro de SIGELAB.

No implementar servicios externos de mensajería.

### WhatsApp

**Estado:** pendiente de decisión.

No tiene responsable de implementación asignado ni forma parte de las tareas actuales.

## 11. Reglas de colaboración

1. Todas las ramas nuevas se crearán desde `develop` actualizado.
2. Cada integrante desarrollará únicamente su módulo.
3. No se modificarán los modelos o Enums compartidos sin aprobación.
4. No se cambiará el esquema SQL unilateralmente.
5. Las reglas de negocio se implementarán en Service.
6. Las consultas SQL se implementarán en Repository.
7. Se utilizará `DatabaseConfig` para JDBC.
8. Los commits tendrán mensajes descriptivos en español.
9. Cada integrante probará su trabajo.
10. Todos los Pull Requests tendrán como destino `develop`.
11. No se hará Merge si existen conflictos o errores de compilación.
12. Cada integrante deberá poder explicar su código durante la defensa.

## 12. Entregables de cada integrante

Cada integrante deberá entregar:

- Código fuente de su módulo.
- Implementación de las capas necesarias.
- Validaciones y reglas correspondientes.
- Evidencia de pruebas.
- Commits descriptivos.
- Pull Request hacia `develop`.
- Breve explicación de cómo ejecutar y verificar su funcionalidad.

## 13. Orden recomendado de implementación

### Etapa 1 — Fundación

Responsable #1.

**Estado: completada inicialmente.**

### Etapa 2 — Desarrollo paralelo

- #2: autenticación y usuarios.
- #3: laboratorios y equipos.
- #4: préstamos.
- #5: devoluciones.
- #6: mantenimiento y auditoría.
- #7: QR y reportes.
- #8: facial.

### Etapa 3 — Extensiones aprobadas

- #5: reservas.
- #2: notificaciones internas.

Estas funcionalidades pueden prepararse en paralelo, pero no deben retrasar los requisitos oficiales.

### Etapa 4 — Integración general

Responsable de coordinación: #1.

Participan todos los integrantes.

### Etapa 5 — Pruebas y entrega

- Ejecutar CP-01 a CP-18.
- Verificar las extensiones.
- Resolver errores.
- Comprobar instalación limpia.
- Completar manual técnico y manual de usuario.
- Preparar demostración y defensa.

## 14. Principio final

Todos los integrantes son responsables de producir una sola aplicación SIGELAB funcional.

Cada módulo debe respetar los contratos compartidos, integrarse correctamente y poder demostrarse.

Las reservas y las notificaciones internas están aprobadas. WhatsApp permanece pendiente de decisión.
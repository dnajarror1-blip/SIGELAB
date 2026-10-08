# SIGELAB — Registro de decisiones técnicas

Este documento registra las decisiones de arquitectura, tecnologías, colaboración y alcance del proyecto SIGELAB.

Las decisiones compartidas deben respetar la guía oficial del proyecto, el plano técnico del equipo y los acuerdos aprobados durante el desarrollo.

## DEC-001 — Tecnologías base

**Estado:** DEFINIDA  
**Origen:** Plano técnico del proyecto.

- Java 21 y Java Swing.
- Maven y Maven Wrapper.
- Microsoft SQL Server 2022 mediante JDBC.
- Git y GitHub.
- Python y FastAPI para reconocimiento facial.
- ZXing para códigos QR.

## DEC-002 — Arquitectura

**Estado:** DEFINIDA  
**Origen:** Guía oficial y plano técnico.

Arquitectura:

**View → Controller → Service → Repository → JDBC → SQL Server**

Responsabilidades:

- **View:** interfaz gráfica mediante Java Swing.
- **Controller:** coordinación de eventos entre la interfaz y los servicios.
- **Service:** reglas de negocio, validaciones, estados y transacciones.
- **Repository:** consultas SQL y persistencia mediante JDBC.
- **Model:** representación de las entidades.
- **Integration:** comunicación con componentes externos, como QR y reconocimiento facial.

No se añadirán capas adicionales sin una necesidad justificada y aprobación del equipo.

## DEC-003 — Estrategia Git

**Estado:** ADOPTADA

- `main`: versiones estables.
- `develop`: integración del trabajo del equipo.
- `feature/*`: desarrollo individual de funcionalidades.
- Todas las ramas de funcionalidades deben crearse desde `develop`.
- Los cambios se integran mediante Pull Requests.
- No realizar modificaciones directamente sobre `main` o `develop`.
- No utilizar `force push` en ramas compartidas.
- Utilizar mensajes de commit descriptivos en español, conservando prefijos como `feat`, `fix`, `docs`, `test` y `refactor`.
- Antes de integrar cambios, comprobar que el proyecto compila y que no se han modificado contratos compartidos sin autorización.

## DEC-004 — Compatibilidad Windows y Linux

**Estado:** ADOPTADA

El mismo código fuente Java debe funcionar en Windows y Linux.

Para garantizarlo:

- Utilizar rutas portables.
- Mantener la configuración fuera del código fuente.
- Utilizar Maven Wrapper.
- Evitar rutas absolutas específicas de un sistema operativo.
- No incluir contraseñas ni claves privadas en Git.
- Documentar las instrucciones de instalación para ambos sistemas.
- Utilizar Docker Compose para facilitar una configuración uniforme de SQL Server.

## DEC-005 — Reservas simples

**Fecha:** 2026-10-08  
**Estado:** APROBADA — decisión confirmada por el equipo.  
**Responsable principal:** Integrante #5.

### Descripción

SIGELAB implementará un módulo de reservas simples para permitir que un usuario reserve un equipo durante un intervalo de fechas.

### Alcance aprobado

- Registrar reservas asociadas a un usuario y un equipo.
- Consultar las reservas registradas.
- Manejar los estados definidos en `EstadoReserva`.
- Permitir cancelar reservas según las reglas de negocio.
- Validar las fechas y evitar reservas incompatibles o solapadas.
- Consultar la disponibilidad del equipo antes de confirmar una reserva.
- Coordinar la validación de disponibilidad con el módulo de préstamos.
- Ejecutar las validaciones críticas en Service y dentro de transacciones cuando corresponda.

### Fuera de alcance

- Reservas recurrentes.
- Horarios por horas o minutos.
- Calendarios avanzados.
- Pagos.
- Integraciones con calendarios externos.

### Justificación

Permitir planificar el uso de los equipos sin añadir complejidad innecesaria al proyecto.

### Integrantes involucrados

- **#5:** implementación principal de reservas.
- **#4:** coordinación con préstamos para evitar conflictos de disponibilidad.
- **#3:** consultas de equipos y sus estados.
- **#1:** mantenimiento de los contratos compartidos cuando sea necesario.

### Infraestructura existente

El proyecto ya contiene:

- Tabla SQL `Reserva`.
- Modelo Java `Reserva.java`.
- Enum `EstadoReserva`.

El módulo funcional deberá implementarse utilizando las capas Repository, Service, Controller y View.

No se modificarán unilateralmente los campos o estados definidos.

## DEC-006 — Notificaciones internas

**Fecha:** 2026-10-08  
**Estado:** APROBADA — decisión confirmada por el equipo.  
**Responsable principal:** Integrante #2.

### Descripción

SIGELAB implementará notificaciones internas para informar a los usuarios sobre eventos importantes del sistema.

Las notificaciones se almacenarán en SQL Server y se mostrarán dentro de la aplicación de escritorio.

### Alcance aprobado

- Registrar notificaciones asociadas a usuarios.
- Consultar las notificaciones de un usuario.
- Identificar notificaciones pendientes de lectura.
- Permitir marcar notificaciones como leídas.
- Utilizar los tipos definidos en `TipoNotificacion`.
- Generar avisos desde procesos relevantes, según las reglas del sistema.
- Centralizar la creación y consulta de notificaciones en la capa Service.
- Evitar que los módulos implementen mecanismos independientes de notificación.

### Eventos posibles

Las notificaciones podrán generarse en situaciones como:

- Confirmación de un préstamo.
- Registro de una devolución.
- Confirmación o cancelación de una reserva.
- Avisos relacionados con la fecha esperada de devolución.

Los eventos definitivos deberán acordarse entre los integrantes afectados para evitar duplicaciones.

### Fuera de alcance actual

- WhatsApp.
- SMS.
- Correos electrónicos.
- Notificaciones push del sistema operativo.
- Servicios externos de mensajería.

### Justificación

Mejorar la comunicación con los usuarios dentro de SIGELAB sin depender de servicios de terceros.

### Integrantes involucrados

- **#2:** implementación del módulo de notificaciones.
- **#4:** integración con eventos de préstamos.
- **#5:** integración con devoluciones y reservas.
- **#1:** coordinación de contratos compartidos.

### Infraestructura existente

El proyecto ya contiene:

- Tabla SQL `Notificacion`.
- Modelo Java `Notificacion.java`.
- Enum `TipoNotificacion`.

El módulo funcional deberá implementarse utilizando Repository, Service, Controller y View.

La lógica de notificaciones no deberá duplicarse dentro de cada módulo.

## DEC-007 — Integración futura con WhatsApp

**Fecha:** 2026-10-08  
**Estado:** PENDIENTE DE DECISIÓN — no incluida en el alcance actual.

### Descripción

Se evaluará la posibilidad de enviar mensajes de WhatsApp desde SIGELAB mediante una integración oficial en una versión futura.

### Consideraciones

- Primero se completará el módulo de notificaciones internas.
- La arquitectura debe permitir futuras ampliaciones sin reescribir las reglas de negocio.
- Una integración futura deberá evaluar los requisitos de la API oficial de WhatsApp.
- También deberán considerarse credenciales, privacidad, consentimiento de los destinatarios y posibles costos.
- No se implementará el envío mediante WhatsApp mientras el equipo no apruebe expresamente esta ampliación.

### Justificación

Evitar que una integración externa retrase las funcionalidades obligatorias, las reservas, las notificaciones internas y las pruebas del proyecto.

### Integrantes involucrados

- **#2:** posible responsable de la integración de mensajería.
- **#1:** revisión de arquitectura y configuración.
- **Equipo completo:** aprobación del cambio de alcance.

## Protocolo de nuevas decisiones

Toda decisión nueva deberá documentar:

- Identificador.
- Fecha.
- Descripción.
- Justificación.
- Integrantes afectados.
- Estado: PROPUESTA, APROBADA o RECHAZADA.

### Reglas para modificar contratos compartidos

1. No modificar unilateralmente nombres de tablas, columnas, modelos, Enums o métodos compartidos.
2. Proponer el cambio y explicar por qué es necesario.
3. Identificar los archivos e integrantes afectados.
4. Obtener la aprobación del equipo antes de aplicar el cambio.
5. Actualizar simultáneamente la implementación y la documentación correspondiente.
6. Ejecutar las pruebas necesarias antes de solicitar la integración mediante Pull Request.

## Principio general del proyecto

**SIGELAB debe cumplir primero todos los requisitos obligatorios de la guía oficial, incluyendo sus reglas de negocio y casos de prueba.**

Las reservas y las notificaciones internas forman parte del alcance adicional aprobado por el equipo.

Cualquier nueva funcionalidad deberá evaluarse en función del tiempo disponible, la complejidad que introduce y su impacto sobre los módulos existentes.
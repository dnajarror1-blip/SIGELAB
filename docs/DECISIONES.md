# SIGELAB — Registro de decisiones técnicas

## DEC-001 — Tecnologías base
**Estado:** Definido en el plano técnico.

- Java 21 y Java Swing.
- Maven y Maven Wrapper.
- Microsoft SQL Server 2022 mediante JDBC.
- Git y GitHub.
- Python y FastAPI para reconocimiento facial.
- ZXing para códigos QR.

## DEC-002 — Arquitectura
**Estado:** Obligatorio según la guía oficial.

View → Controller → Service → Repository → JDBC → SQL Server.

Las reglas de negocio pertenecen a Service y las consultas SQL a Repository.

## DEC-003 — Estrategia Git
**Estado:** Adoptado para el equipo.

- `main`: versiones estables.
- `develop`: integración.
- `feature/*`: desarrollo individual.
- Los cambios se integran mediante Pull Requests.

## DEC-004 — Compatibilidad
**Estado:** Decisión del equipo.

El mismo código fuente Java debe funcionar en Windows y Linux. Se utilizarán rutas portables, configuración externa y Maven Wrapper.

## DEC-005 — Reservas simples
**Estado:** Extensión propuesta por el equipo; pendiente de ratificación del contrato técnico.

Una reserva corresponde a un equipo y un intervalo de fechas, sin horarios complejos ni recurrencia.

Responsables de implementación: integrantes 4 y 5.

## DEC-006 — Notificaciones internas
**Estado:** Extensión propuesta por el equipo; pendiente de ratificación del contrato técnico.

Los mensajes se almacenarán en SQL Server y se mostrarán dentro de SIGELAB. No se implementarán notificaciones externas.

Responsable principal: integrante 2.

## Protocolo de nuevas decisiones

Cada decisión futura debe documentar:

- Identificador.
- Fecha.
- Descripción.
- Justificación.
- Integrantes afectados.
- Estado: PROPUESTA, APROBADA o RECHAZADA.

No se modificarán contratos compartidos sin aprobación previa del equipo.
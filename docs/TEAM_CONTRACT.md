# SIGELAB — Contrato Técnico del Equipo

## 1. Autoridad
1. Guía oficial SIGELAB.
2. projectDesign.docx y decisiones aprobadas del equipo.
3. Guía técnica facial.
4. Proyecto de referencia facial.

Ante conflictos prevalece la guía oficial.

## 2. Tecnologías
- Java 21
- Java Swing
- Maven Wrapper
- SQL Server 2022
- JDBC
- Git y GitHub
- ZXing para QR
- Python + FastAPI para reconocimiento facial
- Docker Compose para SQL Server

El código compartido debe funcionar en Linux y Windows.

## 3. Arquitectura
View → Controller → Service → Repository → JDBC → SQL Server

Las reglas de negocio pertenecen a Service.
Las consultas SQL pertenecen a Repository.

## 4. Paquete base
sigelab

Paquetes:
- model
- enums
- repository
- service
- controller
- view
- config
- util
- exception
- integration.qr
- integration.facial

## 5. Modelos compartidos
Usuario, Rol, Laboratorio, TipoEquipo, Equipo,
Prestamo, DetallePrestamo, Devolucion,
Mantenimiento, Auditoria, PerfilFacial,
AuditoriaFacial.

Extensiones:
Reserva, Notificacion.

Los nombres y campos deben respetar el plano
técnico aprobado.

## 6. Reglas de negocio
RN-01 a RN-20 son obligatorias.
CP-01 a CP-18 son pruebas oficiales.

Las extensiones utilizarán EXT-RN y EXT-CP
para evitar confusiones.

## 7. Extensiones
Reservas simples:
- Un equipo por reserva.
- Intervalos de fechas.
- Sin reservas recurrentes.
- Sin modificar directamente EstadoEquipo.

Notificaciones:
- Internas.
- Persistidas en SQL Server.
- Sin WhatsApp, SMS ni servicios externos.

Las extensiones no deben bloquear el CORE.

## 8. Git
main: versiones estables.
develop: integración.
feature/*: desarrollo individual.

Toda feature parte de develop.
Los cambios llegan a develop mediante Pull Request.

No modificar main directamente.
No utilizar force push en ramas compartidas.

## 9. Responsabilidades compartidas
Integrante 1 administra la estructura,
modelos, enums, base de datos y configuración.

Los demás integrantes implementan sus módulos
utilizando los contratos aprobados.

Nadie modifica unilateralmente:
- Tablas o columnas.
- Modelos o enums compartidos.
- Contratos públicos.
- pom.xml.
- Endpoints faciales.
- Estructura del proyecto.

## 10. Protocolo de cambios
Toda modificación compartida debe indicar:
1. Cambio propuesto.
2. Justificación.
3. Archivos afectados.
4. Integrantes afectados.
5. Aprobación del equipo.
6. Pruebas necesarias.

## 11. Registro de decisiones
Las decisiones pendientes deberán discutirse
y aprobarse antes de implementarse.

Estado inicial: pendiente de revisión del equipo.

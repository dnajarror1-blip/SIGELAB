# SIGELAB — Contrato Técnico del Equipo

**Proyecto:** Sistema de Gestión y Control de Laboratorios Universitarios  
**Curso:** Programación II — Universidad Mariano Gálvez de Guatemala  
**Integrantes:** 8  
**Versión del contrato:** 1.0

## 1. Objetivo

Establecer una estructura técnica única para SIGELAB, permitiendo que los ocho integrantes desarrollen sus módulos en paralelo sin generar incompatibilidades.

Todos los integrantes deberán utilizar los mismos nombres, modelos, enumeraciones, tablas, métodos compartidos y reglas de negocio.

La prioridad es cumplir los requisitos oficiales con la menor complejidad necesaria.

## 2. Jerarquía de documentos

En caso de contradicción, se aplicará este orden:

1. Guía oficial del proyecto SIGELAB.
2. `projectDesign.docx` y decisiones aprobadas del equipo.
3. Contratos definidos en este documento.
4. Guía técnica del módulo facial.
5. Proyecto de referencia facial.

La guía oficial tiene prioridad sobre cualquier decisión interna.

Las reservas y notificaciones internas están aprobadas por el equipo.

WhatsApp queda pendiente de una decisión futura y no forma parte de la implementación actual.

## 3. Tecnologías oficiales

- Java 21.
- Java Swing.
- Maven y Maven Wrapper.
- Microsoft SQL Server 2022.
- JDBC.
- Git y GitHub.
- Docker Compose para SQL Server.
- ZXing para generación y lectura de QR.
- Python, FastAPI y OpenCV/InsightFace para reconocimiento facial.

El código Java deberá funcionar en Windows y Linux sin versiones independientes del proyecto.

No se agregarán dependencias sin necesidad justificada.

## 4. Arquitectura obligatoria

El proyecto utiliza:

**View → Controller → Service → Repository → JDBC → SQL Server**

Responsabilidades:

- **View:** mostrar información y recibir acciones del usuario.
- **Controller:** coordinar eventos de las vistas y llamar a los servicios.
- **Service:** implementar reglas de negocio, validaciones, transacciones y cambios de estado.
- **Repository:** consultar y modificar datos mediante JDBC.
- **Model:** representar las entidades.
- **Integration:** comunicarse con funciones QR y el servicio facial.

No se escribirán consultas SQL dentro de View o Controller.

No se implementarán reglas de negocio críticas únicamente en la interfaz.

## 5. Estructura oficial

Paquete base:

`sigelab`

Estructura:

```text
src/main/java/sigelab/
├── Main.java
├── model/
├── enums/
├── repository/
├── service/
├── controller/
├── view/
├── config/
├── util/
├── exception/
└── integration/
    ├── qr/
    └── facial/

src/test/java/sigelab/

database/
├── sigelab.sql
├── seed/
└── migrations/

docs/
facial-service/
pom.xml
README.md
docker-compose.yml
```

Las carpetas de futuros módulos se crearán cuando sean necesarias.

No se agregarán capas como DAO, DTO, Manager o Facade sin aprobación.

## 6. Infraestructura compartida existente

La rama `develop` contiene la fundación inicial:

- Configuración Maven y Java 21.
- Maven Wrapper.
- 14 modelos Java.
- 10 Enums.
- Esquema SQL con 14 tablas y 17 claves foráneas.
- Configuración de SQL Server con Docker Compose.
- Datos iniciales de roles ADMIN y ENCARGADO.
- Clase `DatabaseConfig`.
- Prueba de integración JDBC.
- README con configuración para Windows y Linux.

Los servicios, repositorios, controladores y vistas funcionales deberán implementarse en sus módulos correspondientes.

## 7. Modelos canónicos

Los nombres oficiales son:

1. `Usuario`
2. `Rol`
3. `Laboratorio`
4. `TipoEquipo`
5. `Equipo`
6. `Prestamo`
7. `DetallePrestamo`
8. `Devolucion`
9. `Mantenimiento`
10. `Auditoria`
11. `PerfilFacial`
12. `AuditoriaFacial`
13. `Reserva`
14. `Notificacion`

Los modelos existentes son compartidos.

No se crearán duplicados ni variantes con nombres diferentes.

Los atributos deben coincidir con las columnas SQL.

## 8. Enumeraciones canónicas

| Enum | Valores |
|---|---|
| EstadoUsuario | ACTIVO, INACTIVO |
| EstadoLaboratorio | ACTIVO, INACTIVO |
| EstadoEquipo | DISPONIBLE, PRESTADO, DANADO, MANTENIMIENTO, BAJA |
| EstadoPrestamo | ACTIVO, FINALIZADO |
| CondicionDevolucion | BUENO, DANADO |
| TipoMantenimiento | PREVENTIVO, CORRECTIVO |
| ResultadoMantenimiento | REPARADO, NO_REPARABLE |
| EstadoMantenimiento | ABIERTO, CERRADO |
| EstadoReserva | ACTIVA, CANCELADA, COMPLETADA |
| TipoNotificacion | RESERVA_CREADA, RESERVA_CANCELADA, RESERVA_COMPLETADA, PRESTAMO_VENCIDO, SISTEMA |

No se utilizarán valores escritos libremente como cadenas de texto cuando exista un Enum.

Los cambios a los Enums requieren aprobación.

## 9. Contrato JDBC

La conexión a SQL Server se obtiene mediante:

`DatabaseConfig.getConnection()`

Todos los Repository deberán utilizar esta configuración compartida.

Las credenciales se obtendrán mediante variables de entorno.

Está prohibido introducir contraseñas directamente en clases Java o subir archivos privados al repositorio.

Las conexiones y recursos JDBC deberán cerrarse correctamente, preferiblemente mediante `try-with-resources`.

Cuando una operación incluya varios cambios relacionados, deberán ejecutarse dentro de una transacción.

Si falla una operación, se utilizará `rollback` para evitar información inconsistente.

Los repositorios involucrados en una misma transacción deberán utilizar la misma conexión JDBC.

## 10. Contratos mínimos entre módulos

### 10.1 Equipos

**Responsable:** Integrante #3.

La clase `EquipoRepository` deberá implementar el contrato definido en el plano técnico:

```java
Optional<Equipo> buscarPorId(int id);
Optional<Equipo> buscarPorCodigoInventario(String codigo);
List<Equipo> listar();
List<Equipo> listarDisponibles();
void guardar(Equipo equipo);
void actualizar(Equipo equipo);
void actualizarEstado(int idEquipo, EstadoEquipo estado);
void eliminar(int idEquipo);
```

La eliminación solamente podrá realizarse cuando no exista historial que lo impida.

Los integrantes que necesiten consultar equipos deberán reutilizar el módulo existente.

### 10.2 Préstamos

**Responsable:** Integrante #4.

`PrestamoService` deberá ofrecer las operaciones definidas en el plano técnico:

- `crearPrestamo`
- `buscarActivos`
- `buscarPorId`
- `validarEquipoParaPrestamo`

La creación de préstamos deberá incluir:

- Encabezado del préstamo.
- Detalles asociados.
- Actualización de equipos a PRESTADO.
- Registro de auditoría.
- Una transacción que permita revertir todos los cambios si falla una parte.

Las firmas exactas deberán respetar `projectDesign.docx` y acordarse cuando sea necesaria una integración adicional.

### 10.3 Auditoría

**Responsable:** Integrante #6.

Contrato del servicio compartido:

```java
void registrar(
    Integer idUsuario,
    String accion,
    String entidad,
    Integer idRegistro,
    String descripcion
);
```

La clase será `AuditService`.

Los demás módulos deberán utilizar este servicio para registrar eventos relevantes.

No crear servicios de auditoría duplicados.

Cuando la auditoría forme parte de una transacción crítica, deberá participar en la misma transacción.

### 10.4 Códigos QR

**Responsable:** Integrante #7.

Contrato:

```java
void generar(String codigoInventario, File destino);
String leer(File imagen);
```

La clase será `QrService`.

El QR identifica un equipo mediante su código de inventario.

La lectura QR no modifica el estado del equipo.

### 10.5 Reconocimiento facial

**Responsables:** Integrantes #8 y #2.

El módulo Java utilizará `FacialAuthClient` para comunicarse con el servicio Python.

Endpoints acordados:

- `GET /health`
- `GET /face/status/{userId}`
- `POST /face/enroll`
- `POST /face/verify`
- `DELETE /face/profile/{userId}`

El servicio facial funcionará localmente mediante FastAPI.

Java deberá verificar primero usuario, contraseña y estado ACTIVO antes de solicitar la verificación facial.

Las contraseñas no se enviarán al servicio Python.

Los errores del servicio facial deberán manejarse de forma segura y comprensible.

## 11. Contrato de reservas

**Estado:** APROBADO.  
**Responsable principal:** Integrante #5.  
**Coordinación:** Integrantes #3 y #4.

### Modelo y tabla

- Modelo: `Reserva`
- Tabla: `Reserva`
- Enum: `EstadoReserva`

Estados válidos:

- ACTIVA
- CANCELADA
- COMPLETADA

### Reglas

1. Una reserva corresponde a un usuario y un equipo.
2. Cada reserva utiliza `fechaInicio` y `fechaFin`.
3. La fecha final no puede ser anterior a la inicial.
4. Dos reservas ACTIVA del mismo equipo no pueden tener fechas solapadas.
5. La disponibilidad debe verificarse en Service.
6. La creación de una reserva no cambia directamente `EstadoEquipo`.
7. La creación de préstamos deberá consultar las reservas aplicables para evitar conflictos.
8. La cancelación deberá seguir las reglas del módulo.
9. No se implementarán horarios, recurrencias ni calendarios avanzados.

### Coordinación

El integrante #5 desarrollará `ReservaService` y `ReservaRepository`.

El integrante #4 deberá utilizar las validaciones de reservas al implementar préstamos.

El integrante #3 proporcionará las consultas de equipos necesarias.

Las firmas públicas adicionales se acordarán antes de que otro módulo dependa de ellas.

## 12. Contrato de notificaciones internas

**Estado:** APROBADO.  
**Responsable principal:** Integrante #2.

### Modelo y tabla

- Modelo: `Notificacion`
- Tabla: `Notificacion`
- Enum: `TipoNotificacion`

### Reglas

1. Cada notificación pertenece a un usuario.
2. Las notificaciones se almacenan en SQL Server.
3. El sistema permite consultar notificaciones por usuario.
4. Las notificaciones pueden estar leídas o pendientes.
5. El usuario podrá marcar sus notificaciones como leídas.
6. La generación de notificaciones se centraliza en `NotificacionService`.
7. Las operaciones que generen notificaciones deberán coordinarse con ese servicio.
8. Los tipos de notificación deben respetar `TipoNotificacion`.
9. No se implementará mensajería externa en esta etapa.

### Integración

El integrante #2 implementará:

- `NotificacionRepository`
- `NotificacionService`
- El controlador y la interfaz necesarios para consultar notificaciones.

Los integrantes #4 y #5 se coordinarán con el #2 para generar las notificaciones relacionadas con préstamos, devoluciones y reservas.

No se crearán tipos nuevos ni servicios de notificación independientes sin aprobación.

## 13. Reglas de negocio y pruebas

Las reglas oficiales **RN-01 a RN-20** son obligatorias.

Los casos de prueba oficiales **CP-01 a CP-18** también son obligatorios.

Las reglas adicionales de reservas y notificaciones deberán identificarse separadamente mediante:

- `EXT-RN`
- `EXT-CP`

Esto evita confundir los requisitos oficiales con las ampliaciones aprobadas por el equipo.

Cada integrante es responsable de probar su módulo.

## 14. Manejo de errores

Los errores técnicos no deberán mostrarse directamente al usuario.

Se utilizarán excepciones como:

- `BusinessException`
- `DataAccessException`
- `FacialClientException`

O una estrategia equivalente aprobada.

La interfaz deberá presentar mensajes comprensibles.

Las validaciones de negocio pertenecen a Service.

## 15. Estrategia Git

Ramas:

- `main`: versión estable.
- `develop`: integración del equipo.
- `feature/*`: desarrollo individual.

Reglas:

1. Crear cada rama desde `develop` actualizado.
2. Desarrollar solamente los archivos correspondientes al módulo.
3. Realizar commits pequeños y descriptivos en español.
4. Publicar los cambios mediante Push.
5. Crear Pull Request hacia `develop`.
6. Revisar conflictos y ejecutar pruebas antes del Merge.
7. No modificar directamente `main`.
8. No utilizar `force push` en ramas compartidas.

## 16. Archivos compartidos protegidos

No modificar unilateralmente:

- `pom.xml`
- `database/sigelab.sql`
- Archivos de migraciones
- Modelos compartidos
- Enums compartidos
- `DatabaseConfig.java`
- Contratos públicos utilizados por otros integrantes
- Endpoints del módulo facial
- Estructura general de paquetes
- `TEAM_CONTRACT.md`

Cualquier modificación debe coordinarse con el integrante #1 y los demás afectados.

## 17. Criterios para completar un módulo

Antes de solicitar un Pull Request, el integrante deberá verificar:

- Modelos y Enums correctamente utilizados.
- Repository implementado.
- Service con reglas de negocio.
- Controller conectado.
- View funcional.
- Validaciones realizadas.
- Manejo de errores.
- Transacciones cuando correspondan.
- Auditoría cuando sea necesaria.
- Pruebas del módulo ejecutadas.
- Compilación Maven correcta.
- Documentación mínima actualizada.

No se considera terminado un módulo únicamente porque su interfaz se muestre.

## 18. Protocolo de cambios compartidos

Antes de modificar un contrato:

1. Describir el cambio propuesto.
2. Explicar su justificación.
3. Identificar los archivos afectados.
4. Identificar a los integrantes afectados.
5. Obtener aprobación.
6. Aplicar los cambios de manera coordinada.
7. Actualizar la documentación.
8. Ejecutar las pruebas correspondientes.

## 19. Decisiones de alcance

### Funcionalidades aprobadas

- Requisitos oficiales del proyecto.
- Reservas simples.
- Notificaciones internas.

### Pendiente de decisión

- Integración con WhatsApp.

### Fuera del alcance actual

- Aplicaciones móviles.
- Microservicios.
- Pagos.
- Calendarios avanzados.
- Vigilancia facial continua.
- Servicios externos de mensajería no aprobados.

## 20. Principio final

El trabajo de los ocho integrantes deberá integrarse en una única aplicación funcional, coherente y defendible.

La prioridad será completar correctamente los requisitos oficiales, las reservas y las notificaciones internas sin introducir complejidad innecesaria.
# SIGELAB — Diccionario de datos

## 1. Fuente y convenciones

Este documento describe las **14 tablas del esquema `dbo`** declaradas en [database/sigelab.sql](../database/sigelab.sql). No presupone que una base local ya tenga esos objetos ni que sus datos hayan sido verificados. Las relaciones se representan en [MODELO_ER.md](MODELO_ER.md).

- **PK:** clave primaria. Cada tabla tiene una PK llamada `PK_<nombre de la tabla>`.
- **FK:** clave foránea. Los nombres exactos y destinos de las 17 FK están en el inventario del [modelo ER](MODELO_ER.md), §3.
- **UNIQUE:** unicidad de una columna o combinación de columnas; no implica unicidad de cada componente de una combinación.
- **Admite NULL:** refleja la declaración SQL, no una validación de formulario.
- **IDENTITY(1,1):** identificador generado por SQL Server, comenzando en 1 e incrementando en 1. No garantiza una secuencia sin saltos.
- **DEFAULT:** valor utilizado cuando se omite la columna en una inserción; no sustituye un `NULL` explícito ni actualiza automáticamente la fila después.

Solo se indican valores predeterminados cuando el script declara un `DEFAULT`. Las demás columnas no tienen uno explícito; `IDENTITY` se documenta por separado. `DATE` almacena fecha sin hora; `DATETIME2` almacena fecha y hora sin zona horaria. En `DECIMAL(p,s)`, `p` es la precisión total y `s` la escala.

## 2. Rol

Catálogo de roles. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_rol` | `INT IDENTITY(1,1)` | No | PK | Identificador del rol. |
| `nombre` | `NVARCHAR(30)` | No | UNIQUE | Nombre del rol. |

- `UQ_Rol_nombre`: unicidad de `nombre`.
- [001_roles.sql](../database/seed/001_roles.sql) agrega `ADMIN` y `ENCARGADO` si no existen. Son datos iniciales, no valores `DEFAULT` ni una lista limitada por `CHECK`.

## 3. Usuario

Datos de usuario, credenciales, rol y estado. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_usuario` | `INT IDENTITY(1,1)` | No | PK | Identificador del usuario. |
| `nombre` | `NVARCHAR(60)` | No | — | Nombre. |
| `apellido` | `NVARCHAR(60)` | No | — | Apellido. |
| `username` | `NVARCHAR(50)` | No | UNIQUE | Nombre utilizado para autenticarse. |
| `password_hash` | `NVARCHAR(255)` | No | — | Hash de contraseña, no contraseña en texto plano. |
| `correo` | `NVARCHAR(120)` | No | — | Correo del usuario. |
| `id_rol` | `INT` | No | FK → `Rol.id_rol` | Rol asignado. |
| `estado` | `VARCHAR(10)` | No | — | Estado del usuario. |

- `UQ_Usuario_username`: unicidad de `username`.
- `CK_Usuario_estado`: `ACTIVO`, `INACTIVO`.
- BCrypt es un acuerdo de la aplicación; el tipo de columna no verifica el algoritmo del hash. El script no declara unicidad ni validación de formato para `correo`.

## 4. Laboratorio

Ubicación lógica de los equipos. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_laboratorio` | `INT IDENTITY(1,1)` | No | PK | Identificador del laboratorio. |
| `codigo` | `NVARCHAR(20)` | No | UNIQUE | Código del laboratorio. |
| `nombre` | `NVARCHAR(80)` | No | — | Nombre. |
| `ubicacion` | `NVARCHAR(120)` | No | — | Ubicación. |
| `capacidad` | `INT` | No | — | Capacidad declarada. |
| `estado` | `VARCHAR(10)` | No | — | Estado del laboratorio. |

- `UQ_Laboratorio_codigo`: unicidad de `codigo`.
- `CK_Laboratorio_capacidad`: `capacidad > 0`.
- `CK_Laboratorio_estado`: `ACTIVO`, `INACTIVO`.

## 5. TipoEquipo

Catálogo de categorías de equipo. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_tipo_equipo` | `INT IDENTITY(1,1)` | No | PK | Identificador del tipo. |
| `nombre` | `NVARCHAR(60)` | No | UNIQUE | Nombre de la categoría. |
| `descripcion` | `NVARCHAR(200)` | Sí | — | Descripción opcional. |

- `UQ_TipoEquipo_nombre`: unicidad de `nombre`.

## 6. Equipo

Activo administrado, relacionado con un tipo y un laboratorio. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_equipo` | `INT IDENTITY(1,1)` | No | PK | Identificador del equipo. |
| `codigo_inventario` | `NVARCHAR(30)` | No | UNIQUE | Código único que utiliza la identificación QR. |
| `nombre` | `NVARCHAR(100)` | No | — | Nombre del equipo. |
| `id_tipo_equipo` | `INT` | No | FK → `TipoEquipo.id_tipo_equipo` | Categoría. |
| `marca` | `NVARCHAR(60)` | No | — | Marca. |
| `modelo` | `NVARCHAR(60)` | No | — | Modelo del equipo. |
| `numero_serie` | `NVARCHAR(80)` | No | — | Número de serie. |
| `id_laboratorio` | `INT` | No | FK → `Laboratorio.id_laboratorio` | Laboratorio asignado. |
| `estado` | `VARCHAR(20)` | No | — | Estado actual. |
| `observaciones` | `NVARCHAR(250)` | Sí | — | Observaciones opcionales. |

- `UQ_Equipo_codigo_inventario`: unicidad de `codigo_inventario`.
- `CK_Equipo_estado`: `DISPONIBLE`, `PRESTADO`, `DANADO`, `MANTENIMIENTO`, `BAJA`.
- `numero_serie` no tiene restricción `UNIQUE` en el script. El `CHECK` de estado limita valores, pero no controla transiciones ni verifica préstamos asociados.

## 7. Prestamo

Encabezado de préstamo; sus equipos se registran en `DetallePrestamo`. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_prestamo` | `INT IDENTITY(1,1)` | No | PK | Identificador del préstamo. |
| `id_usuario_solicitante` | `INT` | No | FK → `Usuario.id_usuario` | Usuario que solicita los equipos. |
| `id_usuario_registra` | `INT` | No | FK → `Usuario.id_usuario` | Usuario que registra la operación. |
| `fecha_prestamo` | `DATE` | No | — | Fecha del préstamo. |
| `fecha_esperada_devolucion` | `DATE` | No | — | Fecha prevista de devolución. |
| `estado` | `VARCHAR(15)` | No | — | Estado del préstamo. |
| `observaciones` | `NVARCHAR(250)` | Sí | — | Observaciones opcionales. |

- `CK_Prestamo_estado`: `ACTIVO`, `FINALIZADO`.
- `CK_Prestamo_fechas`: `fecha_esperada_devolucion >= fecha_prestamo`.
- No existe un estado SQL `VENCIDO`: el vencimiento debe calcularse por fecha y equipos pendientes. La existencia de al menos un detalle no está garantizada por estas restricciones.

## 8. DetallePrestamo

Asociación de un equipo con un préstamo. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_detalle_prestamo` | `INT IDENTITY(1,1)` | No | PK | Identificador del detalle. |
| `id_prestamo` | `INT` | No | FK → `Prestamo.id_prestamo` | Encabezado al que pertenece. |
| `id_equipo` | `INT` | No | FK → `Equipo.id_equipo` | Equipo incluido. |

- `UQ_DetallePrestamo_PrestamoEquipo`: `UNIQUE (id_prestamo, id_equipo)`.
- No puede repetirse un equipo dentro del mismo préstamo. El mismo equipo sí puede aparecer en préstamos históricos distintos; evitar dos préstamos activos del mismo equipo requiere validación transaccional en `Service`.

## 9. Devolucion

Registro individual de devolución de un detalle. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_devolucion` | `INT IDENTITY(1,1)` | No | PK | Identificador de la devolución. |
| `id_detalle_prestamo` | `INT` | No | FK → `DetallePrestamo.id_detalle_prestamo`; UNIQUE | Detalle que se devuelve. |
| `id_usuario` | `INT` | No | FK → `Usuario.id_usuario` | Usuario que registra la devolución. |
| `fecha_devolucion` | `DATE` | No | — | Fecha de la devolución. |
| `condicion` | `VARCHAR(10)` | No | — | Condición de recepción. |
| `observaciones` | `NVARCHAR(250)` | Sí | — | Observaciones opcionales. |

- `UQ_Devolucion_DetallePrestamo`: unicidad de `id_detalle_prestamo`.
- `CK_Devolucion_condicion`: `BUENO`, `DANADO`.
- El detalle sin devolución representa un equipo pendiente. Validar que el préstamo esté activo, actualizar el equipo y finalizar el encabezado cuando corresponda son responsabilidades del proceso de devolución, no acciones automáticas de la FK.

## 10. Mantenimiento

Historial de intervención sobre un equipo. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_mantenimiento` | `INT IDENTITY(1,1)` | No | PK | Identificador del mantenimiento. |
| `id_equipo` | `INT` | No | FK → `Equipo.id_equipo` | Equipo intervenido. |
| `fecha_ingreso` | `DATE` | No | — | Fecha de ingreso. |
| `tipo` | `VARCHAR(12)` | No | — | Tipo de mantenimiento. |
| `descripcion` | `NVARCHAR(300)` | No | — | Descripción de la intervención. |
| `id_responsable` | `INT` | No | FK → `Usuario.id_usuario` | Usuario responsable. |
| `fecha_salida` | `DATE` | Sí | — | Fecha de salida; puede faltar mientras esté abierto. |
| `resultado` | `VARCHAR(15)` | Sí | — | Resultado; puede faltar mientras esté abierto. |
| `estado` | `VARCHAR(10)` | No | — | Estado del mantenimiento. |
| `observaciones` | `NVARCHAR(250)` | Sí | — | Observaciones opcionales. |

- `CK_Mantenimiento_tipo`: `PREVENTIVO`, `CORRECTIVO`.
- `CK_Mantenimiento_resultado`: `NULL`, `REPARADO`, `NO_REPARABLE`.
- `CK_Mantenimiento_estado`: `ABIERTO`, `CERRADO`.
- El script no añade un `CHECK` que relacione fecha de salida, resultado y estado, ni que ordene las fechas. Tampoco impide por sí solo múltiples mantenimientos abiertos de un equipo. No se deben atribuir esas validaciones a las restricciones existentes.

## 11. Auditoria

Trazabilidad de operaciones relevantes. Forma parte del modelo mínimo oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_auditoria` | `INT IDENTITY(1,1)` | No | PK | Identificador del evento. |
| `id_usuario` | `INT` | Sí | FK → `Usuario.id_usuario` | Usuario identificado; puede ser nulo en eventos sin usuario válido. |
| `fecha_hora` | `DATETIME2` | No | — | Fecha y hora del evento. |
| `accion` | `VARCHAR(50)` | No | — | Acción registrada. |
| `entidad` | `VARCHAR(50)` | No | — | Nombre de la entidad afectada. |
| `id_registro` | `INT` | Sí | — | Referencia lógica al registro, sin FK. |
| `descripcion` | `NVARCHAR(500)` | No | — | Descripción del evento. |

`accion` y `entidad` no tienen listas de valores limitadas por `CHECK`. El SQL no asigna automáticamente `fecha_hora` ni crea eventos al modificar otras tablas; la auditoría requerida debe registrarse desde el proceso correspondiente, dentro de su misma transacción cuando aplique.

## 12. PerfilFacial

Perfil biométrico único por usuario. Es soporte de integración facial acordado por el equipo.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_perfil_facial` | `INT IDENTITY(1,1)` | No | PK | Identificador del perfil. |
| `id_usuario` | `INT` | No | FK → `Usuario.id_usuario`; UNIQUE | Usuario al que pertenece. |
| `embedding` | `VARBINARY(MAX)` | No | — | Bytes del embedding, que deben llegar cifrados desde el servicio facial. |
| `modelo` | `NVARCHAR(50)` | No | — | Identificación del modelo facial. |
| `dimension` | `INT` | No | — | Dimensión del embedding. |
| `cantidad_muestras` | `INT` | No | — | Cantidad de muestras utilizadas. |
| `quality_score` | `DECIMAL(6,4)` | Sí | — | Métrica de calidad opcional. |
| `fecha_registro` | `DATETIME2` | No | — | Fecha y hora del registro. |
| `fecha_actualizacion` | `DATETIME2` | Sí | — | Fecha y hora de actualización, si corresponde. |
| `activo` | `BIT` | No | — | Vigencia lógica del perfil. |

- `UQ_PerfilFacial_Usuario`: unicidad de `id_usuario`.
- `CK_PerfilFacial_dimension`: `dimension > 0`.
- `CK_PerfilFacial_muestras`: `cantidad_muestras > 0`.
- El SQL no obliga a usar exactamente 512 dimensiones ni verifica AES-GCM. Ambas decisiones de la integración facial deben cumplirse en su implementación. `activo` no tiene `DEFAULT` declarado.
- Un usuario puede tener cero o un perfil almacenado. La discrepancia con la cardinalidad `1:1` del plano técnico se señala en el [modelo ER](MODELO_ER.md), §4; no se modifica el contrato con este documento.

## 13. AuditoriaFacial

Eventos del componente biométrico. Es soporte de integración facial acordado por el equipo.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_auditoria_facial` | `INT IDENTITY(1,1)` | No | PK | Identificador del evento facial. |
| `id_usuario` | `INT` | No | FK → `Usuario.id_usuario` | Usuario asociado; a diferencia de `Auditoria`, aquí no admite nulos. |
| `fecha_hora` | `DATETIME2` | No | — | Fecha y hora del evento. |
| `accion` | `VARCHAR(40)` | No | — | Acción facial registrada. |
| `resultado` | `VARCHAR(30)` | No | — | Resultado del evento. |
| `score` | `DECIMAL(8,5)` | Sí | — | Puntuación de similitud, si corresponde. |
| `threshold` | `DECIMAL(8,5)` | Sí | — | Umbral utilizado, si corresponde. |
| `detalle` | `NVARCHAR(500)` | Sí | — | Detalle opcional. |
| `dispositivo` | `NVARCHAR(120)` | Sí | — | Información opcional del dispositivo. |

No hay `CHECK` de rangos para `score`/`threshold` ni listas SQL para `accion`/`resultado`. No se deben incluir embeddings completos, fotografías ni secretos en los eventos.

## 14. Reserva

Reserva simple de un equipo por un usuario, dentro de un intervalo de fechas. Es una extensión aprobada del equipo, no una entidad mínima exigida por la guía oficial.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_reserva` | `INT IDENTITY(1,1)` | No | PK | Identificador de la reserva. |
| `id_usuario` | `INT` | No | FK → `Usuario.id_usuario` | Usuario reservante. |
| `id_equipo` | `INT` | No | FK → `Equipo.id_equipo` | Equipo reservado. |
| `fecha_inicio` | `DATE` | No | — | Fecha inicial. |
| `fecha_fin` | `DATE` | No | — | Fecha final. |
| `estado` | `VARCHAR(12)` | No | — | Estado de la reserva. |
| `fecha_creacion` | `DATETIME2` | No | DEFAULT `SYSDATETIME()` | Fecha y hora de creación. |
| `observaciones` | `NVARCHAR(250)` | Sí | — | Observaciones opcionales. |

- `CK_Reserva_estado`: `ACTIVA`, `CANCELADA`, `COMPLETADA`.
- `CK_Reserva_fechas`: `fecha_fin >= fecha_inicio`.
- `DF_Reserva_fecha_creacion`: `DEFAULT SYSDATETIME()`.
- El SQL no impide solapamientos entre reservas. Las validaciones y la integración con préstamos pertenecen al contrato de reservas de [TEAM_CONTRACT.md](TEAM_CONTRACT.md), §11. Crear una reserva no cambia automáticamente `Equipo.estado`.

## 15. Notificacion

Mensaje interno persistido para un usuario. Es una extensión aprobada del equipo, no mensajería externa.

| Columna | Tipo SQL | Admite NULL | Claves / generación | Descripción |
|---|---|---|---|---|
| `id_notificacion` | `INT IDENTITY(1,1)` | No | PK | Identificador del mensaje. |
| `id_usuario` | `INT` | No | FK → `Usuario.id_usuario` | Usuario destinatario. |
| `tipo` | `VARCHAR(30)` | No | — | Tipo de notificación. |
| `titulo` | `NVARCHAR(100)` | No | — | Título del mensaje. |
| `mensaje` | `NVARCHAR(300)` | No | — | Contenido del mensaje. |
| `fecha_hora` | `DATETIME2` | No | DEFAULT `SYSDATETIME()` | Fecha y hora de creación. |
| `leida` | `BIT` | No | DEFAULT `(0)` | Indicador de lectura; inicialmente falso si se omite. |
| `entidad` | `VARCHAR(30)` | Sí | — | Entidad relacionada, si corresponde. |
| `id_referencia` | `INT` | Sí | — | Referencia lógica al registro, sin FK. |

- `DF_Notificacion_fecha_hora`: `DEFAULT SYSDATETIME()`.
- `DF_Notificacion_leida`: `DEFAULT (0)`.
- `tipo` no tiene un `CHECK` SQL. Los valores acordados de `TipoNotificacion` son `RESERVA_CREADA`, `RESERVA_CANCELADA`, `RESERVA_COMPLETADA`, `PRESTAMO_VENCIDO` y `SISTEMA`; su uso corresponde a la aplicación, según [TEAM_CONTRACT.md](TEAM_CONTRACT.md), §§8 y 12.
- No existe una restricción SQL que evite mensajes duplicados. No se deben guardar credenciales ni secretos en su contenido.

## 16. Límites del esquema documentado

- Las FK no declaran borrado ni actualización en cascada; utilizan `NO ACTION`. Las referencias lógicas de auditoría/notificaciones no son FK adicionales.
- `NOT NULL` no rechaza por sí mismo cadenas vacías o compuestas solo de espacios. Las validaciones de negocio se mantienen en `Service`.
- Los `CHECK` de estados controlan valores, no transiciones. El SQL no implementa permisos, segundo factor facial, disponibilidad, solapamientos ni cierre automático de préstamos.
- El script inicial crea tablas faltantes; no transforma automáticamente una tabla existente para adaptarla a otra versión. La documentación no sustituye las migraciones necesarias cuando se aprueben cambios.
- Este trabajo no modifica el SQL ni los contratos y no acredita pruebas de ejecución. Debe actualizarse de forma coordinada cuando cambie el esquema compartido.

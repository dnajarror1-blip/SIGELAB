# Manual de usuario — SIGELAB

## Preparación e inicio de sesión

Pedir al responsable técnico que complete el arranque del README, incluido el enrollment inicial del ADMIN existente. El seed por sí solo no permite superar FaceID. Abrir SIGELAB, escribir username y contraseña, e iniciar sesión. Si son válidos y el usuario está ACTIVO, se abre la captura facial. Seguir las indicaciones de cámara y liveness hasta terminar; no iniciar varios intentos simultáneos.

Un rostro distinto, perfil ausente, usuario inactivo, servicio inaccesible o auditoría no disponible deniega el acceso. No hay opción de omitir el segundo factor. Si hay error de persistencia, avisar al responsable técnico sin enviar contraseñas ni capturas de claves. Los usuarios DEMO están documentados únicamente como credenciales de demostración en README.

## Menú y permisos

ADMIN puede administrar todos los módulos. ENCARGADO opera catálogos, préstamos, devoluciones, reservas, mantenimiento, QR y reportes; no administra usuarios ni perfiles faciales ni consulta auditoría. ESTUDIANTE consulta laboratorios y equipos en modo lectura. El menú muestra las opciones autorizadas; permisos y estado se vuelven a validar en operaciones críticas.

Usar **Cerrar sesión** al terminar. Para cambiar de usuario, volver a autenticarse con contraseña y rostro.

## Usuarios y administración facial (ADMIN)

En **Usuarios**, completar nombre, apellido, username único, contraseña, correo y rol, y pulsar **Crear usuario**. Seleccionar una fila para **Activar / Desactivar seleccionado**. No puede desactivarse a sí mismo.

Con la fila del usuario seleccionada:

- **Estado facial** consulta si existe un perfil activo.
- **Registrar rostro** inicia captura para un usuario ACTIVO sin perfil.
- **Reemplazar rostro** requiere confirmación; usar si ya tiene perfil.
- **Eliminar rostro** requiere confirmación y obligará a registrar otro perfil antes del siguiente login.

Comprobar siempre la fila seleccionada. Esperar el resultado mientras los controles están deshabilitados. Si se confirmó una operación pero falló el refresco, consultar **Estado facial** antes de repetir. El bootstrap de consola queda para el ADMIN inicial y no crea usuarios.

## Laboratorios, tipos y equipos

**Laboratorios**: registrar código único, nombre, ubicación, capacidad positiva y estado; consultar/seleccionar para editar o eliminar cuando el historial lo permita. ESTUDIANTE solo consulta.

**Tipos de equipo**: registrar nombre y descripción del catálogo antes de crear equipos. Acceso administrativo según menú.

**Equipos**: completar código de inventario único, nombre, tipo, marca, modelo, serie, laboratorio y observaciones. Un equipo nuevo inicia DISPONIBLE. Seleccionar para editar datos; el CRUD no es el flujo de préstamo, devolución o mantenimiento. La eliminación está restringida a equipos disponibles sin historial.

## QR

Abrir **QR**, introducir el código de un equipo existente y pulsar **Generar QR** para guardar la imagen. Para consultar una imagen existente, pulsar **Leer QR** y seleccionarla. Se identifica el equipo y su disponibilidad; PRESTADO se informa bloqueado. Generar o leer QR no cambia estados ni crea préstamos.

## Préstamos

Abrir **Préstamos** y pulsar **Nuevo préstamo**. Elegir solicitante ACTIVO, fecha esperada de devolución y equipos disponibles; completar observaciones cuando corresponda. Confirmar y verificar el resultado. El usuario registrador es el de la sesión. No se permite prestar un equipo PRESTADO, DANADO, MANTENIMIENTO o BAJA.

Usar **Ver activos**, **Buscar préstamo** y **Ver equipos** para consultar. El alta guarda encabezado, detalles, estados y auditoría juntos; ante error no suponer éxito ni repetir sin consultar.

## Devoluciones

Abrir **Devoluciones**, **Actualizar préstamos**, seleccionar el préstamo y marcar solo sus equipos pendientes. Indicar BUENO o DANADO por equipo y pulsar **Confirmar devolución**. BUENO deja DISPONIBLE; DANADO deja DANADO. No se puede devolver dos veces un mismo detalle.

Para CP-14, registrar previamente un préstamo de DEMO-EQ-001/002/003. Seleccionar dos pendientes y devolverlos juntos (uno BUENO y otro DANADO). Deben aparecer dos devoluciones, el tercero seguir PRESTADO, un único pendiente y préstamo ACTIVO. Solo se finaliza al devolver el último pendiente.

## Mantenimiento

En **Mantenimiento**, pulsar **Ingresar Equipo a Taller**, seleccionar equipo, tipo, responsable y diagnóstico. Para correctivo usar equipo DANADO; preventivo corresponde a DISPONIBLE. Confirmar: debe pasar a MANTENIMIENTO.

Seleccionar una orden abierta y **Finalizar Mantenimiento Seleccionado**. Informar resultado y observaciones: REPARADO deja DISPONIBLE; NO_REPARABLE deja BAJA. **Actualizar** refresca la lista. No se puede cerrar dos veces ni abrir otra orden para un equipo con mantenimiento abierto.

## Dashboard, reportes y auditoría

El inicio muestra el dashboard cuando tiene permiso de reportes. **Actualizar resumen** consulta cantidades reales; una base recién creada todavía no contiene historial operativo.

En **Reportes**, elegir consulta (equipos por estado, préstamos, préstamos por fechas, historial de equipo/equipos o mantenimientos), completar filtros y pulsar **Consultar**. Un préstamo vencido es ACTIVO, con fecha esperada pasada y detalles pendientes; no es un nuevo estado del equipo o préstamo.

En **Auditoría** (ADMIN), consultar historial, filtrar por acción, entidad y usuario con **Buscar**, o **Mostrar Todos**. Comprobar LOGIN_EXITOSO/LOGIN_FALLIDO y eventos de operaciones relevantes. La vista consulta registros, no los edita.

## Reservas (extensión adicional)

El menú incluye Reservas para los roles autorizados. No forma parte de los CP oficiales; no asumir que una reserva crea o cancela automáticamente un préstamo. Su integración con Préstamos no es parte de esta preparación.

## Incidencias y aceptación

Si una operación no termina claramente, consultar su estado antes de repetir. Si falta acceso a un menú, solicitar revisión del rol; no compartir cuentas. Mantener claves y archivos privados fuera de capturas y reportes. Registrar pruebas manuales en [MATRIZ_PRUEBAS](MATRIZ_PRUEBAS.md), con fecha y evidencia, solo después de realizarlas.

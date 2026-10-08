-- SIGELAB: esquema inicial compartido (Java 21 / SQL Server 2022)
-- Fuente: Guía oficial SIGELAB + projectDesign + extensiones simples.
-- Ejecución: DBeaver, editor SQL Server, ejecutar como SCRIPT (no sentencia aislada).
-- Este script crea tablas faltantes; los cambios posteriores se versionan en migrations/.

IF DB_ID(N'SIGELAB') IS NULL
    EXEC(N'CREATE DATABASE SIGELAB');
GO
USE SIGELAB;
GO

-- 1. Catálogo de roles
IF OBJECT_ID(N'dbo.Rol', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Rol (
        id_rol INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Rol PRIMARY KEY,
        nombre NVARCHAR(30) NOT NULL CONSTRAINT UQ_Rol_nombre UNIQUE
    );
END;
GO

-- 2. Usuarios
IF OBJECT_ID(N'dbo.Usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuario (
        id_usuario INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Usuario PRIMARY KEY,
        nombre NVARCHAR(60) NOT NULL,
        apellido NVARCHAR(60) NOT NULL,
        username NVARCHAR(50) NOT NULL CONSTRAINT UQ_Usuario_username UNIQUE,
        password_hash NVARCHAR(255) NOT NULL,
        correo NVARCHAR(120) NOT NULL,
        id_rol INT NOT NULL,
        estado VARCHAR(10) NOT NULL,
        CONSTRAINT FK_Usuario_Rol FOREIGN KEY (id_rol) REFERENCES dbo.Rol(id_rol),
        CONSTRAINT CK_Usuario_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
    );
END;
GO

-- 3. Laboratorios
IF OBJECT_ID(N'dbo.Laboratorio', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Laboratorio (
        id_laboratorio INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Laboratorio PRIMARY KEY,
        codigo NVARCHAR(20) NOT NULL CONSTRAINT UQ_Laboratorio_codigo UNIQUE,
        nombre NVARCHAR(80) NOT NULL,
        ubicacion NVARCHAR(120) NOT NULL,
        capacidad INT NOT NULL,
        estado VARCHAR(10) NOT NULL,
        CONSTRAINT CK_Laboratorio_capacidad CHECK (capacidad > 0),
        CONSTRAINT CK_Laboratorio_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
    );
END;
GO

-- 4. Tipos de equipo
IF OBJECT_ID(N'dbo.TipoEquipo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TipoEquipo (
        id_tipo_equipo INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TipoEquipo PRIMARY KEY,
        nombre NVARCHAR(60) NOT NULL CONSTRAINT UQ_TipoEquipo_nombre UNIQUE,
        descripcion NVARCHAR(200) NULL
    );
END;
GO

-- 5. Equipos
IF OBJECT_ID(N'dbo.Equipo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Equipo (
        id_equipo INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Equipo PRIMARY KEY,
        codigo_inventario NVARCHAR(30) NOT NULL CONSTRAINT UQ_Equipo_codigo_inventario UNIQUE,
        nombre NVARCHAR(100) NOT NULL,
        id_tipo_equipo INT NOT NULL,
        marca NVARCHAR(60) NOT NULL,
        modelo NVARCHAR(60) NOT NULL,
        numero_serie NVARCHAR(80) NOT NULL,
        id_laboratorio INT NOT NULL,
        estado VARCHAR(20) NOT NULL,
        observaciones NVARCHAR(250) NULL,
        CONSTRAINT FK_Equipo_TipoEquipo FOREIGN KEY (id_tipo_equipo) REFERENCES dbo.TipoEquipo(id_tipo_equipo),
        CONSTRAINT FK_Equipo_Laboratorio FOREIGN KEY (id_laboratorio) REFERENCES dbo.Laboratorio(id_laboratorio),
        CONSTRAINT CK_Equipo_estado CHECK (estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA'))
    );
END;
GO

-- 6. Encabezado de préstamo
IF OBJECT_ID(N'dbo.Prestamo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Prestamo (
        id_prestamo INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Prestamo PRIMARY KEY,
        id_usuario_solicitante INT NOT NULL,
        id_usuario_registra INT NOT NULL,
        fecha_prestamo DATE NOT NULL,
        fecha_esperada_devolucion DATE NOT NULL,
        estado VARCHAR(15) NOT NULL,
        observaciones NVARCHAR(250) NULL,
        CONSTRAINT FK_Prestamo_UsuarioSolicitante FOREIGN KEY (id_usuario_solicitante) REFERENCES dbo.Usuario(id_usuario),
        CONSTRAINT FK_Prestamo_UsuarioRegistra FOREIGN KEY (id_usuario_registra) REFERENCES dbo.Usuario(id_usuario),
        CONSTRAINT CK_Prestamo_estado CHECK (estado IN ('ACTIVO','FINALIZADO')),
        CONSTRAINT CK_Prestamo_fechas CHECK (fecha_esperada_devolucion >= fecha_prestamo)
    );
END;
GO

-- 7. Equipos asociados al préstamo
IF OBJECT_ID(N'dbo.DetallePrestamo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DetallePrestamo (
        id_detalle_prestamo INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DetallePrestamo PRIMARY KEY,
        id_prestamo INT NOT NULL,
        id_equipo INT NOT NULL,
        CONSTRAINT FK_DetallePrestamo_Prestamo FOREIGN KEY (id_prestamo) REFERENCES dbo.Prestamo(id_prestamo),
        CONSTRAINT FK_DetallePrestamo_Equipo FOREIGN KEY (id_equipo) REFERENCES dbo.Equipo(id_equipo),
        CONSTRAINT UQ_DetallePrestamo_PrestamoEquipo UNIQUE (id_prestamo, id_equipo)
    );
END;
GO

-- 8. Devoluciones individuales
IF OBJECT_ID(N'dbo.Devolucion', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Devolucion (
        id_devolucion INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Devolucion PRIMARY KEY,
        id_detalle_prestamo INT NOT NULL CONSTRAINT UQ_Devolucion_DetallePrestamo UNIQUE,
        id_usuario INT NOT NULL,
        fecha_devolucion DATE NOT NULL,
        condicion VARCHAR(10) NOT NULL,
        observaciones NVARCHAR(250) NULL,
        CONSTRAINT FK_Devolucion_DetallePrestamo FOREIGN KEY (id_detalle_prestamo) REFERENCES dbo.DetallePrestamo(id_detalle_prestamo),
        CONSTRAINT FK_Devolucion_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario),
        CONSTRAINT CK_Devolucion_condicion CHECK (condicion IN ('BUENO','DANADO'))
    );
END;
GO

-- 9. Mantenimientos
IF OBJECT_ID(N'dbo.Mantenimiento', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Mantenimiento (
        id_mantenimiento INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Mantenimiento PRIMARY KEY,
        id_equipo INT NOT NULL,
        fecha_ingreso DATE NOT NULL,
        tipo VARCHAR(12) NOT NULL,
        descripcion NVARCHAR(300) NOT NULL,
        id_responsable INT NOT NULL,
        fecha_salida DATE NULL,
        resultado VARCHAR(15) NULL,
        estado VARCHAR(10) NOT NULL,
        observaciones NVARCHAR(250) NULL,
        CONSTRAINT FK_Mantenimiento_Equipo FOREIGN KEY (id_equipo) REFERENCES dbo.Equipo(id_equipo),
        CONSTRAINT FK_Mantenimiento_Responsable FOREIGN KEY (id_responsable) REFERENCES dbo.Usuario(id_usuario),
        CONSTRAINT CK_Mantenimiento_tipo CHECK (tipo IN ('PREVENTIVO','CORRECTIVO')),
        CONSTRAINT CK_Mantenimiento_resultado CHECK (resultado IS NULL OR resultado IN ('REPARADO','NO_REPARABLE')),
        CONSTRAINT CK_Mantenimiento_estado CHECK (estado IN ('ABIERTO','CERRADO'))
    );
END;
GO

-- 10. Auditoría general
IF OBJECT_ID(N'dbo.Auditoria', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Auditoria (
        id_auditoria INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Auditoria PRIMARY KEY,
        id_usuario INT NULL,
        fecha_hora DATETIME2 NOT NULL,
        accion VARCHAR(50) NOT NULL,
        entidad VARCHAR(50) NOT NULL,
        id_registro INT NULL,
        descripcion NVARCHAR(500) NOT NULL,
        CONSTRAINT FK_Auditoria_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario)
    );
END;
GO

-- 11. Perfil biométrico (embedding debe llegar cifrado desde servicio facial)
IF OBJECT_ID(N'dbo.PerfilFacial', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PerfilFacial (
        id_perfil_facial INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PerfilFacial PRIMARY KEY,
        id_usuario INT NOT NULL CONSTRAINT UQ_PerfilFacial_Usuario UNIQUE,
        embedding VARBINARY(MAX) NOT NULL,
        modelo NVARCHAR(50) NOT NULL,
        dimension INT NOT NULL,
        cantidad_muestras INT NOT NULL,
        quality_score DECIMAL(6,4) NULL,
        fecha_registro DATETIME2 NOT NULL,
        fecha_actualizacion DATETIME2 NULL,
        activo BIT NOT NULL,
        CONSTRAINT FK_PerfilFacial_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario),
        CONSTRAINT CK_PerfilFacial_dimension CHECK (dimension > 0),
        CONSTRAINT CK_PerfilFacial_muestras CHECK (cantidad_muestras > 0)
    );
END;
GO

-- 12. Auditoría biométrica
IF OBJECT_ID(N'dbo.AuditoriaFacial', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AuditoriaFacial (
        id_auditoria_facial INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AuditoriaFacial PRIMARY KEY,
        id_usuario INT NOT NULL,
        fecha_hora DATETIME2 NOT NULL,
        accion VARCHAR(40) NOT NULL,
        resultado VARCHAR(30) NOT NULL,
        score DECIMAL(8,5) NULL,
        threshold DECIMAL(8,5) NULL,
        detalle NVARCHAR(500) NULL,
        dispositivo NVARCHAR(120) NULL,
        CONSTRAINT FK_AuditoriaFacial_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario)
    );
END;
GO

-- 13. EXTENSIÓN DEL EQUIPO: Reservas simples
IF OBJECT_ID(N'dbo.Reserva', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Reserva (
        id_reserva INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Reserva PRIMARY KEY,
        id_usuario INT NOT NULL,
        id_equipo INT NOT NULL,
        fecha_inicio DATE NOT NULL,
        fecha_fin DATE NOT NULL,
        estado VARCHAR(12) NOT NULL,
        fecha_creacion DATETIME2 NOT NULL CONSTRAINT DF_Reserva_fecha_creacion DEFAULT SYSDATETIME(),
        observaciones NVARCHAR(250) NULL,
        CONSTRAINT FK_Reserva_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario),
        CONSTRAINT FK_Reserva_Equipo FOREIGN KEY (id_equipo) REFERENCES dbo.Equipo(id_equipo),
        CONSTRAINT CK_Reserva_estado CHECK (estado IN ('ACTIVA','CANCELADA','COMPLETADA')),
        CONSTRAINT CK_Reserva_fechas CHECK (fecha_fin >= fecha_inicio)
    );
END;
GO

-- 14. EXTENSIÓN DEL EQUIPO: Notificaciones internas
IF OBJECT_ID(N'dbo.Notificacion', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notificacion (
        id_notificacion INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notificacion PRIMARY KEY,
        id_usuario INT NOT NULL,
        tipo VARCHAR(30) NOT NULL,
        titulo NVARCHAR(100) NOT NULL,
        mensaje NVARCHAR(300) NOT NULL,
        fecha_hora DATETIME2 NOT NULL CONSTRAINT DF_Notificacion_fecha_hora DEFAULT SYSDATETIME(),
        leida BIT NOT NULL CONSTRAINT DF_Notificacion_leida DEFAULT (0),
        entidad VARCHAR(30) NULL,
        id_referencia INT NULL,
        CONSTRAINT FK_Notificacion_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario)
    );
END;
GO

-- Las reglas entre varias filas/tablas (solapamiento de reservas,
-- préstamo duplicado activo, transiciones de estados) se validan en Service
-- y dentro de transacciones. Las restricciones SQL son defensa adicional.
SELECT name AS tabla
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo')
ORDER BY name;
GO

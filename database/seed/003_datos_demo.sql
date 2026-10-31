-- Solo datos DEMO; ejecutar después de 001_roles.sql y 002_estudiante.sql.
-- No crea perfiles faciales ni restablece contraseñas/estados de datos existentes.
USE SIGELAB;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @admin INT = (SELECT id_rol FROM dbo.Rol WHERE nombre = N'ADMIN');
DECLARE @solicitante INT = (SELECT id_rol FROM dbo.Rol WHERE nombre = N'ESTUDIANTE');
IF @admin IS NULL OR @solicitante IS NULL
BEGIN
    ROLLBACK;
    THROW 50001, 'Ejecute primero los seeds de roles.', 1;
END;
DECLARE @hash NVARCHAR(255) = N'$2a$10$qycND3nS.kw22GU0p5BsbOndMLo7AEnbJcrE4.5DRW.FcyF0kaFR2';
IF NOT EXISTS (SELECT 1 FROM dbo.Usuario WHERE username = N'admin.demo')
    INSERT INTO dbo.Usuario (nombre, apellido, username, password_hash, correo, id_rol, estado)
    VALUES (N'Administrador', N'Demo', N'admin.demo', @hash, N'admin@sigelab.example', @admin, 'ACTIVO');
IF NOT EXISTS (SELECT 1 FROM dbo.Usuario WHERE username = N'solicitante.demo')
    INSERT INTO dbo.Usuario (nombre, apellido, username, password_hash, correo, id_rol, estado)
    VALUES (N'Solicitante', N'Demo', N'solicitante.demo', @hash, N'solicitante@sigelab.example', @solicitante, 'ACTIVO');
IF NOT EXISTS (SELECT 1 FROM dbo.Laboratorio WHERE codigo = N'LAB-DEMO')
    INSERT INTO dbo.Laboratorio (codigo, nombre, ubicacion, capacidad, estado)
    VALUES (N'LAB-DEMO', N'Laboratorio demo', N'Campus demo', 20, 'ACTIVO');
IF NOT EXISTS (SELECT 1 FROM dbo.TipoEquipo WHERE nombre = N'Portátil demo')
    INSERT INTO dbo.TipoEquipo (nombre, descripcion) VALUES (N'Portátil demo', N'Equipo para aceptación');
DECLARE @lab INT = (SELECT id_laboratorio FROM dbo.Laboratorio WHERE codigo = N'LAB-DEMO');
DECLARE @tipo INT = (SELECT id_tipo_equipo FROM dbo.TipoEquipo WHERE nombre = N'Portátil demo');
INSERT INTO dbo.Equipo (codigo_inventario, nombre, id_tipo_equipo, marca, modelo, numero_serie, id_laboratorio, estado, observaciones)
SELECT v.codigo, N'Portátil ' + v.codigo, @tipo, N'Demo', N'Demo 2026', N'SERIE-' + v.codigo, @lab, 'DISPONIBLE', N'Datos demo'
FROM (VALUES (N'DEMO-EQ-001'), (N'DEMO-EQ-002'), (N'DEMO-EQ-003'), (N'DEMO-EQ-004')) v(codigo)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Equipo e WHERE e.codigo_inventario = v.codigo);
COMMIT;
GO

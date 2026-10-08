-- Ejecutar DESPUÉS de database/sigelab.sql.
USE SIGELAB;
GO
IF NOT EXISTS (SELECT 1 FROM dbo.Rol WHERE nombre = N'ADMIN')
    INSERT INTO dbo.Rol (nombre) VALUES (N'ADMIN');
IF NOT EXISTS (SELECT 1 FROM dbo.Rol WHERE nombre = N'ENCARGADO')
    INSERT INTO dbo.Rol (nombre) VALUES (N'ENCARGADO');
GO
SELECT id_rol, nombre FROM dbo.Rol ORDER BY id_rol;
GO

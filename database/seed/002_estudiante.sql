-- Datos del rol de consulta; ejecutar después de 001_roles.sql.
USE SIGELAB;
GO
IF NOT EXISTS (SELECT 1 FROM dbo.Rol WHERE nombre = N'ESTUDIANTE')
    INSERT INTO dbo.Rol (nombre) VALUES (N'ESTUDIANTE');
GO

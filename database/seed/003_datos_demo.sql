-- Solo datos DEMO; ejecutar después de 001_roles.sql y 002_estudiante.sql.
-- No crea perfiles faciales ni restablece contraseñas/estados de datos existentes.
USE SIGELAB;
GO
-- Un único EXEC mantiene validación e inserciones en el mismo batch,
-- incluso si el cliente divide el script por sentencias. No usa variables locales.
EXEC(N'
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM dbo.Rol WHERE nombre = N''ADMIN'')
       OR NOT EXISTS (SELECT 1 FROM dbo.Rol WHERE nombre = N''ESTUDIANTE'')
    BEGIN
        THROW 50001, N''Faltan los roles ADMIN o ESTUDIANTE. Ejecute primero 001_roles.sql y 002_estudiante.sql.'', 1;
    END;

    INSERT INTO dbo.Usuario (nombre, apellido, username, password_hash, correo, id_rol, estado)
    SELECT N''Administrador'', N''Demo'', N''admin.demo'',
           N''$2a$10$qycND3nS.kw22GU0p5BsbOndMLo7AEnbJcrE4.5DRW.FcyF0kaFR2'',
           N''admin@sigelab.example'', r.id_rol, ''ACTIVO''
    FROM dbo.Rol r
    WHERE r.nombre = N''ADMIN''
      AND NOT EXISTS (SELECT 1 FROM dbo.Usuario WHERE username = N''admin.demo'');

    INSERT INTO dbo.Usuario (nombre, apellido, username, password_hash, correo, id_rol, estado)
    SELECT N''Solicitante'', N''Demo'', N''solicitante.demo'',
           N''$2a$10$qycND3nS.kw22GU0p5BsbOndMLo7AEnbJcrE4.5DRW.FcyF0kaFR2'',
           N''solicitante@sigelab.example'', r.id_rol, ''ACTIVO''
    FROM dbo.Rol r
    WHERE r.nombre = N''ESTUDIANTE''
      AND NOT EXISTS (SELECT 1 FROM dbo.Usuario WHERE username = N''solicitante.demo'');

    INSERT INTO dbo.Laboratorio (codigo, nombre, ubicacion, capacidad, estado)
    SELECT N''LAB-DEMO'', N''Laboratorio demo'', N''Campus demo'', 20, ''ACTIVO''
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Laboratorio WHERE codigo = N''LAB-DEMO'');

    INSERT INTO dbo.TipoEquipo (nombre, descripcion)
    SELECT N''Portátil demo'', N''Equipo para aceptación''
    WHERE NOT EXISTS (SELECT 1 FROM dbo.TipoEquipo WHERE nombre = N''Portátil demo'');

    INSERT INTO dbo.Equipo (codigo_inventario, nombre, id_tipo_equipo, marca, modelo, numero_serie, id_laboratorio, estado, observaciones)
    SELECT v.codigo, N''Portátil '' + v.codigo, t.id_tipo_equipo,
           N''Demo'', N''Demo 2026'', N''SERIE-'' + v.codigo,
           l.id_laboratorio, ''DISPONIBLE'', N''Datos demo''
    FROM (VALUES (N''DEMO-EQ-001''), (N''DEMO-EQ-002''), (N''DEMO-EQ-003''), (N''DEMO-EQ-004'')) v(codigo)
    CROSS JOIN dbo.TipoEquipo t
    CROSS JOIN dbo.Laboratorio l
    WHERE t.nombre = N''Portátil demo''
      AND l.codigo = N''LAB-DEMO''
      AND NOT EXISTS (SELECT 1 FROM dbo.Equipo e WHERE e.codigo_inventario = v.codigo);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
');
GO

package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoEquipo;
import sigelab.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.sql.Date;
import sigelab.enums.EstadoMantenimiento;

public class ReporteRepository {

    public List<Map<String, Object>> listarEquiposPorEstado(EstadoEquipo estado) {
        String sql = """
                SELECT id_equipo, codigo_inventario, nombre, marca,
                       modelo, numero_serie, estado
                FROM dbo.Equipo
                WHERE estado = ?
                ORDER BY nombre ASC
                """;

        List<Map<String, Object>> resultados = new ArrayList<>();

        try (Connection conexion = DatabaseConfig.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, estado.name());

            try (ResultSet rs = sentencia.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();

                    fila.put("id_equipo", rs.getInt("id_equipo"));
                    fila.put("codigo_inventario", rs.getString("codigo_inventario"));
                    fila.put("nombre", rs.getString("nombre"));
                    fila.put("marca", rs.getString("marca"));
                    fila.put("modelo", rs.getString("modelo"));
                    fila.put("numero_serie", rs.getString("numero_serie"));
                    fila.put("estado", rs.getString("estado"));

                    resultados.add(fila);
                }
            }

            return resultados;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar el reporte de equipos por estado.", e);
        }
    }

    public List<Map<String, Object>> listarPrestamosConEstadoReporte() {
        String sql = """
            SELECT
                p.id_prestamo,
                p.id_usuario_solicitante,
                p.id_usuario_registra,
                p.fecha_prestamo,
                p.fecha_esperada_devolucion,
                p.estado,
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM dbo.DetallePrestamo dp
                    WHERE dp.id_prestamo = p.id_prestamo
                      AND NOT EXISTS (
                          SELECT 1
                          FROM dbo.Devolucion d
                          WHERE d.id_detalle_prestamo = dp.id_detalle_prestamo
                      )
                ) THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS tiene_detalle_pendiente
            FROM dbo.Prestamo p
            WHERE p.estado IN ('ACTIVO', 'FINALIZADO')
            ORDER BY p.fecha_esperada_devolucion ASC, p.id_prestamo ASC
            """;

        List<Map<String, Object>> resultados = new ArrayList<>();
        try (Connection conexion = DatabaseConfig.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet rs = sentencia.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("id_prestamo", rs.getInt("id_prestamo"));
                fila.put("id_usuario_solicitante", rs.getInt("id_usuario_solicitante"));
                fila.put("id_usuario_registra", rs.getInt("id_usuario_registra"));
                fila.put("fecha_prestamo", rs.getDate("fecha_prestamo"));
                fila.put("fecha_esperada_devolucion", rs.getDate("fecha_esperada_devolucion"));
                fila.put("estado", rs.getString("estado"));
                fila.put("tiene_detalle_pendiente", rs.getBoolean("tiene_detalle_pendiente"));
                resultados.add(fila);
            }
            return resultados;
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar el reporte de préstamos.", e);
        }
    }

    public List<Map<String, Object>> listarPrestamosEntreFechas(
            LocalDate fechaInicio, LocalDate fechaFin) {

        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException(
                    "Las fechas de inicio y fin son obligatorias.");
        }

        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior a la inicial.");
        }

        String sql = """
            SELECT
                p.id_prestamo,
                p.id_usuario_solicitante,
                p.id_usuario_registra,
                p.fecha_prestamo,
                p.fecha_esperada_devolucion,
                p.estado,
                p.observaciones,
                CASE WHEN EXISTS (
                    SELECT 1 FROM dbo.DetallePrestamo dp
                    WHERE dp.id_prestamo = p.id_prestamo
                      AND NOT EXISTS (
                          SELECT 1 FROM dbo.Devolucion d
                          WHERE d.id_detalle_prestamo = dp.id_detalle_prestamo
                      )
                ) THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS tiene_detalle_pendiente
            FROM dbo.Prestamo p
            WHERE p.fecha_prestamo BETWEEN ? AND ?
            ORDER BY p.fecha_prestamo ASC, p.id_prestamo ASC
            """;

        List<Map<String, Object>> resultados = new ArrayList<>();

        try (Connection conexion = DatabaseConfig.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setDate(1, Date.valueOf(fechaInicio));
            sentencia.setDate(2, Date.valueOf(fechaFin));

            try (ResultSet rs = sentencia.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();

                    fila.put("id_prestamo", rs.getInt("id_prestamo"));
                    fila.put("id_usuario_solicitante",
                            rs.getInt("id_usuario_solicitante"));
                    fila.put("id_usuario_registra",
                            rs.getInt("id_usuario_registra"));
                    fila.put("fecha_prestamo", rs.getDate("fecha_prestamo"));
                    fila.put("fecha_esperada_devolucion",
                            rs.getDate("fecha_esperada_devolucion"));
                    fila.put("estado", rs.getString("estado"));
                    fila.put("observaciones", rs.getString("observaciones"));
                    fila.put("tiene_detalle_pendiente", rs.getBoolean("tiene_detalle_pendiente"));

                    resultados.add(fila);
                }
            }

            return resultados;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar préstamos entre fechas.", e);
        }
    }

    public List<Map<String, Object>> listarHistorialEquipos() {
        return consultarHistorialEquipos(null);
    }

    public List<Map<String, Object>> listarHistorialEquipo(String codigoInventario) {
        if (codigoInventario == null || codigoInventario.isBlank()) {
            throw new IllegalArgumentException("El código de inventario es obligatorio");
        }
        return consultarHistorialEquipos(codigoInventario);
    }

    private List<Map<String, Object>> consultarHistorialEquipos(String codigoInventario) {
        String sql = """
            SELECT
                e.id_equipo,
                e.codigo_inventario,
                e.nombre AS nombre_equipo,
                e.estado AS estado_equipo,
                p.id_prestamo,
                p.fecha_prestamo,
                p.fecha_esperada_devolucion,
                p.estado AS estado_prestamo,
                dp.id_detalle_prestamo,
                d.fecha_devolucion,
                d.condicion AS condicion_devolucion
            FROM dbo.Equipo e
            LEFT JOIN dbo.DetallePrestamo dp
                ON dp.id_equipo = e.id_equipo
            LEFT JOIN dbo.Prestamo p
                ON p.id_prestamo = dp.id_prestamo
            LEFT JOIN dbo.Devolucion d
                ON d.id_detalle_prestamo = dp.id_detalle_prestamo
            """;
        // Solo se agregan fragmentos constantes; la entrada se enlaza como parámetro JDBC.
        if (codigoInventario != null) {
            sql += " WHERE e.codigo_inventario = ?";
        }
        sql += " ORDER BY e.codigo_inventario ASC, p.fecha_prestamo DESC, p.id_prestamo DESC";

        List<Map<String, Object>> resultados = new ArrayList<>();

        try (Connection conexion = DatabaseConfig.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (codigoInventario != null) {
                sentencia.setString(1, codigoInventario);
            }
            try (ResultSet rs = sentencia.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();

                    fila.put("id_equipo", rs.getInt("id_equipo"));
                    fila.put("codigo_inventario", rs.getString("codigo_inventario"));
                    fila.put("nombre_equipo", rs.getString("nombre_equipo"));
                    fila.put("estado_equipo", rs.getString("estado_equipo"));
                    fila.put("id_prestamo", rs.getObject("id_prestamo"));
                    fila.put("fecha_prestamo", rs.getDate("fecha_prestamo"));
                    fila.put("fecha_esperada_devolucion",
                            rs.getDate("fecha_esperada_devolucion"));
                    fila.put("estado_prestamo", rs.getString("estado_prestamo"));
                    fila.put("id_detalle_prestamo",
                            rs.getObject("id_detalle_prestamo"));
                    fila.put("fecha_devolucion", rs.getDate("fecha_devolucion"));
                    fila.put("condicion_devolucion",
                            rs.getString("condicion_devolucion"));

                    resultados.add(fila);
                }

                return resultados;
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar el historial de equipos.", e);
        }
    }

    public List<Map<String, Object>> listarHistorialMantenimientos(
            EstadoMantenimiento estado) {

        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado del mantenimiento es obligatorio.");
        }

        String sql = """
            SELECT
                m.id_mantenimiento,
                m.id_equipo,
                e.codigo_inventario,
                e.nombre AS nombre_equipo,
                m.fecha_ingreso,
                m.tipo,
                m.descripcion,
                m.id_responsable,
                m.fecha_salida,
                m.resultado,
                m.estado AS estado_mantenimiento,
                m.observaciones
            FROM dbo.Mantenimiento m
            INNER JOIN dbo.Equipo e
                ON e.id_equipo = m.id_equipo
            WHERE m.estado = ?
            ORDER BY m.fecha_ingreso DESC,
                     m.id_mantenimiento DESC
            """;

        List<Map<String, Object>> resultados = new ArrayList<>();

        try (Connection conexion = DatabaseConfig.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, estado.name());

            try (ResultSet rs = sentencia.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();

                    fila.put("id_mantenimiento",
                            rs.getInt("id_mantenimiento"));
                    fila.put("id_equipo", rs.getInt("id_equipo"));
                    fila.put("codigo_inventario",
                            rs.getString("codigo_inventario"));
                    fila.put("nombre_equipo",
                            rs.getString("nombre_equipo"));
                    fila.put("fecha_ingreso", rs.getDate("fecha_ingreso"));
                    fila.put("tipo", rs.getString("tipo"));
                    fila.put("descripcion", rs.getString("descripcion"));
                    fila.put("id_responsable", rs.getInt("id_responsable"));
                    fila.put("fecha_salida", rs.getDate("fecha_salida"));
                    fila.put("resultado", rs.getString("resultado"));
                    fila.put("estado_mantenimiento",
                            rs.getString("estado_mantenimiento"));
                    fila.put("observaciones",
                            rs.getString("observaciones"));

                    resultados.add(fila);
                }
            }

            return resultados;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar el historial de mantenimientos.", e);
        }
    }


    public Map<String, Object> obtenerConteosDashboard() {
        return obtenerConteosDashboard(LocalDate.now());
    }

    public Map<String, Object> obtenerConteosDashboard(LocalDate hoy) {
        Map<String, Object> conteos = new LinkedHashMap<>();

        // Inicializar todos los estados en cero.
        for (EstadoEquipo estado : EstadoEquipo.values()) {
            conteos.put("equipos_" + estado.name(), 0);
        }

        conteos.put("prestamos_ACTIVO", 0);
        conteos.put("prestamos_FINALIZADO", 0);
        conteos.put("prestamos_VENCIDO", 0);

        String sqlEquipos = """
            SELECT estado, COUNT(*) AS total
            FROM dbo.Equipo
            GROUP BY estado
            """;

        String sqlPrestamos = """
            SELECT estado, COUNT(*) AS total
            FROM dbo.Prestamo
            GROUP BY estado
            """;

        String sqlVencidos = """
            SELECT COUNT(*) AS total
            FROM dbo.Prestamo p
            WHERE p.estado = 'ACTIVO'
              AND p.fecha_esperada_devolucion < ?
              AND EXISTS (
                  SELECT 1
                  FROM dbo.DetallePrestamo dp
                  WHERE dp.id_prestamo = p.id_prestamo
                    AND NOT EXISTS (
                        SELECT 1
                        FROM dbo.Devolucion d
                        WHERE d.id_detalle_prestamo =
                              dp.id_detalle_prestamo
                    )
              )
            """;

        try (Connection conexion = DatabaseConfig.getConnection()) {

            try (PreparedStatement sentencia =
                         conexion.prepareStatement(sqlEquipos);
                 ResultSet rs = sentencia.executeQuery()) {

                while (rs.next()) {
                    conteos.put(
                            "equipos_" + rs.getString("estado"),
                            rs.getInt("total"));
                }
            }

            try (PreparedStatement sentencia =
                         conexion.prepareStatement(sqlPrestamos);
                 ResultSet rs = sentencia.executeQuery()) {

                while (rs.next()) {
                    conteos.put(
                            "prestamos_" + rs.getString("estado"),
                            rs.getInt("total"));
                }
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sqlVencidos)) {
                sentencia.setDate(1, Date.valueOf(hoy));
                try (ResultSet rs = sentencia.executeQuery()) {
                    if (rs.next()) {
                        conteos.put("prestamos_VENCIDO", rs.getInt("total"));
                    }
                }
            }

            int totalEquipos = 0;
            for (EstadoEquipo estado : EstadoEquipo.values()) {
                totalEquipos += ((Number) conteos.get("equipos_" + estado.name())).intValue();
            }
            conteos.put("equipos_TOTAL", totalEquipos);
            return conteos;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar los conteos del dashboard.", e);
        }
    }

}


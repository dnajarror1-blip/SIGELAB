package sigelab.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoMantenimiento;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Mantenimiento;

public class MantenimientoRepository {

    public Mantenimiento guardar(Mantenimiento m) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return guardar(m, conn);
        }
    }

    public Mantenimiento guardar(Mantenimiento m, Connection conn) throws SQLException {
        String sql = "INSERT INTO dbo.Mantenimiento (id_equipo, fecha_ingreso, tipo, descripcion, id_responsable, "
                   + "fecha_salida, resultado, estado, observaciones) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, m.getIdEquipo());
            stmt.setDate(2, Date.valueOf(m.getFechaIngreso() != null ? m.getFechaIngreso() : LocalDate.now()));
            stmt.setString(3, m.getTipo().name());
            stmt.setString(4, m.getDescripcion());
            stmt.setInt(5, m.getIdResponsable());

            if (m.getFechaSalida() != null) {
                stmt.setDate(6, Date.valueOf(m.getFechaSalida()));
            } else {
                stmt.setNull(6, Types.DATE);
            }

            if (m.getResultado() != null) {
                stmt.setString(7, m.getResultado().name());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }

            stmt.setString(8, m.getEstado().name());
            stmt.setString(9, m.getObservaciones());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    m.setIdMantenimiento(rs.getInt(1));
                }
            }
        }
        return m;
    }

    public void actualizar(Mantenimiento m) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            actualizar(m, conn);
        }
    }

    public void actualizar(Mantenimiento m, Connection conn) throws SQLException {
        String sql = "UPDATE dbo.Mantenimiento SET fecha_salida = ?, resultado = ?, estado = ?, observaciones = ? "
                   + "WHERE id_mantenimiento = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (m.getFechaSalida() != null) {
                stmt.setDate(1, Date.valueOf(m.getFechaSalida()));
            } else {
                stmt.setNull(1, Types.DATE);
            }

            if (m.getResultado() != null) {
                stmt.setString(2, m.getResultado().name());
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }

            stmt.setString(3, m.getEstado().name());
            stmt.setString(4, m.getObservaciones());
            stmt.setInt(5, m.getIdMantenimiento());

            stmt.executeUpdate();
        }
    }

    public Optional<Mantenimiento> buscarPorId(int idMantenimiento) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return buscarPorId(idMantenimiento, conn);
        }
    }

    public Optional<Mantenimiento> buscarPorId(int idMantenimiento, Connection conn) throws SQLException {
        String sql = "SELECT id_mantenimiento, id_equipo, fecha_ingreso, tipo, descripcion, id_responsable, "
                   + "fecha_salida, resultado, estado, observaciones FROM dbo.Mantenimiento WHERE id_mantenimiento = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMantenimiento);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearMantenimiento(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Mantenimiento> listarTodos() throws SQLException {
        String sql = "SELECT id_mantenimiento, id_equipo, fecha_ingreso, tipo, descripcion, id_responsable, "
                   + "fecha_salida, resultado, estado, observaciones FROM dbo.Mantenimiento ORDER BY fecha_ingreso DESC";
        List<Mantenimiento> lista = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearMantenimiento(rs));
            }
        }
        return lista;
    }

    public List<Mantenimiento> listarPorEstado(EstadoMantenimiento estado) throws SQLException {
        String sql = "SELECT id_mantenimiento, id_equipo, fecha_ingreso, tipo, descripcion, id_responsable, "
                   + "fecha_salida, resultado, estado, observaciones FROM dbo.Mantenimiento WHERE estado = ? ORDER BY fecha_ingreso DESC";
        List<Mantenimiento> lista = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, estado.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMantenimiento(rs));
                }
            }
        }
        return lista;
    }

    public List<Mantenimiento> listarPorEquipo(int idEquipo) throws SQLException {
        String sql = "SELECT id_mantenimiento, id_equipo, fecha_ingreso, tipo, descripcion, id_responsable, "
                   + "fecha_salida, resultado, estado, observaciones FROM dbo.Mantenimiento WHERE id_equipo = ? ORDER BY fecha_ingreso DESC";
        List<Mantenimiento> lista = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idEquipo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMantenimiento(rs));
                }
            }
        }
        return lista;
    }

    private Mantenimiento mapearMantenimiento(ResultSet rs) throws SQLException {
        Mantenimiento m = new Mantenimiento();
        m.setIdMantenimiento(rs.getInt("id_mantenimiento"));
        m.setIdEquipo(rs.getInt("id_equipo"));

        Date fechaIngreso = rs.getDate("fecha_ingreso");
        if (fechaIngreso != null) {
            m.setFechaIngreso(fechaIngreso.toLocalDate());
        }

        String tipoStr = rs.getString("tipo");
        if (tipoStr != null) {
            m.setTipo(TipoMantenimiento.valueOf(tipoStr));
        }

        m.setDescripcion(rs.getString("descripcion"));
        m.setIdResponsable(rs.getInt("id_responsable"));

        Date fechaSalida = rs.getDate("fecha_salida");
        if (fechaSalida != null) {
            m.setFechaSalida(fechaSalida.toLocalDate());
        }

        String resultadoStr = rs.getString("resultado");
        if (resultadoStr != null) {
            m.setResultado(ResultadoMantenimiento.valueOf(resultadoStr));
        }

        String estadoStr = rs.getString("estado");
        if (estadoStr != null) {
            m.setEstado(EstadoMantenimiento.valueOf(estadoStr));
        }

        m.setObservaciones(rs.getString("observaciones"));
        return m;
    }
}

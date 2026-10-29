package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.enums.TipoNotificacion;
import sigelab.exception.DataAccessException;
import sigelab.model.Notificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificacionRepository {

    // Guarda una notificación nueva y le pone el id que generó la base
    public void guardar(Notificacion n) {
        String sql = "INSERT INTO dbo.Notificacion (id_usuario, tipo, titulo, mensaje, fecha_hora, leida, entidad, id_referencia) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, n.getIdUsuario());
            stmt.setString(2, n.getTipo().name());
            stmt.setString(3, n.getTitulo());
            stmt.setString(4, n.getMensaje());

            // Si no trae fecha, usamos la de ahora
            LocalDateTime fecha = n.getFechaHora() != null ? n.getFechaHora() : LocalDateTime.now();
            stmt.setTimestamp(5, Timestamp.valueOf(fecha));

            stmt.setBoolean(6, n.isLeida());

            // entidad e id_referencia son opcionales, pueden ir vacíos
            if (n.getEntidad() != null) {
                stmt.setString(7, n.getEntidad());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }
            if (n.getIdReferencia() != null) {
                stmt.setInt(8, n.getIdReferencia());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.executeUpdate();

            // Recogemos el id que la base le asignó
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    n.setIdNotificacion(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la notificación", e);
        }
    }

    // Todas las notificaciones de un usuario, las más nuevas primero
    public List<Notificacion> listarPorUsuario(int idUsuario) {
        String sql = "SELECT id_notificacion, id_usuario, tipo, titulo, mensaje, fecha_hora, leida, entidad, id_referencia "
                + "FROM dbo.Notificacion WHERE id_usuario = ? ORDER BY fecha_hora DESC, id_notificacion DESC";
        List<Notificacion> lista = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las notificaciones", e);
        }
        return lista;
    }

    // Cuántas notificaciones le faltan por leer (para el numerito del menú)
    public int contarNoLeidas(int idUsuario) {
        String sql = "SELECT COUNT(*) FROM dbo.Notificacion WHERE id_usuario = ? AND leida = 0";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar las notificaciones", e);
        }
    }

    // Marca una notificación como leída. Solo funciona si le pertenece a ese usuario.
    // Devuelve true si encontró y actualizó alguna.
    public boolean marcarComoLeida(int idNotificacion, int idUsuario) {
        String sql = "UPDATE dbo.Notificacion SET leida = 1 WHERE id_notificacion = ? AND id_usuario = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idNotificacion);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo marcar la notificación como leída", e);
        }
    }

    // Marca todas las pendientes de un usuario como leídas. Devuelve cuántas cambió.
    public int marcarTodasComoLeidas(int idUsuario) {
        String sql = "UPDATE dbo.Notificacion SET leida = 1 WHERE id_usuario = ? AND leida = 0";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron marcar las notificaciones como leídas", e);
        }
    }

    // Convierte una fila de la base en un objeto Notificacion
    private Notificacion mapear(ResultSet rs) throws SQLException {
        Notificacion n = new Notificacion();
        n.setIdNotificacion(rs.getInt("id_notificacion"));
        n.setIdUsuario(rs.getInt("id_usuario"));
        n.setTipo(TipoNotificacion.valueOf(rs.getString("tipo")));
        n.setTitulo(rs.getString("titulo"));
        n.setMensaje(rs.getString("mensaje"));

        Timestamp fecha = rs.getTimestamp("fecha_hora");
        if (fecha != null) {
            n.setFechaHora(fecha.toLocalDateTime());
        }

        n.setLeida(rs.getBoolean("leida"));
        n.setEntidad(rs.getString("entidad"));

        // id_referencia puede ser nulo, hay que revisar antes de usar el valor
        int idReferencia = rs.getInt("id_referencia");
        if (!rs.wasNull()) {
            n.setIdReferencia(idReferencia);
        }
        return n;
    }
}
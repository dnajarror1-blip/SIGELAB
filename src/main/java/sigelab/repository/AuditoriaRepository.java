package sigelab.repository;

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
import sigelab.config.DatabaseConfig;
import sigelab.model.Auditoria;

public class AuditoriaRepository {

    public java.util.Optional<sigelab.model.Rol> buscarRolUsuarioActivo(Connection conn, int idUsuario) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT r.nombre FROM dbo.Usuario u JOIN dbo.Rol r ON r.id_rol = u.id_rol "
                + "WHERE u.id_usuario = ? AND u.estado = 'ACTIVO'")) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? java.util.Optional.of(new sigelab.model.Rol(rs.getString(1)))
                        : java.util.Optional.empty();
            }
        }
    }

    public void guardar(Auditoria auditoria) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            guardar(auditoria, conn);
        }
    }

    public void guardar(Auditoria auditoria, Connection conn) throws SQLException {
        String sql = "INSERT INTO dbo.Auditoria (id_usuario, fecha_hora, accion, entidad, id_registro, descripcion) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (auditoria.getIdUsuario() != null) {
                stmt.setInt(1, auditoria.getIdUsuario());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }

            LocalDateTime fechaHora = auditoria.getFechaHora() != null ? auditoria.getFechaHora() : LocalDateTime.now();
            stmt.setTimestamp(2, Timestamp.valueOf(fechaHora));
            stmt.setString(3, auditoria.getAccion());
            stmt.setString(4, auditoria.getEntidad());

            if (auditoria.getIdRegistro() != null) {
                stmt.setInt(5, auditoria.getIdRegistro());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setString(6, auditoria.getDescripcion());

            if (stmt.executeUpdate() != 1) throw new SQLException("No se registró el evento de auditoría.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    auditoria.setIdAuditoria(rs.getInt(1));
                }
            }
        }
    }

    public List<Auditoria> listarTodos() throws SQLException {
        String sql = "SELECT id_auditoria, id_usuario, fecha_hora, accion, entidad, id_registro, descripcion "
                   + "FROM dbo.Auditoria ORDER BY fecha_hora DESC";
        List<Auditoria> lista = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearAuditoria(rs));
            }
        }
        return lista;
    }

    public List<Auditoria> buscarPorFiltros(String accion, String entidad, Integer idUsuario) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id_auditoria, id_usuario, fecha_hora, accion, entidad, id_registro, descripcion FROM dbo.Auditoria WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (accion != null && !accion.isBlank()) {
            sql.append(" AND accion = ?");
            parametros.add(accion);
        }
        if (entidad != null && !entidad.isBlank()) {
            sql.append(" AND entidad = ?");
            parametros.add(entidad);
        }
        if (idUsuario != null) {
            sql.append(" AND id_usuario = ?");
            parametros.add(idUsuario);
        }

        sql.append(" ORDER BY fecha_hora DESC");

        List<Auditoria> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAuditoria(rs));
                }
            }
        }
        return lista;
    }

    private Auditoria mapearAuditoria(ResultSet rs) throws SQLException {
        Auditoria auditoria = new Auditoria();
        auditoria.setIdAuditoria(rs.getInt("id_auditoria"));

        int idUsuario = rs.getInt("id_usuario");
        if (!rs.wasNull()) {
            auditoria.setIdUsuario(idUsuario);
        }

        Timestamp timestamp = rs.getTimestamp("fecha_hora");
        if (timestamp != null) {
            auditoria.setFechaHora(timestamp.toLocalDateTime());
        }

        auditoria.setAccion(rs.getString("accion"));
        auditoria.setEntidad(rs.getString("entidad"));

        int idRegistro = rs.getInt("id_registro");
        if (!rs.wasNull()) {
            auditoria.setIdRegistro(idRegistro);
        }

        auditoria.setDescripcion(rs.getString("descripcion"));
        return auditoria;
    }
}

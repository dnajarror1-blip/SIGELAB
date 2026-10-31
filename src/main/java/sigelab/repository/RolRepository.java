package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.exception.DataAccessException;
import sigelab.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RolRepository {

    /** Consulta mínima del actor sin cargar credenciales y con la conexión de la operación. */
    public java.util.Optional<Rol> buscarDeUsuarioActivo(java.sql.Connection conn, int idUsuario)
            throws java.sql.SQLException {
        String sql = """
                SELECT r.id_rol, r.nombre
                FROM dbo.Usuario u JOIN dbo.Rol r ON r.id_rol = u.id_rol
                WHERE u.id_usuario = ? AND u.estado = 'ACTIVO'
                """;
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (var rs = stmt.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                Rol rol = new Rol(rs.getString("nombre"));
                rol.setIdRol(rs.getInt("id_rol"));
                return java.util.Optional.of(rol);
            }
        }
    }

    public Optional<Rol> buscarPorId(int idRol) {
        String sql = "SELECT id_rol, nombre FROM dbo.Rol WHERE id_rol = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idRol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el rol", e);
        }
    }

    public List<Rol> listar() {
        String sql = "SELECT id_rol, nombre FROM dbo.Rol ORDER BY nombre";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Rol> roles = new ArrayList<>();
            while (rs.next()) {
                roles.add(mapear(rs));
            }
            return roles;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo listar los roles", e);
        }
    }

    private Rol mapear(ResultSet rs) throws SQLException {
        Rol rol = new Rol(rs.getString("nombre"));
        rol.setIdRol(rs.getInt("id_rol"));
        return rol;
    }
}

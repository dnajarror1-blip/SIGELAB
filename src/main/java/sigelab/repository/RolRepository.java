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

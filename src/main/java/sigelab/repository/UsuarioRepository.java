package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioRepository {

    private static final String SQL_BUSCAR_POR_USERNAME =
            "SELECT id_usuario, nombre, apellido, username, password_hash, "
                    + "correo, id_rol, estado FROM dbo.Usuario WHERE username = ?";

    // TEMPORAL: lanza SQLException hasta acordar DataAccessException con el #1
    public Optional<Usuario> buscarPorUsername(String username) throws SQLException {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_BUSCAR_POR_USERNAME)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setCorreo(rs.getString("correo"));
        u.setIdRol(rs.getInt("id_rol"));
        u.setEstado(EstadoUsuario.valueOf(rs.getString("estado")));
        return u;
    }
}
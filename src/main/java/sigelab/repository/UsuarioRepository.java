package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.DataAccessException;
import sigelab.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UsuarioRepository {

    private static final String SQL_BUSCAR_POR_USERNAME =
            "SELECT id_usuario, nombre, apellido, username, password_hash, "
                    + "correo, id_rol, estado FROM dbo.Usuario WHERE username = ?";

    private static final String SQL_INSERTAR =
            "INSERT INTO dbo.Usuario (nombre, apellido, username, password_hash, "
                    + "correo, id_rol, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";

    // Busca un usuario por su username; si no existe devuelve vacío
    public Optional<Usuario> buscarPorUsername(String username) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_BUSCAR_POR_USERNAME)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            // Convertimos el error de SQL en la excepción del equipo
            // y guardamos el error original (e) para poder investigarlo
            throw new DataAccessException("No se pudo buscar el usuario", e);
        }
    }

    // Guarda un usuario nuevo y le pone el id que generó SQL Server
    public Usuario guardar(Usuario usuario) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            // Llenamos los 7 ? en el mismo orden del INSERT
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setString(3, usuario.getUsername());
            ps.setString(4, usuario.getPasswordHash());
            ps.setString(5, usuario.getCorreo());
            ps.setInt(6, usuario.getIdRol());
            ps.setString(7, usuario.getEstado().name());

            ps.executeUpdate();

            // Leemos el id que se generó y se lo ponemos al usuario
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    usuario.setIdUsuario(keys.getInt(1));
                }
            }
            return usuario;

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el usuario", e);
        }
    }

    // Convierte una fila de la base en un objeto Usuario
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
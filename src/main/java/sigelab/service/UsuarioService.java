package sigelab.service;

import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Usuario;
import sigelab.repository.UsuarioRepository;

import java.sql.SQLException;

public class UsuarioService {

    // Con este objeto hablamos con la base de datos
    private final UsuarioRepository usuarioRepository = new UsuarioRepository();

    // TEMPORAL: cuando el #1 suba las excepciones del equipo, las cambiamos aquí
    public Usuario crearUsuario(String nombre, String apellido, String username,
                                String passwordPlano, String correo, int idRol)
            throws SQLException {

        // Primero revisamos que los datos tengan sentido
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }
        if (passwordPlano == null || passwordPlano.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }

        // Si ya hay alguien con ese username, no seguimos
        if (usuarioRepository.buscarPorUsername(username).isPresent()) {
            throw new IllegalArgumentException("Ese username ya existe");
        }

        // Convertimos la contraseña en un hash, así nunca se guarda tal cual
        String hash = BCrypt.hashpw(passwordPlano, BCrypt.gensalt());

        // Armamos el usuario con todos sus datos
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setCorreo(correo);
        u.setIdRol(idRol);
        u.setEstado(EstadoUsuario.ACTIVO); // todo usuario nuevo empieza activo

        // Lo guardamos y devolvemos el usuario ya con su id
        return usuarioRepository.guardar(u);
    }
}
package sigelab.controller;

import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.service.PermisoService;
import sigelab.service.UsuarioService;

import java.sql.SQLException;
import java.util.Set;

public class LoginController {

    private final UsuarioService usuarioService = new UsuarioService();
    private final PermisoService permisoService = new PermisoService();

    // Aquí guardamos lo que la pantalla necesita saber después de intentar entrar.
    // Un "record" es una clase corta que solo guarda datos.
    public record ResultadoLogin(boolean exito, String mensaje,
                                 Usuario usuario, Set<Modulo> menu) { }

    // La pantalla llama a este método cuando el usuario presiona "Entrar"
    public ResultadoLogin iniciarSesion(String username, String password) {
        try {
            // Revisamos usuario, contraseña y que esté activo
            Usuario u = usuarioService.autenticar(username, password);

            // Según su rol, sacamos las opciones del menú que le tocan
            Set<Modulo> menu = permisoService.modulosDe(u.getIdRol());

            return new ResultadoLogin(true, "Bienvenido " + u.getNombre(), u, menu);

        } catch (IllegalArgumentException e) {
            // Error de reglas (contraseña mala, usuario inactivo...): se lo mostramos tal cual
            return new ResultadoLogin(false, e.getMessage(), null, Set.of());

        } catch (SQLException e) {
            // Error de base de datos: al usuario no le mostramos detalles técnicos
            return new ResultadoLogin(false, "No se pudo conectar con la base de datos", null, Set.of());
        }
    }
}
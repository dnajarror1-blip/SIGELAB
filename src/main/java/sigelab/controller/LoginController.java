package sigelab.controller;

import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.exception.DatabaseConfigurationException;
import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.service.AutenticacionService;
import sigelab.service.PermisoService;
import sigelab.service.UsuarioService;

import java.util.Set;

public class LoginController {

    private final AutenticacionService autenticacion;

    public LoginController() {
        this(new UsuarioService(), new PermisoService());
    }

    public LoginController(UsuarioService usuarios, PermisoService permisos) {
        this(new AutenticacionService(usuarios, permisos));
    }

    public LoginController(AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
    }

    // Aquí guardamos lo que la pantalla necesita saber después de intentar entrar.
    // Un "record" es una clase corta que solo guarda datos.
    public record ResultadoLogin(boolean exito, String mensaje,
                                 Usuario usuario, Set<Modulo> menu, String nombreRol) { }

    // La pantalla llama a este método cuando el usuario presiona "Entrar"
    public ResultadoLogin iniciarSesion(String username, String password) {
        try {
            var sesion = autenticacion.autenticar(username, password);
            return new ResultadoLogin(true, "Bienvenido " + sesion.usuario().getNombre(),
                    sesion.usuario(), sesion.menu(), sesion.nombreRol());

        } catch (BusinessException e) {
            // Error de reglas (contraseña mala, usuario inactivo...): se lo mostramos tal cual
            return new ResultadoLogin(false, e.getMessage(), null, Set.of(), null);

        } catch (DatabaseConfigurationException e) {
            return new ResultadoLogin(false,
                    "La conexión a la base de datos no está configurada. Contacta al administrador.",
                    null, Set.of(), null);

        } catch (DataAccessException e) {
            // Error de base de datos: al usuario no le mostramos detalles técnicos
            return new ResultadoLogin(false, "No se pudo conectar con la base de datos", null, Set.of(), null);
        }
    }
}

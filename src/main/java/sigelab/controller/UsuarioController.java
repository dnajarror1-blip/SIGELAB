package sigelab.controller;

import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.service.PermisoService;
import sigelab.service.UsuarioService;

import java.util.List;

public class UsuarioController {

    private final UsuarioService usuarioService = new UsuarioService();
    private final PermisoService permisoService = new PermisoService();

    // El usuario que tiene la sesión abierta (quien está usando la pantalla)
    private final Usuario usuarioActual;

    // Respuesta que entiende la pantalla. El <T> significa "el dato puede ser de cualquier tipo":
    // una lista de usuarios, un usuario, o nada.
    public record Respuesta<T>(boolean exito, String mensaje, T dato) { }

    public UsuarioController(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    // Trae todos los usuarios para la tabla
    public Respuesta<List<Usuario>> listar() {
        try {
            verificarPermiso();
            return new Respuesta<>(true, "OK", usuarioService.listarUsuarios());
        } catch (BusinessException e) {
            return new Respuesta<>(false, e.getMessage(), List.of());
        } catch (DataAccessException e) {
            return new Respuesta<>(false, "No se pudo conectar con la base de datos", List.of());
        }
    }

    // Crea un usuario nuevo con los datos del formulario
    public Respuesta<Usuario> crear(String nombre, String apellido, String username,
                                    String password, String correo, int idRol) {
        try {
            verificarPermiso();
            Usuario nuevo = usuarioService.crearUsuario(nombre, apellido, username,
                    password, correo, idRol);
            return new Respuesta<>(true, "Usuario creado", nuevo);
        } catch (BusinessException e) {
            return new Respuesta<>(false, e.getMessage(), null);
        } catch (DataAccessException e) {
            return new Respuesta<>(false, "No se pudo conectar con la base de datos", null);
        }
    }

    // Activa o desactiva a un usuario
    public Respuesta<Void> cambiarEstado(int idUsuario, EstadoUsuario nuevoEstado) {
        try {
            verificarPermiso();
            usuarioService.cambiarEstado(idUsuario, nuevoEstado, usuarioActual.getIdUsuario());
            return new Respuesta<>(true, "Estado actualizado", null);
        } catch (BusinessException e) {
            return new Respuesta<>(false, e.getMessage(), null);
        } catch (DataAccessException e) {
            return new Respuesta<>(false, "No se pudo conectar con la base de datos", null);
        }
    }

    // Aunque la pantalla esconda el botón, aquí también revisamos el permiso.
    // Así nadie puede saltarse el menú y llamar a estos métodos directamente.
    private void verificarPermiso() {
        if (!permisoService.puedeAcceder(usuarioActual.getIdRol(), Modulo.USUARIOS)) {
            throw new BusinessException("No tienes permiso para administrar usuarios");
        }
    }
}
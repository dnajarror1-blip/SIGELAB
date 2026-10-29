package sigelab.service;

import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Usuario;

public class FacialAdminService {

    private final UsuarioService usuarioService;
    private final PermisoService permisoService;

    // Constructor normal, el que usa la app
    public FacialAdminService() {
        this(new UsuarioService(), new PermisoService());
    }

    // Este otro nos sirve para pruebas
    public FacialAdminService(UsuarioService usuarioService, PermisoService permisoService) {
        this.usuarioService = usuarioService;
        this.permisoService = permisoService;
    }

    // Confirma que quien pide la acción facial es un administrador activo.
    // Devuelve el usuario tal como está ahora en la base de datos.
    public Usuario validarAdministrador(Usuario quienPide) {
        if (quienPide == null || quienPide.getUsername() == null) {
            throw new BusinessException("No hay una sesión iniciada");
        }

        // Lo volvemos a buscar en la base, porque el objeto de la sesión pudo quedar viejo
        Usuario actual = usuarioService.buscarPorUsername(quienPide.getUsername())
                .orElseThrow(() -> new BusinessException("El usuario de la sesión ya no existe"));

        // Verificamos que sea la misma persona y no solo el mismo nombre
        if (actual.getIdUsuario() == null || !actual.getIdUsuario().equals(quienPide.getIdUsuario())) {
            throw new BusinessException("La sesión no coincide con el usuario registrado");
        }

        // Si lo desactivaron mientras estaba conectado, ya no puede seguir
        if (actual.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException("El usuario no está activo");
        }

        // Solo los roles con permiso sobre enrollment facial (ADMIN) pasan
        if (!permisoService.puedeAcceder(actual.getIdRol(), Modulo.ENROLLMENT_FACIAL)) {
            throw new BusinessException("No tienes permiso para administrar rostros");
        }

        return actual;
    }
}
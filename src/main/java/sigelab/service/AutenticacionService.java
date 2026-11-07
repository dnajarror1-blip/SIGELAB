package sigelab.service;

import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.exception.DatabaseConfigurationException;
import sigelab.model.Rol;
import sigelab.model.Usuario;

import java.util.Set;

/** Login completo; validar una contraseña por sí sola no crea una sesión. */
public class AutenticacionService {
    @FunctionalInterface
    public interface VerificadorFacial { boolean verificar(int idUsuario); }

    private final UsuarioService usuarios;
    private final PermisoService permisos;
    private final VerificadorFacial facial;
    private final AuditService auditoria;

    public AutenticacionService(UsuarioService usuarios, PermisoService permisos) {
        this(usuarios, permisos, id -> {
            try {
                var cliente = FacialAuthClient.desdeEntorno();
                var perfil = cliente.consultarPerfil(id);
                if (!perfil.registrado() || !perfil.activo()) {
                    throw new BusinessException("Debe registrar un rostro activo con un administrador. Acceso denegado.");
                }
                return cliente.verificar(id).matched();
            } catch (FacialAuthClient.ErrorComunicacion | IllegalArgumentException error) {
                throw new BusinessException("La verificación facial no está disponible. Acceso denegado.");
            }
        }, new AuditService());
    }

    public AutenticacionService(UsuarioService usuarios, PermisoService permisos,
                                VerificadorFacial facial, AuditService auditoria) {
        this.usuarios = usuarios;
        this.permisos = permisos;
        this.facial = facial;
        this.auditoria = auditoria;
    }

    public record Sesion(Usuario usuario, Set<Modulo> menu, String nombreRol) { }

    public Sesion autenticar(String username, String password) {
        return autenticar(username, password, () -> { });
    }

    /** Notificación de presentación; las validaciones y su orden permanecen iguales. */
    public Sesion autenticar(String username, String password, Runnable antesDeVerificarRostro) {
        // UsuarioService ya registra los fallos de credenciales; no duplicarlos aquí.
        Usuario usuario = usuarios.autenticar(username, password);
        Rol rol;
        Usuario actual;
        Set<Modulo> menu;
        try {
            if (usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getIdUsuario() == null
                    || usuario.getIdUsuario() <= 0) {
                throw new BusinessException("El usuario no está activo");
            }
            resolverRol(usuario);
            antesDeVerificarRostro.run();
            if (!facial.verificar(usuario.getIdUsuario())) {
                throw new BusinessException("La verificación facial no fue satisfactoria. Acceso denegado.");
            }
            actual = usuarios.revalidarUsuarioActivo(usuario.getIdUsuario());
            rol = resolverRol(actual);
            menu = permisos.modulosDe(rol);
        } catch (BusinessException | DataAccessException | DatabaseConfigurationException error) {
            auditoria.registrarObligatorio(usuario.getIdUsuario(), "LOGIN_FALLIDO", "Usuario", usuario.getIdUsuario(),
                    "Acceso denegado después de validar credenciales.");
            throw error;
        } catch (FacialAuthClient.ErrorComunicacion error) {
            auditoria.registrarObligatorio(usuario.getIdUsuario(), "LOGIN_FALLIDO", "Usuario", usuario.getIdUsuario(),
                    "Servicio facial no disponible o respuesta inválida.");
            throw new BusinessException("La verificación facial no está disponible. Acceso denegado.");
        }
        // Fuera del catch: un fallo de auditoría exitosa no genera un segundo evento.
        auditoria.registrarObligatorio(actual.getIdUsuario(), "LOGIN_EXITOSO", "Usuario", actual.getIdUsuario(),
                "Contraseña y verificación facial satisfactorias.");
        return new Sesion(actual, menu, rol.getNombre());
    }

    private Rol resolverRol(Usuario usuario) {
        return usuarios.buscarRolPorId(usuario.getIdRol())
                .filter(rol -> rol.getNombre() != null && !rol.getNombre().isBlank())
                .orElseThrow(() -> new BusinessException("El usuario no tiene un rol válido"));
    }
}

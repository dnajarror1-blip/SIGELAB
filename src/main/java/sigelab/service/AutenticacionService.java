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
                return FacialAuthClient.desdeEntorno().verificar(id).matched();
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
        // UsuarioService ya registra los fallos de credenciales; no duplicarlos aquí.
        Usuario usuario = usuarios.autenticar(username, password);
        try {
            if (usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getIdUsuario() == null
                    || usuario.getIdUsuario() <= 0) {
                throw new BusinessException("El usuario no está activo");
            }
            resolverRol(usuario);
            if (!facial.verificar(usuario.getIdUsuario())) {
                throw new BusinessException("La verificación facial no fue satisfactoria. Acceso denegado.");
            }
            Usuario actual = usuarios.revalidarUsuarioActivo(usuario.getIdUsuario());
            Rol rol = resolverRol(actual);
            Set<Modulo> menu = permisos.modulosDe(rol);
            auditoria.registrar(actual.getIdUsuario(), "LOGIN_EXITOSO", "Usuario", actual.getIdUsuario(),
                    "Contraseña y verificación facial satisfactorias.");
            return new Sesion(actual, menu, rol.getNombre());
        } catch (BusinessException | DataAccessException | DatabaseConfigurationException error) {
            auditoria.registrar(usuario.getIdUsuario(), "LOGIN_FALLIDO", "Usuario", usuario.getIdUsuario(),
                    "Acceso denegado después de validar credenciales.");
            throw error;
        } catch (FacialAuthClient.ErrorComunicacion error) {
            auditoria.registrar(usuario.getIdUsuario(), "LOGIN_FALLIDO", "Usuario", usuario.getIdUsuario(),
                    "Servicio facial no disponible o respuesta inválida.");
            throw new BusinessException("La verificación facial no está disponible. Acceso denegado.");
        }
    }

    private Rol resolverRol(Usuario usuario) {
        return usuarios.buscarRolPorId(usuario.getIdRol())
                .filter(rol -> rol.getNombre() != null && !rol.getNombre().isBlank())
                .orElseThrow(() -> new BusinessException("El usuario no tiene un rol válido"));
    }
}

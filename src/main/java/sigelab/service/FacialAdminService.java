package sigelab.service;

import sigelab.exception.BusinessException;
import sigelab.model.Usuario;

import java.util.Objects;
import java.util.function.Supplier;

/** Administración facial. Revalida al actor en SQL antes de cada operación. */
public class FacialAdminService {
    private final UsuarioService usuarios;
    private final OperacionesFaciales facial;

    public FacialAdminService() {
        this(new UsuarioService(), new ClienteActual(FacialAuthClient::desdeEntorno));
    }

    public FacialAdminService(UsuarioService usuarios, FacialAuthClient cliente) {
        this(usuarios, new ClienteActual(() -> Objects.requireNonNull(cliente)));
    }

    // Frontera sustituible por un doble en memoria, sin modificar el cliente estabilizado.
    interface OperacionesFaciales {
        FacialAuthClient.EstadoPerfil consultar(int targetUserId);
        FacialAuthClient.Registro registrar(int targetUserId, int adminUserId, boolean replace);
        FacialAuthClient.Eliminacion eliminar(int targetUserId, int adminUserId);
    }

    FacialAdminService(UsuarioService usuarios, OperacionesFaciales facial) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.facial = Objects.requireNonNull(facial);
    }

    public FacialAuthClient.EstadoPerfil consultarEstado(Usuario actor, int targetUserId) {
        autorizar(actor, targetUserId);
        return consultarObjetivo(targetUserId);
    }

    public FacialAuthClient.Registro registrarRostro(Usuario actor, int targetUserId) {
        int adminUserId = autorizar(actor, targetUserId);
        var estado = consultarObjetivo(targetUserId);
        if (estado.registrado()) {
            throw new BusinessException("El usuario ya tiene un perfil facial. Utiliza Reemplazar rostro.");
        }
        if (!estado.usuarioActivo()) {
            throw new BusinessException("El usuario objetivo debe estar activo para registrar su rostro.");
        }
        return comprobarRegistro(facial.registrar(targetUserId, adminUserId, false));
    }

    /** Invocación explícita; la confirmación visual corresponde a la View. */
    public FacialAuthClient.Registro reemplazarRostro(Usuario actor, int targetUserId) {
        int adminUserId = autorizar(actor, targetUserId);
        return comprobarRegistro(facial.registrar(targetUserId, adminUserId, true));
    }

    public FacialAuthClient.Eliminacion eliminarRostro(Usuario actor, int targetUserId) {
        int adminUserId = autorizar(actor, targetUserId);
        var resultado = facial.eliminar(targetUserId, adminUserId);
        if (!resultado.deleted()) {
            throw new BusinessException("No se confirmó la eliminación del perfil facial.");
        }
        return resultado;
    }

    private int autorizar(Usuario actor, int targetUserId) {
        if (actor == null || actor.getIdUsuario() == null || actor.getIdUsuario() <= 0) {
            throw new BusinessException("Debes iniciar sesión con un administrador activo.");
        }
        if (targetUserId <= 0) {
            throw new BusinessException("Selecciona un usuario objetivo válido.");
        }
        int adminUserId = actor.getIdUsuario();
        usuarios.exigirAdministradorActivo(adminUserId);
        return adminUserId;
    }

    private FacialAuthClient.EstadoPerfil consultarObjetivo(int targetUserId) {
        var estado = facial.consultar(targetUserId);
        if (!estado.usuarioExiste()) {
            throw new BusinessException("El usuario objetivo ya no existe.");
        }
        return estado;
    }

    private FacialAuthClient.Registro comprobarRegistro(FacialAuthClient.Registro resultado) {
        if (!resultado.registered()) {
            throw new BusinessException("No se confirmó el registro del perfil facial.");
        }
        return resultado;
    }

    private record ClienteActual(Supplier<FacialAuthClient> proveedor) implements OperacionesFaciales {
        private FacialAuthClient cliente() {
            try {
                return proveedor.get();
            } catch (IllegalArgumentException error) {
                throw new BusinessException("La configuración del servicio facial no es válida. Contacta al administrador.");
            }
        }
        public FacialAuthClient.EstadoPerfil consultar(int id) { return cliente().consultarPerfil(id); }
        public FacialAuthClient.Registro registrar(int id, int admin, boolean replace) {
            return cliente().registrar(id, admin, replace);
        }
        public FacialAuthClient.Eliminacion eliminar(int id, int admin) { return cliente().eliminar(id, admin); }
    }
}

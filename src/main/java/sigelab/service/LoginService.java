package sigelab.service;

import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.model.Usuario;

public class LoginService {

    // Los textos de acción, tal como los pide el documento del proyecto
    private static final String LOGIN_EXITOSO = "LOGIN_EXITOSO";
    private static final String LOGIN_FALLIDO = "LOGIN_FALLIDO";
    private static final String ENTIDAD = "Usuario";

    private final UsuarioService usuarioService;
    private final AuditService auditService;

    public LoginService() {
        this(new UsuarioService(), new AuditService());
    }

    // Este segundo constructor permite cambiar las piezas en las pruebas
    public LoginService(UsuarioService usuarioService, AuditService auditService) {
        this.usuarioService = usuarioService;
        this.auditService = auditService;
    }

    // Valida al usuario, deja constancia en auditoría y devuelve al usuario si entró.
    // Si falla, vuelve a lanzar el mismo error para que la pantalla lo muestre.
    public Usuario iniciarSesion(String username, String password) {
        try {
            // Revisa existencia, contraseña con BCrypt y que esté ACTIVO
            Usuario usuario = usuarioService.autenticar(username, password);

            // AQUÍ, más adelante, va la verificación facial (FacialAuthClient.verify)

            // Todo bien: dejamos constancia del inicio de sesión
            auditService.registrar(usuario.getIdUsuario(), LOGIN_EXITOSO, ENTIDAD,
                    usuario.getIdUsuario(),
                    "Inicio de sesión correcto de " + usuario.getUsername());
            return usuario;

        } catch (BusinessException e) {
            // Falló: guardamos el intento. Si el usuario existe, anotamos su id.
            // La contraseña nunca se guarda en el texto.
            Integer idUsuario = buscarId(username);
            auditService.registrar(idUsuario, LOGIN_FALLIDO, ENTIDAD, idUsuario,
                    recortar("Intento fallido de " + username + ": " + e.getMessage()));
            throw e;
        }
    }

    // Devuelve el id del usuario si existe, o null si no existe o no se pudo consultar.
    // La auditoría no debe romper el login, por eso no dejamos que este error salga.
    private Integer buscarId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return usuarioService.buscarPorUsername(username)
                    .map(Usuario::getIdUsuario)
                    .orElse(null);
        } catch (DataAccessException e) {
            return null;
        }
    }

    // La columna descripcion admite máximo 500 caracteres
    private String recortar(String texto) {
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
    }
}
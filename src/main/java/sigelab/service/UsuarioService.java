package sigelab.service;

import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.repository.RolRepository;
import sigelab.repository.UsuarioRepository;

import java.sql.Connection;
import java.sql.SQLException;
import sigelab.config.DatabaseConfig;
import sigelab.exception.DataAccessException;
import java.util.List;
import java.util.Optional;

public class UsuarioService {

    // Con este objeto hablamos con la base de datos
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    private final AuditService auditoria;
    private final ConexionUsuario conexiones;

    @FunctionalInterface
    interface ConexionUsuario { Connection abrir() throws SQLException; }

    public UsuarioService() {
        this(new UsuarioRepository(), new RolRepository());
    }

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles) {
        this(usuarios, roles, new AuditService(), DatabaseConfig::getConnection);
    }

    UsuarioService(UsuarioRepository usuarios, RolRepository roles, AuditService auditoria, ConexionUsuario conexiones) {
        this.auditoria = auditoria;
        this.conexiones = conexiones;
        this.usuarioRepository = usuarios;
        this.rolRepository = roles;
    }

    public List<Rol> listarRoles() {
        return rolRepository.listar();
    }

    public Optional<Rol> buscarRolPorId(int idRol) {
        return rolRepository.buscarPorId(idRol);
    }

    public Usuario crearUsuario(String nombre, String apellido, String username,
                                String passwordPlano, String correo, int idRol) {

        // Primero revisamos que los datos tengan sentido
        nombre = validarTexto(nombre, "El nombre", 60);
        apellido = validarTexto(apellido, "El apellido", 60);
        username = validarTexto(username, "El username", 50);
        correo = validarTexto(correo, "El correo", 120);
        if (!correo.matches("[^\\s@]+@[^\\s@]+")) {
            throw new BusinessException("El correo no tiene un formato válido");
        }
        if (passwordPlano == null || passwordPlano.isBlank() || passwordPlano.length() < 6) {
            throw new BusinessException("La contraseña debe tener al menos 6 caracteres");
        }
        if (idRol <= 0) {
            throw new BusinessException("Debe seleccionar un rol válido");
        }
        if (rolRepository.buscarPorId(idRol).isEmpty()) {
            throw new BusinessException("El rol seleccionado no existe");
        }

        // Si ya hay alguien con ese username, no seguimos
        if (usuarioRepository.buscarPorUsername(username).isPresent()) {
            throw new BusinessException("Ese username ya existe");
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

    // Revisa usuario y contraseña. Si todo está bien devuelve el usuario.
    public Usuario autenticar(String username, String passwordPlano) {

        // Si no escribieron nada, ni vale la pena ir a la base
        if (username == null || username.isBlank() || passwordPlano == null) {
            auditoria.registrar(null, "LOGIN_FALLIDO", "Usuario", null, "Credenciales rechazadas.");
            throw new BusinessException("Usuario o contraseña incorrectos");
        }

        // Buscamos al usuario por su username
        Optional<Usuario> encontrado = usuarioRepository.buscarPorUsername(username.trim());

        // Si no existe, damos el mismo mensaje que con contraseña mala,
        // así nadie puede adivinar qué usernames existen
        if (encontrado.isEmpty()) {
            auditoria.registrar(null, "LOGIN_FALLIDO", "Usuario", null, "Credenciales rechazadas.");
            throw new BusinessException("Usuario o contraseña incorrectos");
        }
        Usuario u = encontrado.get();

        // Comparamos lo que escribió con el hash guardado
        boolean passwordValido;
        try {
            passwordValido = BCrypt.checkpw(passwordPlano, u.getPasswordHash());
        } catch (IllegalArgumentException | NullPointerException error) {
            passwordValido = false;
        }
        if (!passwordValido) {
            auditoria.registrar(null, "LOGIN_FALLIDO", "Usuario", null, "Credenciales rechazadas.");
            throw new BusinessException("Usuario o contraseña incorrectos");
        }

        // Aunque la contraseña esté bien, un usuario que no está activo no entra
        if (u.getEstado() != EstadoUsuario.ACTIVO) {
            auditoria.registrar(u.getIdUsuario(), "LOGIN_FALLIDO", "Usuario", u.getIdUsuario(), "Acceso rechazado: usuario inactivo.");
            throw new BusinessException("El usuario no está activo");
        }

        // El éxito pertenece al flujo completo de AutenticacionService, después de FaceID.

        // Todo bien, lo dejamos pasar
        return u;
    }

    /** Reconsulta el estado después de la captura, usando el identificador estable. */
    public Usuario revalidarUsuarioActivo(int idUsuario) {
        Usuario actual = usuarioRepository.buscarPorId(idUsuario)
                .orElseThrow(() -> new BusinessException("El usuario ya no está disponible"));
        if (actual.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException("El usuario no está activo");
        }
        return actual;
    }

    public void exigirAdministradorActivo(int idUsuario) {
        Usuario actor = revalidarUsuarioActivo(idUsuario);
        Rol rol = buscarRolPorId(actor.getIdRol())
                .orElseThrow(() -> new BusinessException("El usuario no tiene un rol válido"));
        if (!"ADMIN".equals(rol.getNombre())) {
            throw new BusinessException("Se requiere un administrador activo");
        }
    }

    // Devuelve todos los usuarios para mostrarlos en la tabla
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.listarTodos();
    }

    // Activa o desactiva a un usuario.
    // idQuienLoHace es el usuario que tiene la sesión abierta
    public void cambiarEstado(int idUsuario, EstadoUsuario nuevoEstado, int idQuienLoHace) {

        // Regla: nadie puede desactivarse a sí mismo, porque se quedaría sin acceso
        if (idUsuario == idQuienLoHace && nuevoEstado == EstadoUsuario.INACTIVO) {
            throw new BusinessException("No puedes desactivar tu propio usuario");
        }

        if (idUsuario <= 0 || idQuienLoHace <= 0 || nuevoEstado == null) {
            throw new BusinessException("Debe indicar usuario, actor y estado válidos.");
        }
        try (Connection conn = conexiones.abrir()) {
            conn.setAutoCommit(false);
            try {
                auditoria.validarAcceso(conn, idQuienLoHace, Modulo.USUARIOS);
                if (!usuarioRepository.cambiarEstado(idUsuario, nuevoEstado, conn)) {
                    throw new BusinessException("No se encontró el usuario");
                }
                auditoria.registrar(idQuienLoHace,
                        nuevoEstado == EstadoUsuario.INACTIVO ? "USUARIO_DESACTIVADO" : "USUARIO_ACTIVADO",
                        "Usuario", idUsuario, "Cambio de estado a " + nuevoEstado, conn);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof BusinessException negocio) throw negocio;
                throw new DataAccessException("No se pudo actualizar el estado del usuario.", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar para actualizar el usuario.", e);
        }
    }

    private String validarTexto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(campo + " es obligatorio");
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new BusinessException(campo + " no puede superar los " + maximo + " caracteres");
        }
        return limpio;
    }
}

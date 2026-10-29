package sigelab.service;

import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.repository.RolRepository;
import sigelab.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

public class UsuarioService {

    // Con este objeto hablamos con la base de datos
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService() {
        this(new UsuarioRepository(), new RolRepository());
    }

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles) {
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
            throw new BusinessException("Usuario o contraseña incorrectos");
        }

        // Buscamos al usuario por su username
        Optional<Usuario> encontrado = usuarioRepository.buscarPorUsername(username.trim());

        // Si no existe, damos el mismo mensaje que con contraseña mala,
        // así nadie puede adivinar qué usernames existen
        if (encontrado.isEmpty()) {
            throw new BusinessException("Usuario o contraseña incorrectos");
        }
        Usuario u = encontrado.get();

        // Comparamos lo que escribió con el hash guardado
        if (!BCrypt.checkpw(passwordPlano, u.getPasswordHash())) {
            throw new BusinessException("Usuario o contraseña incorrectos");
        }

        // Aunque la contraseña esté bien, un usuario que no está activo no entra
        if (u.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException("El usuario no está activo");
        }

        // Todo bien, lo dejamos pasar
        return u;
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

        // Si el repositorio no encontró ese id, avisamos
        boolean cambiado = usuarioRepository.cambiarEstado(idUsuario, nuevoEstado);
        if (!cambiado) {
            throw new BusinessException("No se encontró el usuario");
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

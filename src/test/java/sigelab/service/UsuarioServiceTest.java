package sigelab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.BusinessException;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.repository.RolRepository;
import sigelab.repository.UsuarioRepository;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioServiceTest {

    @ParameterizedTest
    @MethodSource("datosInvalidos")
    void rechazaDatosInvalidosAntesDeAccederALaBase(String nombre, String apellido,
                                                   String username, String password,
                                                   String correo, int idRol) {
        UsuarioService servicio = new UsuarioService(new UsuarioRepository() {
            @Override
            public Optional<Usuario> buscarPorUsername(String username) {
                throw new AssertionError("No debe consultar usuarios con datos inválidos");
            }

            @Override
            public Usuario guardar(Usuario usuario) {
                throw new AssertionError("No debe guardar datos inválidos");
            }
        }, new RolRepository() {
            @Override
            public Optional<Rol> buscarPorId(int idRol) {
                throw new AssertionError("No debe consultar roles con datos inválidos");
            }
        });

        assertThrows(BusinessException.class,
                () -> servicio.crearUsuario(nombre, apellido, username, password, correo, idRol));
    }

    static Stream<Arguments> datosInvalidos() {
        return Stream.of(
                Arguments.of(null, "Apellido", "usuario", "clave123", "u@correo.test", 42),
                Arguments.of(" ", "Apellido", "usuario", "clave123", "u@correo.test", 42),
                Arguments.of("n".repeat(61), "Apellido", "usuario", "clave123", "u@correo.test", 42),
                Arguments.of("Nombre", null, "usuario", "clave123", "u@correo.test", 42),
                Arguments.of("Nombre", " ", "usuario", "clave123", "u@correo.test", 42),
                Arguments.of("Nombre", "a".repeat(61), "usuario", "clave123", "u@correo.test", 42),
                Arguments.of("Nombre", "Apellido", null, "clave123", "u@correo.test", 42),
                Arguments.of("Nombre", "Apellido", " ", "clave123", "u@correo.test", 42),
                Arguments.of("Nombre", "Apellido", "u".repeat(51), "clave123", "u@correo.test", 42),
                Arguments.of("Nombre", "Apellido", "usuario", "clave123", null, 42),
                Arguments.of("Nombre", "Apellido", "usuario", "clave123", " ", 42),
                Arguments.of("Nombre", "Apellido", "usuario", "clave123", "sin-correo", 42),
                Arguments.of("Nombre", "Apellido", "usuario", "clave123", "c".repeat(121), 42),
                Arguments.of("Nombre", "Apellido", "usuario", null, "u@correo.test", 42),
                Arguments.of("Nombre", "Apellido", "usuario", "12345", "u@correo.test", 42),
                Arguments.of("Nombre", "Apellido", "usuario", "      ", "u@correo.test", 42),
                Arguments.of("Nombre", "Apellido", "usuario", "clave123", "u@correo.test", 0)
        );
    }

    @Test
    void aceptaLongitudesMaximasYConservaElHash() {
        UsuarioService servicio = servicioEnMemoria();
        Usuario usuario = servicio.crearUsuario("n".repeat(60), "a".repeat(60),
                "u".repeat(50), "clave123", "c".repeat(108) + "@correo.test", 42);

        assertEquals(60, usuario.getNombre().length());
        assertEquals(60, usuario.getApellido().length());
        assertEquals(50, usuario.getUsername().length());
        assertEquals(120, usuario.getCorreo().length());
        assertEquals(42, usuario.getIdRol());
        assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
        assertTrue(BCrypt.checkpw("clave123", usuario.getPasswordHash()));
    }

    @Test
    void normalizaCamposSinRecortarLaContrasena() {
        Usuario usuario = servicioEnMemoria().crearUsuario(" Ana ", " Prueba ",
                " ana ", " clave123 ", " ana@correo.test ", 42);

        assertEquals("Ana", usuario.getNombre());
        assertEquals("Prueba", usuario.getApellido());
        assertEquals("ana", usuario.getUsername());
        assertEquals("ana@correo.test", usuario.getCorreo());
        assertTrue(BCrypt.checkpw(" clave123 ", usuario.getPasswordHash()));
        assertFalse(BCrypt.checkpw("clave123", usuario.getPasswordHash()));
    }

    @Test
    void rechazaUnRolQueNoExiste() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicioEnMemoria().crearUsuario("Ana", "Prueba", "ana",
                        "clave123", "ana@correo.test", 99));
        assertEquals("El rol seleccionado no existe", error.getMessage());
    }

    private UsuarioService servicioEnMemoria() {
        return new UsuarioService(new UsuarioRepository() {
            @Override
            public Optional<Usuario> buscarPorUsername(String username) {
                return Optional.empty();
            }

            @Override
            public Usuario guardar(Usuario usuario) {
                usuario.setIdUsuario(100);
                return usuario;
            }
        }, new RolRepository() {
            @Override
            public Optional<Rol> buscarPorId(int idRol) {
                if (idRol != 42) {
                    return Optional.empty();
                }
                Rol rol = new Rol("ADMIN");
                rol.setIdRol(idRol);
                return Optional.of(rol);
            }
        });
    }
}

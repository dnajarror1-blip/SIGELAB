package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.exception.DataAccessException;
import sigelab.exception.DatabaseConfigurationException;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.service.PermisoService;
import sigelab.service.UsuarioService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LoginControllerTest {

    @Test
    void configuracionIncompletaDeniegaAccesoConMensajeSeguro() {
        LoginController controller = controladorQueFalla(
                new DatabaseConfigurationException("Detalle privado de configuración"));

        var resultado = controller.iniciarSesion("usuario", "clave123");

        assertFalse(resultado.exito());
        assertNull(resultado.usuario());
        assertNull(resultado.nombreRol());
        assertTrue(resultado.menu().isEmpty());
        assertEquals("La conexión a la base de datos no está configurada. Contacta al administrador.",
                resultado.mensaje());
    }

    @Test
    void falloSqlNoSePresentaComoConfiguracionIncompleta() {
        var resultado = controladorQueFalla(new DataAccessException("Detalle técnico reservado"))
                .iniciarSesion("usuario", "clave123");

        assertFalse(resultado.exito());
        assertEquals("No se pudo conectar con la base de datos", resultado.mensaje());
        assertNull(resultado.usuario());
        assertTrue(resultado.menu().isEmpty());
    }

    @Test
    void erroresInesperadosSePropagan() {
        IllegalStateException fallo = new IllegalStateException("Error de programación");
        assertSame(fallo, assertThrows(IllegalStateException.class,
                () -> controladorQueFalla(fallo).iniciarSesion("usuario", "clave123")));
    }

    @Test
    void loginUsaElNombreDelRolRealAunqueSuIdSeaDistinto() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setIdRol(42);
        Rol rol = new Rol("ADMIN");
        rol.setIdRol(42);
        UsuarioService servicio = new UsuarioService() {
            @Override
            public Usuario autenticar(String username, String password) {
                return usuario;
            }

            @Override
            public Optional<Rol> buscarRolPorId(int idRol) {
                assertEquals(42, idRol);
                return Optional.of(rol);
            }
        };

        var resultado = new LoginController(servicio, new PermisoService())
                .iniciarSesion("ana", "clave123");

        assertTrue(resultado.exito());
        assertEquals("ADMIN", resultado.nombreRol());
        assertSame(usuario, resultado.usuario());
        assertEquals(Modulo.values().length, resultado.menu().size());
    }

    private LoginController controladorQueFalla(RuntimeException fallo) {
        return new LoginController(new UsuarioService() {
            @Override
            public Usuario autenticar(String username, String password) {
                throw fallo;
            }
        }, new PermisoService());
    }
}

package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Usuario;
import sigelab.service.UsuarioService;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioControllerIT {

    private final UsuarioService usuarioService = new UsuarioService();

    // Crea un usuario real en la base para usarlo como "quien tiene la sesión abierta"
    private Usuario crearActor(String prefijo, int idRol) {
        String username = prefijo + System.currentTimeMillis();
        return usuarioService.crearUsuario("Actor", "Prueba", username,
                "clave123", "actor@correo.test", idRol);
    }

    @Test
    void adminPuedeListarUsuarios() {
        UsuarioController controller = new UsuarioController(crearActor("test_uc_admin_", 1));

        var respuesta = controller.listar();

        assertTrue(respuesta.exito());
        assertFalse(respuesta.dato().isEmpty()); // al menos está el propio actor
    }

    @Test
    void encargadoNoPuedeListarUsuarios() {
        UsuarioController controller = new UsuarioController(crearActor("test_uc_enc_", 2));

        var respuesta = controller.listar();

        // Debe negarle el acceso y no devolver ningún usuario
        assertFalse(respuesta.exito());
        assertTrue(respuesta.dato().isEmpty());
    }

    @Test
    void encargadoNoPuedeCrearUsuarios() {
        UsuarioController controller = new UsuarioController(crearActor("test_uc_enc2_", 2));

        var respuesta = controller.crear("Nuevo", "Usuario",
                "test_uc_no_" + System.currentTimeMillis(), "clave123", "n@correo.test", 2);

        assertFalse(respuesta.exito());
        assertNull(respuesta.dato());
    }

    @Test
    void adminPuedeCrearUsuarios() {
        UsuarioController controller = new UsuarioController(crearActor("test_uc_admin2_", 1));

        var respuesta = controller.crear("Nuevo", "Usuario",
                "test_uc_si_" + System.currentTimeMillis(), "clave123", "n2@correo.test", 2);

        assertTrue(respuesta.exito());
        assertNotNull(respuesta.dato().getIdUsuario());
    }

    @Test
    void adminPuedeDesactivarAOtroPeroNoASiMismo() {
        Usuario admin = crearActor("test_uc_admin3_", 1);
        Usuario otro = crearActor("test_uc_otro_", 2);
        UsuarioController controller = new UsuarioController(admin);

        // Desactivar a otro: permitido
        var aOtro = controller.cambiarEstado(otro.getIdUsuario(), EstadoUsuario.INACTIVO);
        assertTrue(aOtro.exito());

        // Desactivarse a sí mismo: rechazado
        var aSiMismo = controller.cambiarEstado(admin.getIdUsuario(), EstadoUsuario.INACTIVO);
        assertFalse(aSiMismo.exito());
    }
}
package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.service.Modulo;
import sigelab.service.UsuarioService;

import static org.junit.jupiter.api.Assertions.*;

class LoginControllerIT {

    private final LoginController controller = new LoginController();
    private final UsuarioService usuarioService = new UsuarioService();

    @Test
    void adminEntraYVeTodoElMenu() throws Exception {
        // Creamos un usuario con rol 1 (ADMIN)
        String username = "test_ctrl_admin_" + System.currentTimeMillis();
        usuarioService.crearUsuario("Ana", "Admin", username, "clave123", "ana@correo.test", 1);

        var resultado = controller.iniciarSesion(username, "clave123");

        assertTrue(resultado.exito());
        // El admin debe ver todas las opciones
        assertEquals(Modulo.values().length, resultado.menu().size());
    }

    @Test
    void encargadoEntraPeroNoVeUsuarios() throws Exception {
        // Creamos un usuario con rol 2 (ENCARGADO)
        String username = "test_ctrl_enc_" + System.currentTimeMillis();
        usuarioService.crearUsuario("Luis", "Encargado", username, "clave123", "luis@correo.test", 2);

        var resultado = controller.iniciarSesion(username, "clave123");

        assertTrue(resultado.exito());
        assertFalse(resultado.menu().contains(Modulo.USUARIOS));
        assertTrue(resultado.menu().contains(Modulo.RESERVAS_PRESTAMOS));
    }

    @Test
    void contrasenaIncorrectaNoEntra() throws Exception {
        String username = "test_ctrl_mala_" + System.currentTimeMillis();
        usuarioService.crearUsuario("Luis", "Prueba", username, "clave123", "luis2@correo.test", 2);

        var resultado = controller.iniciarSesion(username, "otra_clave");

        // No entra, no hay usuario y el menú viene vacío
        assertFalse(resultado.exito());
        assertNull(resultado.usuario());
        assertTrue(resultado.menu().isEmpty());
    }
}
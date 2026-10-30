package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.service.Modulo;
import sigelab.service.UsuarioService;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="SIGELAB_RUN_SQL_TESTS", matches="1")
class LoginControllerIT {

    private final UsuarioService usuarioService = new UsuarioService();
    // Integración SQL: FaceID simulado explícitamente para no abrir cámara.
    private final LoginController controller = new LoginController(new sigelab.service.AutenticacionService(
            usuarioService, new sigelab.service.PermisoService(), id -> true, new sigelab.service.AuditService()));

    private int rol(String nombre) {
        return usuarioService.listarRoles().stream().filter(r -> nombre.equals(r.getNombre()))
                .findFirst().orElseThrow().getIdRol();
    }

    @Test
    void adminEntraYVeTodoElMenu() throws Exception {
        // Resolvemos ADMIN por su nombre real.
        String username = "test_ctrl_admin_" + System.currentTimeMillis();
        usuarioService.crearUsuario("Ana", "Admin", username, "clave123", "ana@correo.test", rol("ADMIN"));

        var resultado = controller.iniciarSesion(username, "clave123");

        assertTrue(resultado.exito());
        // El admin debe ver todas las opciones
        assertEquals(Modulo.values().length, resultado.menu().size());
    }

    @Test
    void encargadoEntraPeroNoVeUsuarios() throws Exception {
        // Resolvemos ENCARGADO por su nombre real.
        String username = "test_ctrl_enc_" + System.currentTimeMillis();
        usuarioService.crearUsuario("Luis", "Encargado", username, "clave123", "luis@correo.test", rol("ENCARGADO"));

        var resultado = controller.iniciarSesion(username, "clave123");

        assertTrue(resultado.exito());
        assertFalse(resultado.menu().contains(Modulo.USUARIOS));
        assertTrue(resultado.menu().contains(Modulo.RESERVAS_PRESTAMOS));
    }

    @Test
    void contrasenaIncorrectaNoEntra() throws Exception {
        String username = "test_ctrl_mala_" + System.currentTimeMillis();
        usuarioService.crearUsuario("Luis", "Prueba", username, "clave123", "luis2@correo.test", rol("ENCARGADO"));

        var resultado = controller.iniciarSesion(username, "otra_clave");

        // No entra, no hay usuario y el menú viene vacío
        assertFalse(resultado.exito());
        assertNull(resultado.usuario());
        assertTrue(resultado.menu().isEmpty());
    }
}
package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.exception.BusinessException;
import sigelab.model.Auditoria;
import sigelab.model.Usuario;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoginServiceIT {

    private final UsuarioService usuarioService = new UsuarioService();
    private final LoginService loginService = new LoginService();
    private final AuditService auditService = new AuditService();

    // Crea un usuario real con un username que nunca se repite
    private Usuario crearUsuario(String prefijo) {
        String username = prefijo + System.currentTimeMillis();
        return usuarioService.crearUsuario("Prueba", "Login", username,
                "clave123", "login@correo.test", 2);
    }

    @Test
    void loginExitosoDejaRegistroEnAuditoria() {
        Usuario u = crearUsuario("test_aud_ok_");

        Usuario entro = loginService.iniciarSesion(u.getUsername(), "clave123");
        assertEquals(u.getIdUsuario(), entro.getIdUsuario());

        // Buscamos en auditoría los LOGIN_EXITOSO de este usuario
        List<Auditoria> registros =
                auditService.filtrarHistorial("LOGIN_EXITOSO", "Usuario", u.getIdUsuario());

        assertFalse(registros.isEmpty(), "Debe haber un LOGIN_EXITOSO guardado");
    }

    @Test
    void loginFallidoDeUsuarioExistenteGuardaSuId() {
        Usuario u = crearUsuario("test_aud_mal_");

        // Contraseña incorrecta: el login falla y el error sale hacia la pantalla
        assertThrows(BusinessException.class,
                () -> loginService.iniciarSesion(u.getUsername(), "otra_clave"));

        List<Auditoria> registros =
                auditService.filtrarHistorial("LOGIN_FALLIDO", "Usuario", u.getIdUsuario());

        assertFalse(registros.isEmpty(), "Debe haber un LOGIN_FALLIDO con el id del usuario");
    }

    @Test
    void loginFallidoDeUsuarioInexistenteQuedaRegistrado() {
        String inexistente = "test_aud_fantasma_" + System.currentTimeMillis();

        assertThrows(BusinessException.class,
                () -> loginService.iniciarSesion(inexistente, "clave123"));

        // Como el usuario no existe, el registro va sin id: buscamos sin filtrar por usuario
        List<Auditoria> registros =
                auditService.filtrarHistorial("LOGIN_FALLIDO", "Usuario", null);

        boolean aparece = registros.stream()
                .anyMatch(a -> a.getDescripcion().contains(inexistente));
        assertTrue(aparece, "El intento con un usuario inexistente también debe quedar anotado");
    }

    @Test
    void laContrasenaNuncaSeGuardaEnAuditoria() {
        Usuario u = crearUsuario("test_aud_pass_");
        String passwordSecreta = "SECRETA_" + System.currentTimeMillis();

        assertThrows(BusinessException.class,
                () -> loginService.iniciarSesion(u.getUsername(), passwordSecreta));

        List<Auditoria> registros =
                auditService.filtrarHistorial("LOGIN_FALLIDO", "Usuario", u.getIdUsuario());

        // Ninguna descripción debe contener la contraseña que se escribió
        assertTrue(registros.stream()
                .noneMatch(a -> a.getDescripcion().contains(passwordSecreta)));
    }
}
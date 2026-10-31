package sigelab.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditoriaModelTest {

    @Test
    @DisplayName("Debe permitir registro de auditoría con usuario identificado")
    void debePermitirRegistroConUsuario() {
        LocalDateTime ahora = LocalDateTime.now();
        Auditoria a = new Auditoria(1, ahora, "LOGIN_EXITOSO", "Usuario", 1, "Inicio de sesión correcto");
        a.setIdAuditoria(50);

        assertEquals(50, a.getIdAuditoria());
        assertEquals(1, a.getIdUsuario());
        assertEquals(ahora, a.getFechaHora());
        assertEquals("LOGIN_EXITOSO", a.getAccion());
        assertEquals("Usuario", a.getEntidad());
        assertEquals(1, a.getIdRegistro());
        assertEquals("Inicio de sesión correcto", a.getDescripcion());
    }

    @Test
    @DisplayName("Debe permitir usuario_id nulo para intentos de login fallido (CP-02)")
    void debePermitirUsuarioNuloParaLoginFallido() {
        Auditoria a = new Auditoria(null, LocalDateTime.now(), "LOGIN_FALLIDO", "Seguridad", null, "Credenciales incorrectas");

        assertNull(a.getIdUsuario());
        assertEquals("LOGIN_FALLIDO", a.getAccion());
        assertEquals("Seguridad", a.getEntidad());
        assertNull(a.getIdRegistro());
    }
}

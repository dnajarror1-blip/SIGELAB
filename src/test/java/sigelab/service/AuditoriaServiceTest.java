package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.model.Auditoria;
import sigelab.repository.AuditoriaRepository;
import sigelab.exception.DataAccessException;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class AuditoriaServiceTest {
    @Test void registroNoCriticoToleraFalloSinBaseReal() {
        AuditService service = new AuditService(new AuditoriaRepository() {
            public void guardar(Auditoria a) throws SQLException { throw new SQLException("Simulado"); }
        });
        assertDoesNotThrow(() -> service.registrar(null, "LOGIN_FALLIDO", "Usuario", null, "Credenciales rechazadas"));
    }
    @Test void permiteUsuarioNuloYRegistraFechaReferenciaDescripcion() {
        AuditService service = new AuditService(new AuditoriaRepository() {
            public void guardar(Auditoria a) {
                assertNull(a.getIdUsuario()); assertNotNull(a.getFechaHora());
                assertEquals("LOGIN_FALLIDO", a.getAccion()); assertEquals("Usuario", a.getEntidad());
                assertNull(a.getIdRegistro()); assertEquals("Credenciales rechazadas", a.getDescripcion());
            }
        });
        service.registrar(null, "LOGIN_FALLIDO", "Usuario", null, "Credenciales rechazadas");
    }
    @Test void obligatoriaPropagaFalloSqlConMensajeSeguro() {
        AuditService service = new AuditService(new AuditoriaRepository() {
            public void guardar(Auditoria a) throws SQLException { throw new SQLException("SQL privado"); }
        });
        var error = assertThrows(DataAccessException.class,
                () -> service.registrarObligatorio(null,"LOGIN_FALLIDO","Usuario",null,"Acceso denegado."));
        assertFalse(error.getMessage().contains("SQL privado"));
    }
    @Test void obligatoriaPersisteUnaVezConReferenciasNulas() {
        int[] llamadas = {0};
        AuditService service = new AuditService(new AuditoriaRepository() {
            public void guardar(Auditoria a) {
                llamadas[0]++; assertNull(a.getIdUsuario()); assertNull(a.getIdRegistro());
                assertEquals("LOGIN_FALLIDO",a.getAccion()); assertNotNull(a.getFechaHora());
            }
        });
        service.registrarObligatorio(null,"LOGIN_FALLIDO","Usuario",null,"Acceso denegado.");
        assertEquals(1,llamadas[0]);
    }
    @Test void falloConsultaNoPareceHistorialVacio() {
        AuditService service = new AuditService(new AuditoriaRepository() {
            public java.util.List<Auditoria> listarTodos() throws SQLException { throw new SQLException("Privado"); }
            public java.util.List<Auditoria> buscarPorFiltros(String a, String e, Integer u) throws SQLException { throw new SQLException("Privado"); }
        });
        assertThrows(DataAccessException.class, service::obtenerHistorial);
        assertThrows(DataAccessException.class, () -> service.filtrarHistorial(null, null, null));
    }
}

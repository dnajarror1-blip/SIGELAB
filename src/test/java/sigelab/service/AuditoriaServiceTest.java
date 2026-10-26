package sigelab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditoriaServiceTest {

    private AuditService auditoriaService;

    @BeforeEach
    void setUp() {
        auditoriaService = new AuditService();
    }

    @Test
    @DisplayName("Debe manejar fallos de persistencia de forma no bloqueante (RN-18)")
    void debeEjecutarRegistroSinLanzarExcepcionBloqueante() {
        // Al no haber conexión activa en prueba unitaria pura, no debe tumbar la ejecución
        assertDoesNotThrow(() ->
            auditoriaService.registrar(1, "LOGIN_TEST", "Usuario", 1, "Prueba no bloqueante")
        );
    }

    @Test
    @DisplayName("Debe manejar usuario nulo sin excepción (CP-02)")
    void debePermitirRegistroConUsuarioNulo() {
        assertDoesNotThrow(() ->
            auditoriaService.registrar(null, "LOGIN_FALLIDO", "Seguridad", null, "Intento no autenticado")
        );
    }
}

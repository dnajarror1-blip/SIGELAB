package sigelab.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermisoServiceTest {

    private final PermisoService permisos = new PermisoService();

    @Test
    void adminPuedeEntrarATodo() {
        // Probamos con cada módulo que existe, el admin debe poder con todos
        for (Modulo m : Modulo.values()) {
            assertTrue(permisos.puedeAcceder(1, m), "El admin debería poder entrar a " + m);
        }
    }

    @Test
    void encargadoNoPuedeAdministrarUsuarios() {
        assertFalse(permisos.puedeAcceder(2, Modulo.USUARIOS));
        assertFalse(permisos.puedeAcceder(2, Modulo.ENROLLMENT_FACIAL));
        assertFalse(permisos.puedeAcceder(2, Modulo.AUDITORIA));
    }

    @Test
    void encargadoSiPuedeHacerElTrabajoDiario() {
        assertTrue(permisos.puedeAcceder(2, Modulo.LABORATORIOS_EQUIPOS));
        assertTrue(permisos.puedeAcceder(2, Modulo.RESERVAS_PRESTAMOS));
        assertTrue(permisos.puedeAcceder(2, Modulo.NOTIFICACIONES));
    }

    @Test
    void rolDesconocidoNoVeNada() {
        // Un rol que no existe (por ejemplo el 99) no debe tener ninguna opción
        assertTrue(permisos.modulosDe(99).isEmpty());
    }
}
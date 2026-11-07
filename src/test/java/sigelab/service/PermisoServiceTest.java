package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.model.Rol;
import sigelab.repository.RolRepository;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermisoServiceTest {

    private final PermisoService permisos = new PermisoService(new RolRepository() {
        @Override
        public Optional<Rol> buscarPorId(int idRol) {
            String nombre = switch (idRol) {
                case 42 -> "ADMIN";
                case 17 -> "ENCARGADO";
                case 77 -> "ESTUDIANTE";
                case 1, 2, 3 -> "OTRO";
                default -> null;
            };
            if (nombre == null) {
                return Optional.empty();
            }
            Rol rol = new Rol(nombre);
            rol.setIdRol(idRol);
            return Optional.of(rol);
        }
    });

    @Test
    void adminPuedeEntrarATodo() {
        // Probamos con cada módulo que existe, el admin debe poder con todos
        for (Modulo m : Modulo.values()) {
            assertTrue(permisos.puedeAcceder(42, m), "El admin debería poder entrar a " + m);
        }
    }

    @Test
    void encargadoNoPuedeAdministrarUsuarios() {
        assertFalse(permisos.puedeAcceder(17, Modulo.USUARIOS));
        assertFalse(permisos.puedeAcceder(17, Modulo.ENROLLMENT_FACIAL));
        assertFalse(permisos.puedeAcceder(17, Modulo.AUDITORIA));
    }

    @Test
    void encargadoSiPuedeHacerElTrabajoDiario() {
        assertTrue(permisos.puedeAcceder(17, Modulo.LABORATORIOS_EQUIPOS));
        assertTrue(permisos.puedeAcceder(17, Modulo.RESERVAS_PRESTAMOS));
        assertTrue(permisos.puedeAcceder(17, Modulo.NOTIFICACIONES));
        assertTrue(permisos.puedeAcceder(17, Modulo.REPORTES_QR));
    }

    @Test
    void rolDesconocidoNoVeNada() {
        // Un rol que no existe (por ejemplo el 99) no debe tener ninguna opción
        assertTrue(permisos.modulosDe(99).isEmpty());
    }

    @Test
    void estudianteSoloPuedeConsultarLaboratoriosYEquipos() {
        assertEquals(Set.of(Modulo.CONSULTA_LABORATORIOS, Modulo.CONSULTA_EQUIPOS),
                permisos.modulosDe(77));
        for (Modulo modulo : new Modulo[]{Modulo.USUARIOS, Modulo.MANTENIMIENTO,
                Modulo.REPORTES_QR, Modulo.AUDITORIA, Modulo.ENROLLMENT_FACIAL}) {
            assertFalse(permisos.puedeAcceder(77, modulo));
        }
    }

    @Test
    void idsUnoDosYTresNoDefinenElRol() {
        for (int id : new int[]{1, 2, 3}) {
            assertTrue(permisos.modulosDe(id).isEmpty());
        }
    }
}

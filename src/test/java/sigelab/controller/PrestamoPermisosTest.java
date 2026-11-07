package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.exception.BusinessException;
import sigelab.model.Prestamo;
import sigelab.service.PrestamoService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PrestamoPermisosTest {
    @Test
    void sinSesionRechazaConsultasYRegistro() {
        PrestamoController controller = new PrestamoController();
        assertThrows(BusinessException.class, controller::buscarActivos);
        assertThrows(BusinessException.class, () -> controller.buscarPorId(1));
        assertThrows(BusinessException.class, () -> controller.buscarDetalles(1));
        assertThrows(BusinessException.class, () -> controller.crearPrestamo(null, List.of(1)));
        assertThrows(BusinessException.class, () -> controller.identificarEquipoPorQr(null));
    }

    @Test
    void usuarioNoAutorizadoNoPuedeConsultarDatosGenerales() {
        PrestamoService servicio = new PrestamoService() {
            @Override public void validarUsuarioOperador(int id) {
                throw new BusinessException("No tienes permiso para gestionar préstamos.");
            }
            @Override public List<Prestamo> buscarActivos() {
                throw new AssertionError("No debe consultar préstamos ajenos");
            }
        };
        PrestamoController controller = new PrestamoController(servicio, 8);
        assertThrows(BusinessException.class, controller::buscarActivos);
        assertThrows(BusinessException.class, () -> controller.buscarPorId(1));
        assertThrows(BusinessException.class, () -> controller.buscarDetalles(1));
    }

    @Test
    void operadorConsultaPeroNoPuedeRegistrarConOtraIdentidad() {
        PrestamoService servicio = new PrestamoService() {
            @Override public void validarUsuarioOperador(int id) { assertEquals(8, id); }
            @Override public Optional<Prestamo> buscarPorId(int id) { return Optional.empty(); }
            @Override public int crearPrestamo(Prestamo p, List<Integer> ids) {
                throw new AssertionError("No debe registrar con otra identidad");
            }
        };
        PrestamoController controller = new PrestamoController(servicio, 8);
        assertTrue(controller.buscarPorId(1).isEmpty());
        Prestamo p = new Prestamo();
        p.setIdUsuarioRegistra(9);
        assertThrows(BusinessException.class, () -> controller.crearPrestamo(p, List.of(1)));
    }
}

package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoEquipo;
import sigelab.exception.BusinessException;
import sigelab.integration.qr.QrService;
import sigelab.model.Equipo;
import sigelab.repository.*;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PrestamoQrTest {
    @Test
    void identificaEquipoRealDisponibleSinCambiarEstadoNiCrearPrestamo() {
        Equipo equipo = equipo(EstadoEquipo.DISPONIBLE);
        assertSame(equipo, servicio(equipo).identificarEquipoPorQr(new File("qr-simulado.png")));
        assertEquals(EstadoEquipo.DISPONIBLE, equipo.getEstado());
    }

    @Test
    void rechazaTodosLosEstadosNoDisponiblesSinCambiarlos() {
        for (EstadoEquipo estado : EstadoEquipo.values()) {
            if (estado == EstadoEquipo.DISPONIBLE) continue;
            Equipo equipo = equipo(estado);
            assertThrows(BusinessException.class,
                    () -> servicio(equipo).identificarEquipoPorQr(new File("qr-simulado.png")));
            assertEquals(estado, equipo.getEstado());
        }
    }

    @Test
    void rechazaCodigoQueNoExisteEnElRepositorio() {
        assertThrows(BusinessException.class,
                () -> servicio(null).identificarEquipoPorQr(new File("qr-simulado.png")));
    }

    private Equipo equipo(EstadoEquipo estado) {
        Equipo equipo = new Equipo();
        equipo.setIdEquipo(7);
        equipo.setCodigoInventario("CAM-001");
        equipo.setEstado(estado);
        return equipo;
    }

    private PrestamoService servicio(Equipo resultado) {
        QrService lector = new QrService() {
            @Override public String leer(File imagen) { return "CAM-001"; }
        };
        QrEquipoService qr = new QrEquipoService(lector, codigo -> {
            assertEquals("CAM-001", codigo);
            return Optional.ofNullable(resultado);
        });
        return new PrestamoService(new EquipoRepository(), new PrestamoRepository(),
                new DetallePrestamoRepository(), new AuditService(),
                () -> { throw new AssertionError("Leer QR no debe abrir una transacción de préstamo"); }, qr);
    }
}

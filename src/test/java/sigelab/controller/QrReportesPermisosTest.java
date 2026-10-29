package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.exception.BusinessException;
import sigelab.integration.qr.QrService;
import sigelab.model.Rol;
import sigelab.model.Usuario;
import sigelab.repository.ReporteRepository;
import sigelab.repository.RolRepository;
import sigelab.service.PermisoService;
import sigelab.service.QrEquipoService;
import sigelab.service.ReporteService;

import java.time.Clock;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class QrReportesPermisosTest {
    private final PermisoService permisos = new PermisoService(new RolRepository() {
        @Override
        public Optional<Rol> buscarPorId(int id) {
            Rol rol = new Rol("ESTUDIANTE");
            rol.setIdRol(id);
            return Optional.of(rol);
        }
    });

    @Test
    void estudianteNoPuedeGenerarNiLeerQrAunqueInvoqueElController() {
        Usuario estudiante = new Usuario();
        estudiante.setIdRol(77);
        QrEquipoService service = new QrEquipoService(new QrService(), codigo -> {
            throw new AssertionError("No debe consultar equipos sin permiso QR");
        });
        QrController controller = new QrController(estudiante, service, permisos);
        assertThrows(BusinessException.class, () -> controller.generar("INV-001", null));
        assertThrows(BusinessException.class, () -> controller.identificar(null));
    }

    @Test
    void estudianteNoPuedeConsultarElDashboardAunqueInvoqueElController() {
        Usuario estudiante = new Usuario();
        estudiante.setIdRol(77);
        ReporteService service = new ReporteService(new ReporteRepository() {
            @Override
            public Map<String, Object> obtenerConteosDashboard(java.time.LocalDate hoy) {
                throw new AssertionError("No debe consultar reportes sin permiso");
            }
        }, Clock.systemUTC());
        ReporteController controller = new ReporteController(estudiante, service, permisos);
        assertThrows(BusinessException.class, controller::dashboard);
    }
    @Test
    void estudianteNoPuedeConsultarHistorialEspecifico() {
        Usuario estudiante = new Usuario();
        estudiante.setIdRol(77);
        ReporteService service = new ReporteService(new ReporteRepository() {
            @Override
            public List<Map<String, Object>> listarHistorialEquipo(String codigo) {
                throw new AssertionError("No debe consultar historial sin permiso");
            }
        }, Clock.systemUTC());
        ReporteController controller = new ReporteController(estudiante, service, permisos);
        assertThrows(BusinessException.class, () -> controller.historialEquipo("INV-001"));
    }

    @Test
    void adminYEncargadoAccedenAlDashboardYAlHistorialSinIdsFijos() {
        for (String nombre : new String[]{"ADMIN", "ENCARGADO"}) {
            PermisoService autorizados = new PermisoService(new RolRepository() {
                @Override
                public Optional<Rol> buscarPorId(int id) {
                    Rol rol = new Rol(nombre);
                    rol.setIdRol(id);
                    return Optional.of(rol);
                }
            });
            Usuario usuario = new Usuario();
            usuario.setIdRol(91);
            Map<String, Object> conteos = Map.of("equipos_TOTAL", 4);
            List<Map<String, Object>> historial = List.of(Map.of("id_detalle_prestamo", 5));
            ReporteService service = new ReporteService(new ReporteRepository() {
                @Override
                public Map<String, Object> obtenerConteosDashboard(java.time.LocalDate hoy) {
                    return conteos;
                }

                @Override
                public List<Map<String, Object>> listarHistorialEquipo(String codigo) {
                    return historial;
                }
            }, Clock.systemUTC());
            ReporteController controller = new ReporteController(usuario, service, autorizados);
            org.junit.jupiter.api.Assertions.assertEquals(conteos, controller.dashboard());
            org.junit.jupiter.api.Assertions.assertEquals(historial, controller.historialEquipo("INV-001"));
        }
    }
}

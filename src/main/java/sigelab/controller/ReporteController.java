package sigelab.controller;

import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import sigelab.exception.BusinessException;
import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.service.PermisoService;
import sigelab.service.ReporteService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReporteController {
    private final Usuario usuario;
    private final ReporteService service;
    private final PermisoService permisos;

    public ReporteController(Usuario usuario) {
        this(usuario, new ReporteService(), new PermisoService());
    }

    public ReporteController(Usuario usuario, ReporteService service, PermisoService permisos) {
        this.usuario = usuario;
        this.service = service;
        this.permisos = permisos;
    }

    public List<Map<String, Object>> equiposPorEstado(EstadoEquipo estado) {
        verificarPermiso();
        return service.listarEquiposPorEstado(estado);
    }

    public List<Map<String, Object>> prestamos() {
        verificarPermiso();
        return service.listarPrestamos();
    }

    public List<Map<String, Object>> prestamosEntreFechas(LocalDate inicio, LocalDate fin) {
        verificarPermiso();
        return service.listarPrestamosEntreFechas(inicio, fin);
    }

    public List<Map<String, Object>> historialEquipos() {
        verificarPermiso();
        return service.listarHistorialEquipos();
    }

    public List<Map<String, Object>> historialEquipo(String codigoInventario) {
        verificarPermiso();
        return service.listarHistorialEquipo(codigoInventario);
    }

    public List<Map<String, Object>> historialMantenimientos(EstadoMantenimiento estado) {
        verificarPermiso();
        return service.listarHistorialMantenimientos(estado);
    }

    public Map<String, Object> dashboard() {
        verificarPermiso();
        return service.obtenerConteosDashboard();
    }

    private void verificarPermiso() {
        if (usuario == null || !permisos.puedeAcceder(usuario.getIdRol(), Modulo.REPORTES_QR)) {
            throw new BusinessException("No tienes permiso para acceder a reportes");
        }
    }
}

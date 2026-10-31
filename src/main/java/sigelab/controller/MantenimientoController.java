package sigelab.controller;

import java.util.List;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Mantenimiento;
import sigelab.service.AuditService;
import sigelab.service.MantenimientoService;
import sigelab.service.Modulo;

public class MantenimientoController {
    private final MantenimientoService servicio;
    private final AuditService permisos;
    private final int usuario;

    public MantenimientoController(int usuario) {
        this(new MantenimientoService(), new AuditService(), usuario);
    }

    public MantenimientoController(MantenimientoService servicio, AuditService permisos, int usuario) {
        this.servicio = servicio;
        this.permisos = permisos;
        this.usuario = usuario;
    }

    public List<Mantenimiento> listarTodos() {
        permisos.validarConsulta(usuario, Modulo.MANTENIMIENTO);
        return servicio.listarTodos();
    }

    public List<Mantenimiento> listarMantenimientosActivos() {
        permisos.validarConsulta(usuario, Modulo.MANTENIMIENTO);
        return servicio.listarMantenimientosActivos();
    }

    public Mantenimiento ingresarATaller(int equipo, TipoMantenimiento tipo, String descripcion, String observaciones) {
        return servicio.ingresarATaller(equipo, tipo, usuario, descripcion, observaciones);
    }

    public void finalizarMantenimiento(int mantenimiento, ResultadoMantenimiento resultado, String observaciones) {
        servicio.finalizarMantenimiento(mantenimiento, resultado, observaciones, usuario);
    }
}

package sigelab.controller;

import sigelab.service.*;
import sigelab.service.CatalogoSeleccionService.*;
import java.util.List;

/** Cada consulta mantiene el permiso del módulo que abre el selector. */
public class CatalogoSeleccionController {
    private final int actor;
    private final Modulo modulo;
    private final CatalogoSeleccionService servicio;
    private final AuditService permisos;
    public CatalogoSeleccionController(int actor, Modulo modulo) {
        this(actor, modulo, new CatalogoSeleccionService(), new AuditService());
    }
    public CatalogoSeleccionController(int actor, Modulo modulo, CatalogoSeleccionService servicio, AuditService permisos) {
        this.actor = actor; this.modulo = modulo; this.servicio = servicio; this.permisos = permisos;
    }
    public List<Solicitante> listarSolicitantesActivos() {
        permisos.validarConsulta(actor, Modulo.RESERVAS_PRESTAMOS);
        return servicio.listarSolicitantesActivos();
    }
    public java.util.Map<Integer, String> nombresUsuarios() {
        permisos.validarConsulta(actor, Modulo.RESERVAS_PRESTAMOS);
        return servicio.nombresUsuarios();
    }
    public List<EquipoOpcion> listarEquipos(boolean soloDisponibles) {
        permisos.validarConsulta(actor, modulo);
        return servicio.listarEquipos(soloDisponibles);
    }
}

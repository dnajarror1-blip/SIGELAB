package sigelab.controller;

import java.util.List;
import sigelab.model.Auditoria;
import sigelab.service.AuditService;
import sigelab.service.Modulo;

public class AuditoriaController {
    private final AuditService servicio;
    private final Integer usuario;

    public AuditoriaController(AuditService servicio, Integer usuario) {
        this.servicio = servicio;
        this.usuario = usuario;
    }

    public List<Auditoria> obtenerHistorial() {
        servicio.validarConsulta(usuario, Modulo.AUDITORIA);
        return servicio.obtenerHistorial();
    }

    public List<Auditoria> filtrarHistorial(String accion, String entidad) {
        servicio.validarConsulta(usuario, Modulo.AUDITORIA);
        return servicio.filtrarHistorial(accion, entidad, null);
    }
}

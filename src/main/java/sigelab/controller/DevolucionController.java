package sigelab.controller;

import java.util.List;
import java.util.Map;
import sigelab.enums.CondicionDevolucion;
import sigelab.model.Devolucion;
import sigelab.repository.DevolucionRepository.PrestamoPendiente;
import sigelab.service.DevolucionService;

public class DevolucionController {
    private final DevolucionService servicio;
    private final int actor;

    public DevolucionController(int actor) { this(new DevolucionService(), actor); }
    public DevolucionController(DevolucionService servicio, int actor) {
        this.servicio = servicio; this.actor = actor;
    }
    public List<PrestamoPendiente> listarPrestamosPendientes() {
        return servicio.listarPrestamosPendientes(actor);
    }
    public List<DevolucionService.Pendiente> buscarPendientes(int prestamo) {
        return servicio.buscarPendientes(prestamo, actor);
    }
    public List<Devolucion> registrar(int prestamo, Map<Integer, CondicionDevolucion> condiciones, String obs) {
        return servicio.registrar(prestamo, condiciones, obs, actor);
    }
}

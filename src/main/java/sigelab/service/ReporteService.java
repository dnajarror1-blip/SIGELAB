package sigelab.service;

import sigelab.exception.BusinessException;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import sigelab.repository.ReporteRepository;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReporteService {
    private final ReporteRepository repo;
    private final Clock reloj;

    public ReporteService() {
        this(new ReporteRepository(), Clock.systemDefaultZone());
    }

    public ReporteService(ReporteRepository repo, Clock reloj) {
        this.repo = repo;
        this.reloj = reloj;
    }

    public List<Map<String, Object>> listarEquiposPorEstado(EstadoEquipo estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del equipo es obligatorio");
        }
        return repo.listarEquiposPorEstado(estado);
    }

    public List<Map<String, Object>> listarPrestamos() {
        return clasificar(repo.listarPrestamosConEstadoReporte());
    }

    public List<Map<String, Object>> listarPrestamosEntreFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new IllegalArgumentException("El rango de fechas no es válido");
        }
        return clasificar(repo.listarPrestamosEntreFechas(inicio, fin));
    }

    public List<Map<String, Object>> listarHistorialEquipos() {
        return repo.listarHistorialEquipos();
    }

    public List<Map<String, Object>> listarHistorialEquipo(String codigoInventario) {
        if (codigoInventario == null || codigoInventario.isBlank()) {
            throw new IllegalArgumentException("El código de inventario es obligatorio");
        }
        List<Map<String, Object>> filas = repo.listarHistorialEquipo(codigoInventario.trim());
        if (filas.isEmpty()) {
            throw new BusinessException("No existe un equipo con ese código de inventario");
        }
        // El LEFT JOIN conserva una fila del equipo aunque nunca haya sido prestado.
        return filas.stream().filter(fila -> fila.get("id_detalle_prestamo") != null).toList();
    }

    public List<Map<String, Object>> listarHistorialMantenimientos(EstadoMantenimiento estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del mantenimiento es obligatorio");
        }
        return repo.listarHistorialMantenimientos(estado);
    }

    public Map<String, Object> obtenerConteosDashboard() {
        return repo.obtenerConteosDashboard(LocalDate.now(reloj));
    }

    private List<Map<String, Object>> clasificar(List<Map<String, Object>> filas) {
        LocalDate hoy = LocalDate.now(reloj);
        for (Map<String, Object> fila : filas) {
            Object fecha = fila.get("fecha_esperada_devolucion");
            LocalDate esperada = fecha instanceof Date sql ? sql.toLocalDate() : (LocalDate) fecha;
            String estado = (String) fila.get("estado");
            boolean pendiente = Boolean.TRUE.equals(fila.get("tiene_detalle_pendiente"));
            fila.put("estado_reporte", esVencido(estado, esperada, pendiente, hoy) ? "VENCIDO" : estado);
        }
        return filas;
    }

    public static boolean esVencido(String estado, LocalDate esperada, boolean pendiente, LocalDate hoy) {
        return "ACTIVO".equals(estado) && esperada != null && hoy != null
                && esperada.isBefore(hoy) && pendiente;
    }
}

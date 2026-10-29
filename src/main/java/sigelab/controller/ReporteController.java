package sigelab.controller;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import sigelab.service.ReporteService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
public class ReporteController{
 private final ReporteService service;
 public ReporteController(){this(new ReporteService());}
 public ReporteController(ReporteService s){if(s==null)throw new IllegalArgumentException("Servicio de reportes obligatorio.");service=s;}
 public List<Map<String,Object>> equiposPorEstado(EstadoEquipo e){return service.listarEquiposPorEstado(e);}
 public List<Map<String,Object>> prestamos(){return service.listarPrestamos();}
 public List<Map<String,Object>> prestamosEntreFechas(LocalDate a,LocalDate b){return service.listarPrestamosEntreFechas(a,b);}
 public List<Map<String,Object>> historialEquipos(){return service.listarHistorialEquipos();}
 public List<Map<String,Object>> historialMantenimientos(EstadoMantenimiento e){return service.listarHistorialMantenimientos(e);}
 public Map<String,Object> dashboard(){return service.obtenerConteosDashboard();}
}

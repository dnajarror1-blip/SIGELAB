package sigelab.service;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import sigelab.repository.ReporteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
public class ReporteService{
 private final ReporteRepository repo;
 public ReporteService(){this(new ReporteRepository());}
 public ReporteService(ReporteRepository r){if(r==null)throw new IllegalArgumentException("Repositorio de reportes obligatorio.");repo=r;}
 public List<Map<String,Object>> listarEquiposPorEstado(EstadoEquipo e){if(e==null)throw new IllegalArgumentException("Estado de equipo obligatorio.");return repo.listarEquiposPorEstado(e);}
 public List<Map<String,Object>> listarPrestamos(){
  List<Map<String,Object>> rows=repo.listarPrestamosConEstadoReporte(); LocalDate hoy=LocalDate.now();
  for(Map<String,Object> row:rows){
   Object raw=row.get("fecha_esperada_devolucion");
   LocalDate fecha=raw instanceof java.sql.Date d?d.toLocalDate():(LocalDate)raw;
   String estado=String.valueOf(row.get("estado"));
   boolean pendiente=Boolean.TRUE.equals(row.get("tiene_detalle_pendiente"));
   row.put("estado_reporte",esVencido(estado,fecha,pendiente,hoy)?"VENCIDO":estado);
  }
  return rows;
 }
 public List<Map<String,Object>> listarPrestamosEntreFechas(LocalDate a,LocalDate b){if(a==null||b==null||b.isBefore(a))throw new IllegalArgumentException("Rango de fechas inválido.");return repo.listarPrestamosEntreFechas(a,b);}
 public List<Map<String,Object>> listarHistorialEquipos(){return repo.listarHistorialEquipos();}
 public List<Map<String,Object>> listarHistorialMantenimientos(EstadoMantenimiento e){if(e==null)throw new IllegalArgumentException("Estado de mantenimiento obligatorio.");return repo.listarHistorialMantenimientos(e);}
 public Map<String,Object> obtenerConteosDashboard(){return repo.obtenerConteosDashboard();}
 public static boolean esVencido(String estado,LocalDate esperada,boolean pendiente,LocalDate hoy){return "ACTIVO".equals(estado)&&esperada!=null&&hoy!=null&&esperada.isBefore(hoy)&&pendiente;}
}

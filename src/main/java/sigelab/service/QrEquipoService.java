package sigelab.service;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.integration.qr.QrService;
import sigelab.model.Equipo;
import sigelab.repository.EquipoRepository;
import java.io.File;
import java.sql.SQLException;
import java.util.Optional;
public class QrEquipoService {
 private final QrService qr; private final EquipoLookup lookup;
 public QrEquipoService(){this(new QrService(),new RepoLookup(new EquipoRepository()));}
 public QrEquipoService(QrService qr,EquipoLookup lookup){if(qr==null||lookup==null)throw new IllegalArgumentException("Servicios QR y equipos obligatorios.");this.qr=qr;this.lookup=lookup;}
 public Resultado identificar(File imagen){String codigo=qr.leer(imagen);final Optional<Equipo> encontrado;try{encontrado=lookup.buscar(codigo);}catch(SQLException e){throw new DataAccessException("No se pudo consultar el equipo identificado por QR.",e);}Equipo equipo=encontrado.orElseThrow(()->new BusinessException("No existe un equipo con el código de inventario leído."));return new Resultado(codigo,equipo);}
 public void generar(String codigo,File destino){qr.generar(codigo,destino);}
 @FunctionalInterface public interface EquipoLookup{Optional<Equipo> buscar(String codigo)throws SQLException;}
 private record RepoLookup(EquipoRepository repo)implements EquipoLookup{public Optional<Equipo> buscar(String c)throws SQLException{return repo.buscarPorCodigoInventario(c);}}
 public record Resultado(String codigoInventario,Equipo equipo){public boolean disponibleParaUso(){return equipo.getEstado()==sigelab.enums.EstadoEquipo.DISPONIBLE;}public boolean bloqueadoPorPrestamo(){return equipo.getEstado()==sigelab.enums.EstadoEquipo.PRESTADO;}}
}

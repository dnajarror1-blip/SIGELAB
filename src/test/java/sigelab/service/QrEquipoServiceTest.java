package sigelab.service;
import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoEquipo;
import sigelab.model.Equipo;
import java.io.File;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
class QrEquipoServiceTest{
 @Test void disponibleSeIdentificaSinCambiarEstado()throws Exception{verificar(EstadoEquipo.DISPONIBLE,true,false);}
 @Test void prestadoSeIdentificaYSeBloqueaSinCambiarEstado()throws Exception{verificar(EstadoEquipo.PRESTADO,false,true);}
 private void verificar(EstadoEquipo estado,boolean disponible,boolean bloqueado)throws Exception{
  Equipo equipo=new Equipo();equipo.setEstado(estado);equipo.setNombre("Equipo prueba");
  QrEquipoService service=new QrEquipoService(new sigelab.integration.qr.QrService(),c->Optional.of(equipo));File f=File.createTempFile("qr-cp-",".png");
  try{new sigelab.integration.qr.QrService().generar("INV-CP",f);var r=service.identificar(f);assertEquals("INV-CP",r.codigoInventario());assertSame(equipo,r.equipo());assertEquals(disponible,r.disponibleParaUso());assertEquals(bloqueado,r.bloqueadoPorPrestamo());assertEquals(estado,equipo.getEstado());}finally{f.delete();}
 }
}

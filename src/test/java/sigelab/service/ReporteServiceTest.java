package sigelab.service;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class ReporteServiceTest{
 private final LocalDate hoy=LocalDate.of(2026,10,28);
 @Test void vencidoConPendiente(){assertTrue(ReporteService.esVencido("ACTIVO",hoy.minusDays(1),true,hoy));}
 @Test void todosDevueltosNoEsVencido(){assertFalse(ReporteService.esVencido("ACTIVO",hoy.minusDays(1),false,hoy));}
 @Test void fechaHoyNoEsVencida(){assertFalse(ReporteService.esVencido("ACTIVO",hoy,true,hoy));}
}

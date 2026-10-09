package sigelab.model;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoMantenimiento;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;

import static org.junit.jupiter.api.Assertions.*;

class MantenimientoModelTest {

    @Test
    @DisplayName("Debe instanciar y acceder correctamente a las propiedades de Mantenimiento")
    void debeInstanciarYAccederPropiedades() {
        LocalDate hoy = LocalDate.now();
        Mantenimiento m = new Mantenimiento(
            10,
            hoy,
            TipoMantenimiento.CORRECTIVO,
            "Fallo en puerto HDMI",
            1,
            null,
            null,
            EstadoMantenimiento.ABIERTO,
            "Reportado por docente"
        );

        m.setIdMantenimiento(100);

        assertEquals(100, m.getIdMantenimiento());
        assertEquals(10, m.getIdEquipo());
        assertEquals(hoy, m.getFechaIngreso());
        assertEquals(TipoMantenimiento.CORRECTIVO, m.getTipo());
        assertEquals("Fallo en puerto HDMI", m.getDescripcion());
        assertEquals(1, m.getIdResponsable());
        assertNull(m.getFechaSalida());
        assertNull(m.getResultado());
        assertEquals(EstadoMantenimiento.ABIERTO, m.getEstado());
        assertEquals("Reportado por docente", m.getObservaciones());
    }

    @Test
    @DisplayName("Debe actualizar el estado a CERRADO y registrar resultado")
    void debeActualizarEstadoYCierre() {
        Mantenimiento m = new Mantenimiento();
        m.setIdMantenimiento(1);
        m.setEstado(EstadoMantenimiento.ABIERTO);

        LocalDate salida = LocalDate.now();
        m.setFechaSalida(salida);
        m.setResultado(ResultadoMantenimiento.REPARADO);
        m.setEstado(EstadoMantenimiento.CERRADO);
        m.setObservaciones("Reparación exitosa");

        assertEquals(salida, m.getFechaSalida());
        assertEquals(ResultadoMantenimiento.REPARADO, m.getResultado());
        assertEquals(EstadoMantenimiento.CERRADO, m.getEstado());
        assertEquals("Reparación exitosa", m.getObservaciones());
    }
}

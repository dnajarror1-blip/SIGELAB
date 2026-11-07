package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.repository.ReporteRepository;
import sigelab.exception.BusinessException;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReporteServiceTest {
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC);
    private final LocalDate hoy = LocalDate.now(reloj);

    @Test
    void vencidoActivoConDetallePendiente() {
        assertEquals("VENCIDO", clasificar("ACTIVO", hoy.minusDays(1), true));
    }

    @Test
    void todosDevueltosNoEsVencido() {
        assertEquals("ACTIVO", clasificar("ACTIVO", hoy.minusDays(1), false));
    }

    @Test
    void fechaEsperadaHoyNoEsVencida() {
        assertEquals("ACTIVO", clasificar("ACTIVO", hoy, true));
    }

    @Test
    void finalizadoYFechaFuturaNoSonVencidos() {
        assertEquals("FINALIZADO", clasificar("FINALIZADO", hoy.minusDays(1), true));
        assertEquals("ACTIVO", clasificar("ACTIVO", hoy.plusDays(1), true));
    }

    @Test
    void consultaPorFechasTambienClasificaLosVencidos() {
        ReporteService servicio = new ReporteService(new ReporteRepository() {
            @Override
            public List<Map<String, Object>> listarPrestamosEntreFechas(LocalDate inicio, LocalDate fin) {
                assertEquals(hoy.minusDays(10), inicio);
                assertEquals(hoy, fin);
                return List.of(fila("ACTIVO", hoy.minusDays(1), true));
            }
        }, reloj);
        assertEquals("VENCIDO", servicio.listarPrestamosEntreFechas(hoy.minusDays(10), hoy)
                .getFirst().get("estado_reporte"));
    }

    @Test
    void dashboardUsaLaMismaFechaDeReferencia() {
        ReporteService servicio = new ReporteService(new ReporteRepository() {
            @Override
            public Map<String, Object> obtenerConteosDashboard(LocalDate fecha) {
                assertEquals(hoy, fecha);
                return Map.of("prestamos_VENCIDO", 1);
            }
        }, reloj);
        assertEquals(1, servicio.obtenerConteosDashboard().get("prestamos_VENCIDO"));
    }

    @Test
    void rangoInvalidoSeRechazaAntesDeConsultar() {
        ReporteService servicio = new ReporteService(new ReporteRepository() {
            @Override
            public List<Map<String, Object>> listarPrestamosEntreFechas(LocalDate inicio, LocalDate fin) {
                throw new AssertionError("No debe consultar con fechas inválidas");
            }
        }, reloj);
        assertThrows(IllegalArgumentException.class, () -> servicio.listarPrestamosEntreFechas(hoy, hoy.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarPrestamosEntreFechas(null, hoy));
    }

    @Test
    void historialEspecificoUsaElCodigoNormalizadoYConservaLosDatos() {
        Map<String, Object> prestamo = Map.of("codigo_inventario", "INV-001",
                "id_detalle_prestamo", 12, "estado_equipo", "PRESTADO");
        ReporteService servicio = historial(List.of(prestamo));
        assertEquals(List.of(prestamo), servicio.listarHistorialEquipo("  INV-001  "));
    }

    @Test
    void equipoExistenteSinPrestamosDevuelveHistorialVacio() {
        Map<String, Object> equipo = new LinkedHashMap<>();
        equipo.put("codigo_inventario", "INV-001");
        equipo.put("id_detalle_prestamo", null);
        assertTrue(historial(List.of(equipo)).listarHistorialEquipo("INV-001").isEmpty());
    }

    @Test
    void equipoInexistenteSeDistingueDeEquipoSinHistorial() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> historial(List.of()).listarHistorialEquipo("INV-001"));
        assertEquals("No existe un equipo con ese código de inventario", error.getMessage());
    }

    @Test
    void codigoObligatorioSeValidaAntesDeConsultar() {
        ReporteService servicio = new ReporteService(new ReporteRepository() {
            @Override
            public List<Map<String, Object>> listarHistorialEquipo(String codigo) {
                throw new AssertionError("No debe consultar con un código vacío");
            }
        }, reloj);
        assertThrows(IllegalArgumentException.class, () -> servicio.listarHistorialEquipo(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarHistorialEquipo("  "));
    }

    private ReporteService historial(List<Map<String, Object>> filas) {
        return new ReporteService(new ReporteRepository() {
            @Override
            public List<Map<String, Object>> listarHistorialEquipo(String codigo) {
                assertEquals("INV-001", codigo);
                return filas;
            }

            @Override
            public List<Map<String, Object>> listarHistorialEquipos() {
                throw new AssertionError("La consulta específica no debe cargar el historial general");
            }
        }, reloj);
    }

    private Object clasificar(String estado, LocalDate fecha, boolean pendiente) {
        Map<String, Object> fila = fila(estado, fecha, pendiente);
        ReporteService servicio = new ReporteService(new ReporteRepository() {
            @Override
            public List<Map<String, Object>> listarPrestamosConEstadoReporte() {
                return List.of(fila);
            }
        }, reloj);
        Object resultado = servicio.listarPrestamos().getFirst().get("estado_reporte");
        assertEquals(estado, fila.get("estado"));
        return resultado;
    }

    private Map<String, Object> fila(String estado, LocalDate fecha, boolean pendiente) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("estado", estado);
        fila.put("fecha_esperada_devolucion", Date.valueOf(fecha));
        fila.put("tiene_detalle_pendiente", pendiente);
        return fila;
    }
}

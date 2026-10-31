package sigelab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.exception.BusinessException;
import sigelab.enums.TipoMantenimiento;

import static org.junit.jupiter.api.Assertions.*;

class MantenimientoServiceTest {

    private MantenimientoService service;

    @BeforeEach
    void setUp() {
        service = new MantenimientoService();
    }

    @Test
    @DisplayName("Debe lanzar excepción si el ID de equipo es menor o igual a cero")
    void debeRechazarIdEquipoInvalido() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            service.ingresarATaller(0, TipoMantenimiento.PREVENTIVO, 1, "Limpieza de ventiladores", "Ninguna")
        );
        assertTrue(ex.getMessage().contains("no es válido"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el tipo de mantenimiento es nulo")
    void debeRechazarTipoMantenimientoNulo() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            service.ingresarATaller(1, null, 1, "Mantenimiento general", "")
        );
        assertTrue(ex.getMessage().contains("tipo de mantenimiento"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la descripción del mantenimiento está vacía")
    void debeRechazarDescripcionVacia() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            service.ingresarATaller(1, TipoMantenimiento.CORRECTIVO, 1, "   ", "")
        );
        assertTrue(ex.getMessage().contains("descripción"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el responsable es inválido")
    void debeRechazarResponsableInvalido() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            service.ingresarATaller(1, TipoMantenimiento.PREVENTIVO, -1, "Revisión técnica", "")
        );
        assertTrue(ex.getMessage().contains("responsable"));
    }

    @Test
    @DisplayName("Debe lanzar excepción al finalizar si el ID de mantenimiento es inválido")
    void debeRechazarIdMantenimientoInvalidoAlFinalizar() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            service.finalizarMantenimiento(0, ResultadoMantenimiento.REPARADO, "Todo en orden", 1)
        );
        assertTrue(ex.getMessage().contains("no es válido"));
    }

    @Test
    @DisplayName("Debe lanzar excepción al finalizar si el resultado técnico es nulo")
    void debeRechazarResultadoNuloAlFinalizar() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
            service.finalizarMantenimiento(5, null, "Sin resultado", 1)
        );
        assertTrue(ex.getMessage().contains("resultado técnico"));
    }
}

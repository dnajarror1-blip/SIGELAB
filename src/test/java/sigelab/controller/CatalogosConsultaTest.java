package sigelab.controller;

import org.junit.jupiter.api.Test;
import sigelab.exception.BusinessException;
import sigelab.enums.EstadoLaboratorio;
import sigelab.model.Equipo;
import sigelab.model.Laboratorio;
import sigelab.service.EquipoService;
import sigelab.service.LaboratorioService;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogosConsultaTest {
    @Test
    void equiposDeConsultaNoPermitenEscrituras() {
        EquipoController controller = new EquipoController(true, new EquipoService() {
            @Override
            public Equipo crear(String codigo, String nombre, int tipo, String marca,
                                String modelo, String serie, int laboratorio, String observaciones) {
                throw new AssertionError("No debe llamar al servicio de escritura");
            }

            @Override
            public void actualizar(int id, String codigo, String nombre, int tipo, String marca,
                                   String modelo, String serie, int laboratorio, String observaciones) {
                throw new AssertionError("No debe llamar al servicio de escritura");
            }

            @Override
            public void eliminar(int id) {
                throw new AssertionError("No debe llamar al servicio de escritura");
            }
        });
        assertThrows(BusinessException.class,
                () -> controller.crear(null, null, 0, null, null, null, 0, null));
        assertThrows(BusinessException.class,
                () -> controller.actualizar(1, null, null, 0, null, null, null, 0, null));
        assertThrows(BusinessException.class, () -> controller.eliminar(1));
    }

    @Test
    void laboratoriosDeConsultaNoPermitenEscrituras() {
        LaboratorioController controller = new LaboratorioController(true, new LaboratorioService() {
            @Override
            public Laboratorio crear(String codigo, String nombre, String ubicacion,
                                     int capacidad, EstadoLaboratorio estado) {
                throw new AssertionError("No debe llamar al servicio de escritura");
            }

            @Override
            public void actualizar(int id, String codigo, String nombre, String ubicacion,
                                   int capacidad, EstadoLaboratorio estado) {
                throw new AssertionError("No debe llamar al servicio de escritura");
            }

            @Override
            public void eliminar(int id) {
                throw new AssertionError("No debe llamar al servicio de escritura");
            }
        });
        assertThrows(BusinessException.class, () -> controller.crear(null, null, null, 0, null));
        assertThrows(BusinessException.class, () -> controller.actualizar(1, null, null, null, 0, null));
        assertThrows(BusinessException.class, () -> controller.eliminar(1));
    }
}

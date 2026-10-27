package sigelab.service;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import sigelab.exception.BusinessException;
import sigelab.model.Prestamo;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PrestamoServiceTest {

    private final PrestamoService servicio = new PrestamoService();

    @org.jetbrains.annotations.NotNull
    private Prestamo crearPrestamoValido() {
        Prestamo prestamo = new Prestamo();
        prestamo.setIdUsuarioSolicitante(1);
        prestamo.setIdUsuarioRegistra(1);
        prestamo.setFechaPrestamo(LocalDate.of(2026, 10, 26));
        prestamo.setFechaEsperadaDevolucion(LocalDate.of(2026, 10, 30));
        return prestamo;
    }

    @Test
    void rechazaPrestamoNulo() {
        assertThrows(
                BusinessException.class,
                () -> servicio.crearPrestamo(null, List.of(1))
        );
    }

    @Test
    void rechazaListaDeEquiposVacia() {
        Prestamo prestamo = crearPrestamoValido();

        assertThrows(
                BusinessException.class,
                () -> servicio.crearPrestamo(prestamo, List.of())
        );
    }

    @Test
    void rechazaEquiposRepetidos() {
        Prestamo prestamo = crearPrestamoValido();

        assertThrows(
                BusinessException.class,
                () -> servicio.crearPrestamo(prestamo, List.of(1, 1))
        );
    }

    @Test
    void rechazaFechaDeDevolucionAnteriorAlPrestamo() {
        Prestamo prestamo = crearPrestamoValido();
        prestamo.setFechaEsperadaDevolucion(LocalDate.of(2026, 10, 25));

        assertThrows(
                BusinessException.class,
                () -> servicio.crearPrestamo(prestamo, List.of(1))
        );
    }
}
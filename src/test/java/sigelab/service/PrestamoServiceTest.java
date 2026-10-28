package sigelab.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import sigelab.enums.EstadoEquipo;
import sigelab.exception.BusinessException;
import sigelab.model.Equipo;
import sigelab.model.Prestamo;
import sigelab.repository.EquipoRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrestamoServiceTest {

    private final PrestamoService servicio = new PrestamoService();

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

    @Test
    void cp06RechazaEquipoDanado() {
        comprobarEstadoBloqueado(EstadoEquipo.DANADO);
    }

    @Test
    void cp07RechazaEquipoEnMantenimiento() {
        comprobarEstadoBloqueado(EstadoEquipo.MANTENIMIENTO);
    }

    @Test
    void cp08RechazaEquipoDeBaja() {
        comprobarEstadoBloqueado(EstadoEquipo.BAJA);
    }

    private void comprobarEstadoBloqueado(EstadoEquipo estado) {
        Equipo equipo = new Equipo();
        equipo.setIdEquipo(100);
        equipo.setEstado(estado);

        EquipoRepository repositorioSimulado = new EquipoRepository() {
            @Override
            public Optional<Equipo> buscarPorId(int idEquipo) {
                return Optional.of(equipo);
            }
        };

        PrestamoService servicioConEquipo =
                new PrestamoService(repositorioSimulado);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> servicioConEquipo.validarEquipoParaPrestamo(100)
        );

        assertTrue(
                error.getMessage().contains(estado.name()),
                "El mensaje debe indicar el estado que bloqueó el préstamo."
        );
    }
}
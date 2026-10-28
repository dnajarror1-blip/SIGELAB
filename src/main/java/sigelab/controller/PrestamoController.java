package sigelab.controller;

import java.util.List;
import java.util.Optional;

import sigelab.model.Prestamo;
import sigelab.service.PrestamoService;

public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController() {
        this(new PrestamoService());
    }

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    public int crearPrestamo(
            Prestamo prestamo,
            List<Integer> idsEquipo
    ) {
        return prestamoService.crearPrestamo(prestamo, idsEquipo);
    }

    public List<Prestamo> buscarActivos() {
        return prestamoService.buscarActivos();
    }

    public Optional<Prestamo> buscarPorId(int idPrestamo) {
        return prestamoService.buscarPorId(idPrestamo);
    }

    public void validarEquipoParaPrestamo(int idEquipo) {
        prestamoService.validarEquipoParaPrestamo(idEquipo);
    }
}
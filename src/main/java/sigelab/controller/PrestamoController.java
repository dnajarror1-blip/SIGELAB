package sigelab.controller;

import java.util.List;
import java.util.Optional;

import sigelab.model.Prestamo;
import sigelab.service.PrestamoService;
import java.io.File;
import sigelab.exception.BusinessException;
import sigelab.model.Equipo;
import sigelab.model.DetallePrestamo;

public class PrestamoController {

    private final PrestamoService prestamoService;
    private final Integer idUsuarioActual;

    public PrestamoController() {
        this(new PrestamoService());
    }

    public PrestamoController(PrestamoService prestamoService) {
        this(prestamoService, null);
    }

    public PrestamoController(int idUsuarioActual) {
        this(new PrestamoService(), idUsuarioActual);
    }

    public PrestamoController(PrestamoService prestamoService, Integer idUsuarioActual) {
        this.prestamoService = prestamoService;
        this.idUsuarioActual = idUsuarioActual;
    }

    public int crearPrestamo(
            Prestamo prestamo,
            List<Integer> idsEquipo
    ) {
        verificarPermiso();
        if (prestamo != null && prestamo.getIdUsuarioRegistra() != idUsuarioActual) {
            throw new BusinessException("El usuario que registra debe coincidir con la sesión.");
        }
        return prestamoService.crearPrestamo(prestamo, idsEquipo);
    }

    public List<Prestamo> buscarActivos() {
        verificarPermiso();
        return prestamoService.buscarActivos();
    }

    public Optional<Prestamo> buscarPorId(int idPrestamo) {
        verificarPermiso();
        return prestamoService.buscarPorId(idPrestamo);
    }

    public void validarEquipoParaPrestamo(int idEquipo) {
        verificarPermiso();
        prestamoService.validarEquipoParaPrestamo(idEquipo);
    }
    public Equipo identificarEquipoPorQr(File imagen) {
        verificarPermiso();
        return prestamoService.identificarEquipoPorQr(imagen);
    }

    public List<DetallePrestamo> buscarDetalles(int idPrestamo) {
        verificarPermiso();
        return prestamoService.buscarDetalles(idPrestamo);
    }

    private void verificarPermiso() {
        if (idUsuarioActual == null || idUsuarioActual <= 0) {
            throw new BusinessException("Debe iniciar sesión para gestionar préstamos.");
        }
        prestamoService.validarUsuarioOperador(idUsuarioActual);
    }
}

package sigelab.service;

import java.sql.SQLException;

import sigelab.enums.EstadoEquipo;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.model.Equipo;
import sigelab.repository.EquipoRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sigelab.model.Prestamo;

public class PrestamoService {

    private final EquipoRepository equipoRepository;

    public PrestamoService() {
        this(new EquipoRepository());
    }

    public PrestamoService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public void validarEquipoParaPrestamo(int idEquipo) {
        if (idEquipo <= 0) {
            throw new BusinessException(
                    "El identificador del equipo debe ser mayor que cero."
            );
        }

        try {
            Equipo equipo = equipoRepository.buscarPorId(idEquipo)
                    .orElseThrow(() -> new BusinessException(
                            "El equipo seleccionado no existe."
                    ));

            if (equipo.getEstado() != EstadoEquipo.DISPONIBLE) {
                throw new BusinessException(
                        "El equipo no está disponible para préstamo. Estado: "
                                + equipo.getEstado()
                );
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo consultar la disponibilidad del equipo.", e
            );
        }
    }
    private void validarDatosPrestamo(
            Prestamo prestamo,
            List<Integer> idsEquipo
    ) {
        if (prestamo == null) {
            throw new BusinessException(
                    "Debe proporcionar los datos del préstamo."
            );
        }

        if (prestamo.getIdPrestamo() != null) {
            throw new BusinessException(
                    "Un préstamo nuevo no debe tener un identificador asignado."
            );
        }

        if (prestamo.getIdUsuarioSolicitante() <= 0) {
            throw new BusinessException(
                    "Debe seleccionar el usuario solicitante."
            );
        }

        if (prestamo.getIdUsuarioRegistra() <= 0) {
            throw new BusinessException(
                    "Debe identificar al usuario que registra el préstamo."
            );
        }

        if (prestamo.getFechaPrestamo() == null
                || prestamo.getFechaEsperadaDevolucion() == null) {
            throw new BusinessException(
                    "Debe indicar las fechas del préstamo y de devolución."
            );
        }

        if (prestamo.getFechaEsperadaDevolucion()
                .isBefore(prestamo.getFechaPrestamo())) {
            throw new BusinessException(
                    "La devolución esperada no puede ser anterior al préstamo."
            );
        }

        if (prestamo.getObservaciones() != null
                && prestamo.getObservaciones().length() > 250) {
            throw new BusinessException(
                    "Las observaciones no pueden superar los 250 caracteres."
            );
        }

        if (idsEquipo == null || idsEquipo.isEmpty()) {
            throw new BusinessException(
                    "El préstamo debe incluir al menos un equipo."
            );
        }

        Set<Integer> equiposUnicos = new HashSet<>();

        for (Integer idEquipo : idsEquipo) {
            if (idEquipo == null || idEquipo <= 0) {
                throw new BusinessException(
                        "Todos los equipos deben tener un identificador válido."
                );
            }

            if (!equiposUnicos.add(idEquipo)) {
                throw new BusinessException(
                        "El mismo equipo no puede repetirse en el préstamo."
                );
            }
        }
    }
}
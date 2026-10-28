package sigelab.service;
import java.sql.SQLException;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoPrestamo;
import sigelab.model.DetallePrestamo;
import sigelab.repository.PrestamoRepository;
import sigelab.repository.DetallePrestamoRepository;
import java.util.Optional;

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

    private final PrestamoRepository prestamoRepository =
            new PrestamoRepository();

    private final DetallePrestamoRepository detallePrestamoRepository =
            new DetallePrestamoRepository();

    private final AuditService auditService = new AuditService();

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

    public int crearPrestamo(Prestamo prestamo, List<Integer> idsEquipo) {
        validarDatosPrestamo(prestamo, idsEquipo);

        // Trabajamos con una copia y un orden consistente de equipos.
        List<Integer> equiposOrdenados = new ArrayList<>(idsEquipo);
        Collections.sort(equiposOrdenados);

        // El estado inicial lo establece el servicio.
        Prestamo nuevo = new Prestamo(
                prestamo.getIdUsuarioSolicitante(),
                prestamo.getIdUsuarioRegistra(),
                prestamo.getFechaPrestamo(),
                prestamo.getFechaEsperadaDevolucion(),
                EstadoPrestamo.ACTIVO,
                prestamo.getObservaciones()
        );

        int idPrestamo;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);

            try {
                // Revalidamos dentro de la transacción.
                for (Integer idEquipo : equiposOrdenados) {
                    Equipo equipo = equipoRepository
                            .buscarPorId(idEquipo, conn)
                            .orElseThrow(() -> new BusinessException(
                                    "No existe el equipo con ID " + idEquipo + "."
                            ));

                    if (equipo.getEstado() != EstadoEquipo.DISPONIBLE) {
                        throw new BusinessException(
                                "El equipo con ID " + idEquipo
                                        + " no está disponible. Estado: "
                                        + equipo.getEstado()
                        );
                    }
                }

                idPrestamo = prestamoRepository.guardar(conn, nuevo);

                for (Integer idEquipo : equiposOrdenados) {
                    DetallePrestamo detalle =
                            new DetallePrestamo(idPrestamo, idEquipo);

                    detallePrestamoRepository.guardar(conn, detalle);

                    equipoRepository.actualizarEstado(
                            idEquipo,
                            EstadoEquipo.PRESTADO,
                            conn
                    );
                }

                auditService.registrar(
                        nuevo.getIdUsuarioRegistra(),
                        "PRESTAMO_CREADO",
                        "Prestamo",
                        idPrestamo,
                        "Préstamo registrado con "
                                + equiposOrdenados.size() + " equipo(s).",
                        conn
                );

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                if (e instanceof BusinessException businessError) {
                    throw businessError;
                }

                throw new DataAccessException(
                        "No se pudo completar el préstamo. "
                                + "Compruebe su estado antes de volver a intentarlo.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Ocurrió un error de conexión al procesar el préstamo.",
                    e
            );
        }

        // Actualizamos el objeto recibido después de confirmar y cerrar.
        prestamo.setIdPrestamo(idPrestamo);
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        return idPrestamo;
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
    public Optional<Prestamo> buscarPorId(int idPrestamo) {
        if (idPrestamo <= 0) {
            throw new BusinessException(
                    "El identificador del préstamo debe ser mayor que cero."
            );
        }

        return prestamoRepository.buscarPorId(idPrestamo);
    }
    public List<Prestamo> buscarActivos() {
        return prestamoRepository.buscarActivos();
    }
    public List<Prestamo> buscarActivosPorSolicitante(int idSolicitante) {
        if (idSolicitante <= 0) {
            throw new BusinessException(
                    "El identificador del solicitante debe ser mayor que cero."
            );
        }

        return prestamoRepository.buscarActivosPorSolicitante(idSolicitante);
    }
}
package sigelab.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import sigelab.config.DatabaseConfig;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Equipo;
import sigelab.model.Mantenimiento;
import sigelab.repository.EquipoRepository;
import sigelab.repository.MantenimientoRepository;

public class MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;
    private final EquipoRepository equipoRepository;
    private final AuditService auditoriaService;

    @FunctionalInterface
    interface ConexionMantenimiento {
        Connection abrir() throws SQLException;
    }

    private final ConexionMantenimiento conexiones;

    public MantenimientoService() {
        this(new MantenimientoRepository(), new EquipoRepository(), new AuditService());
    }

    public MantenimientoService(MantenimientoRepository mantenimientoRepository,
                                EquipoRepository equipoRepository,
                                AuditService auditoriaService) {
        this(mantenimientoRepository, equipoRepository, auditoriaService, DatabaseConfig::getConnection);
    }

    MantenimientoService(MantenimientoRepository mantenimientoRepository,
            EquipoRepository equipoRepository, AuditService auditoriaService,
            ConexionMantenimiento conexiones) {
        this.conexiones = conexiones;
        this.mantenimientoRepository = mantenimientoRepository;
        this.equipoRepository = equipoRepository;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Registra el ingreso de un equipo a mantenimiento (preventivo o correctivo) aplicando RN-14 y CP-15.
     * Operación transaccional: registra la orden y actualiza el equipo a estado MANTENIMIENTO.
     */
    public Mantenimiento ingresarATaller(int idEquipo, TipoMantenimiento tipo, int idResponsable,
                                        String descripcion, String observaciones) {
        if (idEquipo <= 0) {
            throw new BusinessException("El identificador del equipo no es válido.");
        }
        if (tipo == null) {
            throw new BusinessException("Debe especificar el tipo de mantenimiento (PREVENTIVO o CORRECTIVO).");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new BusinessException("Debe proporcionar una descripción o diagnóstico inicial.");
        }
        validarLongitud(descripcion, 300, "La descripción");
        validarLongitud(observaciones, 250, "Las observaciones");
        if (idResponsable <= 0) {
            throw new BusinessException("Debe especificar el usuario responsable del mantenimiento.");
        }

        return enTransaccion(conn -> {
            auditoriaService.validarAcceso(conn, idResponsable, Modulo.MANTENIMIENTO);

            Optional<Equipo> equipoOpt = equipoRepository.buscarPorId(idEquipo, conn);
            if (equipoOpt.isEmpty()) {
                throw new BusinessException("El equipo especificado no existe en el sistema.");
            }

            Equipo equipo = equipoOpt.get();
            EstadoEquipo estadoActual = equipo.getEstado();

            if (estadoActual != EstadoEquipo.DANADO && estadoActual != EstadoEquipo.DISPONIBLE) {
                throw new BusinessException("El estado del equipo no permite mantenimiento: " + estadoActual);
            }
            if (estadoActual == EstadoEquipo.DISPONIBLE && tipo != TipoMantenimiento.PREVENTIVO) {
                throw new BusinessException("Un equipo disponible solo puede ingresar a mantenimiento preventivo.");
            }
            if (mantenimientoRepository.existeAbierto(idEquipo, conn)) {
                throw new BusinessException("El equipo ya tiene un mantenimiento abierto.");
            }

            Mantenimiento nuevoMantenimiento = new Mantenimiento();
            nuevoMantenimiento.setIdEquipo(idEquipo);
            nuevoMantenimiento.setFechaIngreso(LocalDate.now());
            nuevoMantenimiento.setTipo(tipo);
            nuevoMantenimiento.setDescripcion(descripcion.trim());
            nuevoMantenimiento.setIdResponsable(idResponsable);
            nuevoMantenimiento.setEstado(EstadoMantenimiento.ABIERTO);
            nuevoMantenimiento.setObservaciones(observaciones != null ? observaciones.trim() : "");

            Mantenimiento guardado = mantenimientoRepository.guardar(nuevoMantenimiento, conn);

            // Bloqueo preventivo del equipo en inventario
            equipoRepository.actualizarEstado(idEquipo, EstadoEquipo.MANTENIMIENTO, conn);

            auditoriaService.registrar(
                idResponsable,
                "EQUIPO_ENVIADO_MANTENIMIENTO",
                "Mantenimiento",
                guardado.getIdMantenimiento(),
                "Ingreso a taller (" + tipo + ") del equipo ID " + idEquipo + ": " + descripcion.trim(),
                conn
            );

            return guardado;
        });
    }

    /**
     * Finaliza una orden de mantenimiento aplicando RN-15.
     * Si el resultado es REPARADO -> el equipo pasa a DISPONIBLE (CP-16).
     * Si el resultado es NO_REPARABLE -> el equipo pasa a BAJA (CP-17).
     */
    public void finalizarMantenimiento(int idMantenimiento, ResultadoMantenimiento resultado,
                                       String observacionesFinales, Integer idUsuarioAuditoria) {
        if (idMantenimiento <= 0) {
            throw new BusinessException("El identificador del mantenimiento no es válido.");
        }
        if (resultado == null) {
            throw new BusinessException("Debe especificar el resultado técnico del mantenimiento.");
        }

        if (idUsuarioAuditoria == null || idUsuarioAuditoria <= 0) {
            throw new BusinessException("Debe identificar al usuario que finaliza el mantenimiento.");
        }
        enTransaccion(conn -> {
            auditoriaService.validarAcceso(conn, idUsuarioAuditoria, Modulo.MANTENIMIENTO);

            Optional<Mantenimiento> mantOpt = mantenimientoRepository.buscarPorId(idMantenimiento, conn);
            if (mantOpt.isEmpty()) {
                throw new BusinessException("La orden de mantenimiento especificada no existe.");
            }

            Mantenimiento m = mantOpt.get();
            if (m.getEstado() != EstadoMantenimiento.ABIERTO) {
                throw new BusinessException("La orden de mantenimiento ya fue finalizada previamente.");
            }

            Equipo equipo = equipoRepository.buscarPorId(m.getIdEquipo(), conn)
                    .orElseThrow(() -> new BusinessException("El equipo del mantenimiento no existe."));
            if (equipo.getEstado() != EstadoEquipo.MANTENIMIENTO) {
                throw new BusinessException("El equipo ya no está en mantenimiento. Revise su estado.");
            }
            if (m.getFechaIngreso() == null || m.getFechaIngreso().isAfter(LocalDate.now())) {
                throw new BusinessException("La fecha de ingreso del mantenimiento no es válida para el cierre.");
            }
            m.setFechaSalida(LocalDate.now());
            m.setResultado(resultado);
            m.setEstado(EstadoMantenimiento.CERRADO);

            String obsPrevias = m.getObservaciones() != null ? m.getObservaciones() : "";
            String obsNuevas = observacionesFinales != null ? observacionesFinales.trim() : "";
            String obsCompletas = obsNuevas.isEmpty() ? obsPrevias
                    : obsPrevias.isEmpty() ? obsNuevas : obsPrevias + " | Cierre: " + obsNuevas;
            validarLongitud(obsCompletas, 250, "Las observaciones acumuladas");
            m.setObservaciones(obsCompletas);

            mantenimientoRepository.actualizar(m, conn);

            // Bifurcación técnica según RN-15
            EstadoEquipo nuevoEstadoEquipo;

            if (resultado == ResultadoMantenimiento.REPARADO) {
                nuevoEstadoEquipo = EstadoEquipo.DISPONIBLE; // CP-16
            } else {
                nuevoEstadoEquipo = EstadoEquipo.BAJA; // CP-17
            }

            equipoRepository.actualizarEstado(m.getIdEquipo(), nuevoEstadoEquipo, conn);

            auditoriaService.registrar(
                idUsuarioAuditoria,
                "MANTENIMIENTO_FINALIZADO",
                "Mantenimiento",
                m.getIdMantenimiento(),
                "Cierre de mantenimiento (" + resultado + ") para equipo ID " + m.getIdEquipo() + ". Nuevo estado: " + nuevoEstadoEquipo,
                conn
            );

            return null;
        });
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar(Connection conn) throws SQLException;
    }

    private <T> T enTransaccion(Operacion<T> operacion) {
        try (Connection conn = conexiones.abrir()) {
            // Retiene las lecturas hasta commit: una operación concurrente debe
            // esperar o abortar, nunca sobrescribir un estado validado obsoleto.
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(conn);
                conn.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                if (e instanceof BusinessException negocio) throw negocio;
                throw new DataAccessException("No se pudo completar el mantenimiento. Compruebe su estado antes de reintentar.", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar para procesar el mantenimiento.", e);
        }
    }

    private void validarLongitud(String texto, int maximo, String campo) {
        if (texto != null && texto.trim().length() > maximo) {
            throw new BusinessException(campo + " no puede superar " + maximo + " caracteres.");
        }
    }

    public List<Mantenimiento> listarMantenimientosActivos() {
        try {
            return mantenimientoRepository.listarPorEstado(EstadoMantenimiento.ABIERTO);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los mantenimientos activos.", e);
        }
    }

    public List<Mantenimiento> listarHistorialPorEquipo(int idEquipo) {
        try {
            return mantenimientoRepository.listarPorEquipo(idEquipo);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el historial del equipo.", e);
        }
    }

    public List<Mantenimiento> listarTodos() {
        try {
            return mantenimientoRepository.listarTodos();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los mantenimientos.", e);
        }
    }
}

package sigelab.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import sigelab.config.DatabaseConfig;
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

    public MantenimientoService() {
        this(new MantenimientoRepository(), new EquipoRepository(), new AuditService());
    }

    public MantenimientoService(MantenimientoRepository mantenimientoRepository,
                                EquipoRepository equipoRepository,
                                AuditService auditoriaService) {
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
            throw new IllegalArgumentException("El identificador del equipo no es válido.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Debe especificar el tipo de mantenimiento (PREVENTIVO o CORRECTIVO).");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar una descripción o diagnóstico inicial.");
        }
        if (idResponsable <= 0) {
            throw new IllegalArgumentException("Debe especificar el usuario responsable del mantenimiento.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            Optional<Equipo> equipoOpt = equipoRepository.buscarPorId(idEquipo, conn);
            if (equipoOpt.isEmpty()) {
                throw new IllegalArgumentException("El equipo especificado no existe en el sistema.");
            }

            Equipo equipo = equipoOpt.get();
            EstadoEquipo estadoActual = equipo.getEstado();

            // RN-14: Solo equipos DISPONIBLE o DANADO pueden ingresar a mantenimiento
            if (estadoActual == EstadoEquipo.PRESTADO) {
                throw new IllegalStateException("El equipo está actualmente en préstamo y no puede enviarse a taller.");
            }
            if (estadoActual == EstadoEquipo.MANTENIMIENTO) {
                throw new IllegalStateException("El equipo ya se encuentra actualmente en mantenimiento.");
            }
            if (estadoActual == EstadoEquipo.BAJA) {
                throw new IllegalStateException("El equipo ha sido dado de baja y no puede ingresar a mantenimiento.");
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
                "MANTENIMIENTO_INICIO",
                "Mantenimiento",
                guardado.getIdMantenimiento(),
                "Ingreso a taller (" + tipo + ") del equipo ID " + idEquipo + ": " + descripcion,
                conn
            );

            conn.commit();
            return guardado;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[ERROR-ROLLBACK] Error al revertir transaccion de mantenimiento: " + rollbackEx.getMessage());
                }
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error en la transacción al ingresar equipo a mantenimiento: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("[ERROR-CONEXION] Error al cerrar conexion: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Finaliza una orden de mantenimiento aplicando RN-15.
     * Si el resultado es REPARADO -> el equipo pasa a DISPONIBLE (CP-16).
     * Si el resultado es NO_REPARABLE -> el equipo pasa a BAJA (CP-17).
     */
    public void finalizarMantenimiento(int idMantenimiento, ResultadoMantenimiento resultado,
                                       String observacionesFinales, Integer idUsuarioAuditoria) {
        if (idMantenimiento <= 0) {
            throw new IllegalArgumentException("El identificador del mantenimiento no es válido.");
        }
        if (resultado == null) {
            throw new IllegalArgumentException("Debe especificar el resultado técnico del mantenimiento.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            Optional<Mantenimiento> mantOpt = mantenimientoRepository.buscarPorId(idMantenimiento, conn);
            if (mantOpt.isEmpty()) {
                throw new IllegalArgumentException("La orden de mantenimiento especificada no existe.");
            }

            Mantenimiento m = mantOpt.get();
            if (m.getEstado() == EstadoMantenimiento.CERRADO) {
                throw new IllegalStateException("La orden de mantenimiento ya fue finalizada previamente.");
            }

            m.setFechaSalida(LocalDate.now());
            m.setResultado(resultado);
            m.setEstado(EstadoMantenimiento.CERRADO);

            String obsPrevias = m.getObservaciones() != null ? m.getObservaciones() : "";
            String obsNuevas = observacionesFinales != null ? observacionesFinales.trim() : "";
            String obsCompletas = obsPrevias.isEmpty() ? obsNuevas : obsPrevias + " | Cierre: " + obsNuevas;
            m.setObservaciones(obsCompletas);

            mantenimientoRepository.actualizar(m, conn);

            // Bifurcación técnica según RN-15
            EstadoEquipo nuevoEstadoEquipo;
            String accionAuditoria;

            if (resultado == ResultadoMantenimiento.REPARADO) {
                nuevoEstadoEquipo = EstadoEquipo.DISPONIBLE; // CP-16
                accionAuditoria = "MANTENIMIENTO_REPARADO";
            } else {
                nuevoEstadoEquipo = EstadoEquipo.BAJA; // CP-17
                accionAuditoria = "EQUIPO_BAJA";
            }

            equipoRepository.actualizarEstado(m.getIdEquipo(), nuevoEstadoEquipo, conn);

            auditoriaService.registrar(
                idUsuarioAuditoria != null ? idUsuarioAuditoria : m.getIdResponsable(),
                accionAuditoria,
                "Mantenimiento",
                m.getIdMantenimiento(),
                "Cierre de mantenimiento (" + resultado + ") para equipo ID " + m.getIdEquipo() + ". Nuevo estado: " + nuevoEstadoEquipo,
                conn
            );

            conn.commit();

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[ERROR-ROLLBACK] Error al revertir transaccion de cierre: " + rollbackEx.getMessage());
                }
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error en la transacción al finalizar mantenimiento: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("[ERROR-CONEXION] Error al cerrar conexion: " + closeEx.getMessage());
                }
            }
        }
    }

    public List<Mantenimiento> listarMantenimientosActivos() {
        try {
            return mantenimientoRepository.listarPorEstado(EstadoMantenimiento.ABIERTO);
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar mantenimientos activos: " + e.getMessage(), e);
        }
    }

    public List<Mantenimiento> listarHistorialPorEquipo(int idEquipo) {
        try {
            return mantenimientoRepository.listarPorEquipo(idEquipo);
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar historial de equipo: " + e.getMessage(), e);
        }
    }

    public List<Mantenimiento> listarTodos() {
        try {
            return mantenimientoRepository.listarTodos();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar todos los mantenimientos: " + e.getMessage(), e);
        }
    }
}

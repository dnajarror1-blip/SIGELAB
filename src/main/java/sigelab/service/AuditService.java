package sigelab.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.config.DatabaseConfig;
import java.util.List;

import sigelab.model.Auditoria;
import sigelab.repository.AuditoriaRepository;

public class AuditService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditService() {
        this(new AuditoriaRepository());
    }

    public AuditService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void validarAcceso(Connection conn, int idUsuario, Modulo modulo) throws SQLException {
        var rol = auditoriaRepository.buscarRolUsuarioActivo(conn, idUsuario)
                .orElseThrow(() -> new BusinessException("Debe iniciar sesión con un usuario activo."));
        if (!new PermisoService().modulosDe(rol).contains(modulo)) {
            throw new BusinessException("No tiene permiso para acceder a " + modulo + ".");
        }
    }

    public void validarConsulta(Integer idUsuario, Modulo modulo) {
        if (idUsuario == null || idUsuario <= 0) throw new BusinessException("Debe iniciar sesión.");
        try (Connection conn = DatabaseConfig.getConnection()) {
            validarAcceso(conn, idUsuario, modulo);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo verificar el permiso de consulta.", e);
        }
    }

    /**
     * Registra eventos de auditoría no críticos.
     *
     * Si ocurre un error de persistencia, se muestra una advertencia
     * sin interrumpir la operación que solicitó el registro.
     */
    public void registrar(
            Integer idUsuario,
            String accion,
            String entidad,
            Integer idRegistro,
            String descripcion
    ) {
        try {
            Auditoria auditoria = new Auditoria(
                    idUsuario,
                    LocalDateTime.now(),
                    accion,
                    entidad,
                    idRegistro,
                    descripcion
            );

            auditoriaRepository.guardar(auditoria);

        } catch (Exception e) {
            System.err.println("[ADVERTENCIA-AUDITORIA] No se pudo persistir el evento de auditoría.");
        }
    }

    /** Auditoría obligatoria: el repositorio administra y cierra su propia conexión. */
    public void registrarObligatorio(Integer idUsuario, String accion, String entidad,
                                     Integer idRegistro, String descripcion) {
        try {
            auditoriaRepository.guardar(new Auditoria(idUsuario, LocalDateTime.now(),
                    accion, entidad, idRegistro, descripcion));
        } catch (SQLException | DataAccessException | sigelab.exception.DatabaseConfigurationException error) {
            throw new DataAccessException("No se pudo registrar la auditoría obligatoria. Acceso denegado.", error);
        }
    }

    /**
     * Registra un evento de auditoría dentro de una transacción existente.
     *
     * Utiliza la conexión proporcionada por el servicio que controla
     * la transacción.
     *
     * Si falla el registro, propaga una excepción para que el servicio
     * principal pueda ejecutar rollback().
     */
    public void registrar(
            Integer idUsuario,
            String accion,
            String entidad,
            Integer idRegistro,
            String descripcion,
            Connection conn
    ) {
        if (conn == null) {
            throw new IllegalArgumentException(
                    "La conexión transaccional de auditoría no puede ser nula."
            );
        }

        Auditoria auditoria = new Auditoria(
                idUsuario,
                LocalDateTime.now(),
                accion,
                entidad,
                idRegistro,
                descripcion
        );

        try {
            auditoriaRepository.guardar(auditoria, conn);

        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo registrar la auditoría de la transacción.",
                    e
            );
        }
    }

    /**
     * Obtiene todos los registros de auditoría.
     */
    public List<Auditoria> obtenerHistorial() {
        try {
            return auditoriaRepository.listarTodos();

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar la auditoría.", e);
        }
    }

    /**
     * Permite consultar registros de auditoría utilizando filtros.
     */
    public List<Auditoria> filtrarHistorial(
            String accion,
            String entidad,
            Integer idUsuario
    ) {
        try {
            return auditoriaRepository.buscarPorFiltros(
                    accion,
                    entidad,
                    idUsuario
            );

        } catch (SQLException e) {
            throw new DataAccessException("No se pudo filtrar la auditoría.", e);
        }
    }
}
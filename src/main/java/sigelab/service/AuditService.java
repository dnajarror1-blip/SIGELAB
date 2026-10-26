package sigelab.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
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
            System.err.println(
                    "[ADVERTENCIA-AUDITORIA] No se pudo persistir el log de auditoria: "
                            + e.getMessage()
            );
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
            throw new IllegalStateException(
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

        } catch (Exception e) {
            System.err.println(
                    "[ERROR-AUDITORIA] Error al consultar historial de auditoria: "
                            + e.getMessage()
            );

            return Collections.emptyList();
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

        } catch (Exception e) {
            System.err.println(
                    "[ERROR-AUDITORIA] Error al filtrar historial de auditoria: "
                            + e.getMessage()
            );

            return Collections.emptyList();
        }
    }
}
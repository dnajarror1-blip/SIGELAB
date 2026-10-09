package sigelab.service;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import sigelab.model.Auditoria;
import sigelab.repository.AuditoriaRepository;

public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService() {
        this(new AuditoriaRepository());
    }

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /**
     * Registra un evento de auditoría de forma segura y no bloqueante (RN-18).
     * Si ocurre un error de persistencia, se captura para no comprometer la transacción principal.
     */
    public void registrar(Integer idUsuario, String accion, String entidad, Integer idRegistro, String descripcion) {
        try {
            Auditoria auditoria = new Auditoria(idUsuario, LocalDateTime.now(), accion, entidad, idRegistro, descripcion);
            auditoriaRepository.guardar(auditoria);
        } catch (Exception e) {
            System.err.println("[ADVERTENCIA-AUDITORIA] No se pudo persistir el log de auditoria: " + e.getMessage());
        }
    }

    /**
     * Registra un evento de auditoría dentro de una conexión transaccional abierta.
     */
    public void registrar(Integer idUsuario, String accion, String entidad, Integer idRegistro, String descripcion, Connection conn) {
        try {
            Auditoria auditoria = new Auditoria(idUsuario, LocalDateTime.now(), accion, entidad, idRegistro, descripcion);
            auditoriaRepository.guardar(auditoria, conn);
        } catch (Exception e) {
            System.err.println("[ADVERTENCIA-AUDITORIA] No se pudo persistir el log transaccional de auditoria: " + e.getMessage());
        }
    }

    public List<Auditoria> obtenerHistorial() {
        try {
            return auditoriaRepository.listarTodos();
        } catch (Exception e) {
            System.err.println("[ERROR-AUDITORIA] Error al consultar historial de auditoria: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Auditoria> filtrarHistorial(String accion, String entidad, Integer idUsuario) {
        try {
            return auditoriaRepository.buscarPorFiltros(accion, entidad, idUsuario);
        } catch (Exception e) {
            System.err.println("[ERROR-AUDITORIA] Error al filtrar historial de auditoria: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}

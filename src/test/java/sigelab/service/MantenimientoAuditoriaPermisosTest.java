package sigelab.service;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import sigelab.exception.BusinessException;
import sigelab.model.Rol;
import sigelab.repository.AuditoriaRepository;
import static org.junit.jupiter.api.Assertions.*;

class MantenimientoAuditoriaPermisosTest {
    private final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{Connection.class}, (p,m,a) -> { throw new AssertionError("No usar BD"); });
    private AuditService servicio(String rol) {
        return new AuditService(new AuditoriaRepository() {
            public Optional<Rol> buscarRolUsuarioActivo(Connection c, int id) {
                assertSame(conn,c); assertEquals(42,id);
                Rol r = new Rol(rol); r.setIdRol(987); return Optional.of(r);
            }
        });
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","ENCARGADO"})
    void mantenimientoPermitidoPorNombreDeRol(String rol) throws Exception {
        servicio(rol).validarAcceso(conn,42,Modulo.MANTENIMIENTO);
    }
    @ParameterizedTest @ValueSource(strings={"ESTUDIANTE","DESCONOCIDO"})
    void mantenimientoDenegado(String rol) {
        assertThrows(BusinessException.class, () -> servicio(rol).validarAcceso(conn,42,Modulo.MANTENIMIENTO));
    }
    @ParameterizedTest @ValueSource(strings={"ENCARGADO","ESTUDIANTE","DESCONOCIDO"})
    void auditoriaSoloAdmin(String rol) {
        assertThrows(BusinessException.class, () -> servicio(rol).validarAcceso(conn,42,Modulo.AUDITORIA));
    }
    @Test void permiteAdminAuditoria() throws Exception { servicio("ADMIN").validarAcceso(conn,42,Modulo.AUDITORIA); }
    @Test void usuarioInactivoNoTieneAcceso() {
        AuditService s = new AuditService(new AuditoriaRepository() {
            public Optional<Rol> buscarRolUsuarioActivo(Connection c, int id) { return Optional.empty(); }
        });
        assertThrows(BusinessException.class, () -> s.validarAcceso(conn,42,Modulo.MANTENIMIENTO));
    }
    @Test void consultaSinSesionFallaAntesDeAbrirConexion() {
        assertThrows(BusinessException.class, () -> servicio("ADMIN").validarConsulta(null,Modulo.AUDITORIA));
    }
}

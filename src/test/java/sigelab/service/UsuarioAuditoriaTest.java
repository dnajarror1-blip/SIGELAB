package sigelab.service;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.EstadoUsuario;
import sigelab.exception.*;
import sigelab.model.Usuario;
import sigelab.repository.*;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioAuditoriaTest {
    private final Usuario usuario = new Usuario();
    private final List<String> acciones = new ArrayList<>();
    private boolean encontrado = true, falloAudit, commit, rollback, cerrado;
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;
    private final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
            (p,m,a) -> {
                switch (m.getName()) {
                    case "setAutoCommit" -> assertEquals(false,a[0]);
                    case "commit" -> commit = true;
                    case "rollback" -> { rollback = true; estado = EstadoUsuario.ACTIVO; acciones.clear(); }
                    case "close" -> cerrado = true;
                    default -> throw new AssertionError(m.getName());
                }
                return null;
            });
    private final UsuarioService service = new UsuarioService(new UsuarioRepository() {
        public Optional<Usuario> buscarPorUsername(String nombre) { return encontrado ? Optional.of(usuario) : Optional.empty(); }
        public boolean cambiarEstado(int id, EstadoUsuario nuevo, Connection c) {
            assertSame(conn,c); assertEquals(7,id); estado = nuevo; return encontrado;
        }
    }, new RolRepository(), new AuditService() {
        public void validarAcceso(Connection c, int actor, Modulo modulo) {
            assertSame(conn,c); assertEquals(42,actor); assertEquals(Modulo.USUARIOS,modulo);
        }
        public void registrar(Integer actor, String accion, String entidad, Integer id, String descripcion) {
            assertEquals("Usuario", entidad); assertFalse(descripcion.contains("clave123"));
            assertFalse(descripcion.contains("$2a$")); acciones.add(accion);
        }
        public void registrar(Integer actor, String accion, String entidad, Integer id, String descripcion, Connection c) {
            assertSame(conn,c); assertEquals(42,actor); assertEquals(7,id);
            registrar(actor,accion,entidad,id,descripcion);
            if (falloAudit) throw new DataAccessException("Fallo simulado");
        }
    }, () -> conn);

    UsuarioAuditoriaTest() {
        usuario.setIdUsuario(7); usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setPasswordHash(BCrypt.hashpw("clave123",BCrypt.gensalt(4)));
    }
    @Test void credencialesValidasNoRegistranLoginExitoso() {
        assertSame(usuario, service.autenticar("ana","clave123")); assertTrue(acciones.isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"inexistente","password","inactivo","vacio"})
    void loginInvalidoRegistraFallo(String caso) {
        encontrado = !caso.equals("inexistente");
        if (caso.equals("inactivo")) usuario.setEstado(EstadoUsuario.INACTIVO);
        assertThrows(BusinessException.class, () -> service.autenticar(caso.equals("vacio") ? "" : "ana",
                caso.equals("password") ? "incorrecta" : "clave123"));
        assertEquals(List.of("LOGIN_FALLIDO"),acciones);
    }
    @Test void desactivarEsAtomicoConActorYReferencia() {
        service.cambiarEstado(7,EstadoUsuario.INACTIVO,42);
        assertTrue(commit); assertFalse(rollback); assertTrue(cerrado); assertEquals(EstadoUsuario.INACTIVO,estado);
        assertEquals(List.of("USUARIO_DESACTIVADO"),acciones);
    }
    @Test void falloAuditoriaRevierteDesactivacion() {
        falloAudit = true; assertThrows(DataAccessException.class, () -> service.cambiarEstado(7,EstadoUsuario.INACTIVO,42));
        assertFalse(commit); assertTrue(rollback); assertTrue(cerrado); assertEquals(EstadoUsuario.ACTIVO,estado);
        assertTrue(acciones.isEmpty());
    }
}

package sigelab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import sigelab.enums.*;
import sigelab.exception.*;
import sigelab.model.*;
import sigelab.repository.*;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MantenimientoTransaccionTest {
    private EstadoEquipo estado = EstadoEquipo.DANADO, anterior;
    private Mantenimiento mantenimiento, previo;
    private List<String> eventos = new ArrayList<>(), anteriores;
    private String falla = "";
    private boolean duplicado, commit, rollback, cerrado;
    private final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{Connection.class}, (p, m, a) -> {
                switch (m.getName()) {
                    case "setTransactionIsolation" -> assertEquals(Connection.TRANSACTION_SERIALIZABLE, a[0]);
                    case "setAutoCommit" -> {
                        assertEquals(false, a[0]);
                        anterior = estado; previo = copia(mantenimiento); anteriores = new ArrayList<>(eventos);
                    }
                    case "commit" -> { fallar("commit"); commit = true; }
                    case "rollback" -> {
                        rollback = true; estado = anterior; mantenimiento = previo; eventos = anteriores;
                        fallar("rollback");
                    }
                    case "close" -> cerrado = true;
                    default -> throw new AssertionError("Llamada JDBC inesperada: " + m.getName());
                }
                return null;
            });
    private final AuditService auditoria = new AuditService() {
        public void validarAcceso(Connection c, int usuario, Modulo modulo) {
            assertSame(conn, c); assertEquals(42, usuario); assertEquals(Modulo.MANTENIMIENTO, modulo);
            if (falla.equals("permiso")) throw new BusinessException("Sin permiso");
        }
        public void registrar(Integer usuario, String accion, String entidad, Integer id, String descripcion, Connection c) {
            assertSame(conn, c); assertEquals(42, usuario); assertEquals("Mantenimiento", entidad);
            assertEquals(10, id); assertTrue(descripcion.contains("7"));
            eventos.add(accion);
            if (falla.equals("auditoria")) throw new DataAccessException("Fallo simulado");
        }
    };
    private final MantenimientoService service = new MantenimientoService(new MantenimientoRepository() {
        public boolean existeAbierto(int id, Connection c) { assertSame(conn, c); return duplicado; }
        public Mantenimiento guardar(Mantenimiento m, Connection c) throws SQLException {
            assertSame(conn, c); m.setIdMantenimiento(10); mantenimiento = copia(m); fallar("guardar"); return m;
        }
        public Optional<Mantenimiento> buscarPorId(int id, Connection c) {
            assertSame(conn, c); return Optional.ofNullable(copia(mantenimiento));
        }
        public void actualizar(Mantenimiento m, Connection c) throws SQLException {
            assertSame(conn, c); mantenimiento = copia(m); fallar("actualizar");
        }
    }, new EquipoRepository() {
        public Optional<Equipo> buscarPorId(int id, Connection c) {
            assertSame(conn, c); assertEquals(7, id);
            Equipo e = new Equipo(); e.setIdEquipo(id); e.setEstado(estado); return Optional.of(e);
        }
        public void actualizarEstado(int id, EstadoEquipo nuevo, Connection c) throws SQLException {
            assertSame(conn, c); assertEquals(7, id); estado = nuevo; fallar("equipo");
        }
    }, auditoria, () -> conn);

    private void fallar(String paso) throws SQLException {
        if (falla.equals(paso)) throw new SQLException("Detalle SQL privado");
    }
    private Mantenimiento copia(Mantenimiento m) {
        if (m == null) return null;
        Mantenimiento n = new Mantenimiento(m.getIdEquipo(), m.getFechaIngreso(), m.getTipo(),
                m.getDescripcion(), m.getIdResponsable(), m.getFechaSalida(), m.getResultado(),
                m.getEstado(), m.getObservaciones());
        n.setIdMantenimiento(m.getIdMantenimiento()); return n;
    }
    private void abrir() { service.ingresarATaller(7, TipoMantenimiento.CORRECTIVO, 42, "Diagnóstico", ""); }
    private void cerrar(ResultadoMantenimiento resultado) { service.finalizarMantenimiento(10, resultado, "Listo", 42); }
    private void ordenAbierta() {
        estado = EstadoEquipo.MANTENIMIENTO;
        mantenimiento = new Mantenimiento(7, LocalDate.now(), TipoMantenimiento.CORRECTIVO,
                "Diagnóstico", 42, null, null, EstadoMantenimiento.ABIERTO, "");
        mantenimiento.setIdMantenimiento(10);
    }
    @Test void abreDanadoConAuditoriaYEstado() {
        abrir(); assertTrue(commit); assertFalse(rollback); assertTrue(cerrado);
        assertEquals(EstadoEquipo.MANTENIMIENTO, estado);
        assertEquals(EstadoMantenimiento.ABIERTO, mantenimiento.getEstado());
        assertNull(mantenimiento.getFechaSalida()); assertNull(mantenimiento.getResultado());
        assertEquals(List.of("EQUIPO_ENVIADO_MANTENIMIENTO"), eventos);
    }
    @Test void permitePreventivoDisponible() {
        estado = EstadoEquipo.DISPONIBLE;
        service.ingresarATaller(7, TipoMantenimiento.PREVENTIVO, 42, "Limpieza", "");
        assertTrue(commit); assertEquals(EstadoEquipo.MANTENIMIENTO, estado);
    }
    @ParameterizedTest @EnumSource(value=EstadoEquipo.class, names={"PRESTADO","MANTENIMIENTO","BAJA"})
    void rechazaEstadosInvalidos(EstadoEquipo valor) {
        estado = valor; assertThrows(BusinessException.class, this::abrir);
        assertTrue(rollback); assertFalse(commit); assertNull(mantenimiento); assertEquals(valor, estado);
    }
    @Test void rechazaCorrectivoDisponible() {
        estado = EstadoEquipo.DISPONIBLE; assertThrows(BusinessException.class, this::abrir); assertTrue(rollback);
    }
    @Test void rechazaSegundoAbiertoAunqueElEstadoSeaInconsistente() {
        duplicado = true; assertThrows(BusinessException.class, this::abrir);
        assertTrue(rollback); assertFalse(commit); assertNull(mantenimiento);
    }
    @ParameterizedTest @EnumSource(ResultadoMantenimiento.class)
    void cierraConResultadoYAuditoria(ResultadoMantenimiento resultado) {
        ordenAbierta(); cerrar(resultado);
        assertTrue(commit); assertFalse(rollback); assertTrue(cerrado);
        assertEquals(resultado == ResultadoMantenimiento.REPARADO ? EstadoEquipo.DISPONIBLE : EstadoEquipo.BAJA, estado);
        assertEquals(EstadoMantenimiento.CERRADO, mantenimiento.getEstado());
        assertEquals(resultado, mantenimiento.getResultado()); assertEquals(LocalDate.now(), mantenimiento.getFechaSalida());
        assertEquals(List.of("MANTENIMIENTO_FINALIZADO"), eventos);
    }
    @Test void noCierraDosVeces() {
        ordenAbierta(); cerrar(ResultadoMantenimiento.REPARADO); commit = false;
        assertThrows(BusinessException.class, () -> cerrar(ResultadoMantenimiento.NO_REPARABLE));
        assertFalse(commit); assertTrue(rollback); assertEquals(EstadoEquipo.DISPONIBLE, estado);
        assertEquals(1, eventos.size());
    }
    @ParameterizedTest @ValueSource(strings={"guardar","equipo","auditoria","commit"})
    void aperturaRevierteTodosLosCambios(String paso) {
        falla = paso; DataAccessException ex = assertThrows(DataAccessException.class, this::abrir);
        assertFalse(ex.getMessage().contains("privado")); assertFalse(commit); assertTrue(rollback); assertTrue(cerrado);
        assertEquals(EstadoEquipo.DANADO, estado); assertNull(mantenimiento); assertTrue(eventos.isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"actualizar","equipo","auditoria","commit"})
    void cierreRevierteTodosLosCambios(String paso) {
        ordenAbierta(); falla = paso; assertThrows(DataAccessException.class, () -> cerrar(ResultadoMantenimiento.REPARADO));
        assertFalse(commit); assertTrue(rollback); assertTrue(cerrado);
        assertEquals(EstadoEquipo.MANTENIMIENTO, estado); assertEquals(EstadoMantenimiento.ABIERTO, mantenimiento.getEstado());
        assertNull(mantenimiento.getResultado()); assertNull(mantenimiento.getFechaSalida()); assertTrue(eventos.isEmpty());
    }
    @Test void noSobrescribeEquipoQueYaNoEstaEnMantenimiento() {
        ordenAbierta(); estado = EstadoEquipo.PRESTADO;
        assertThrows(BusinessException.class, () -> cerrar(ResultadoMantenimiento.REPARADO));
        assertTrue(rollback); assertEquals(EstadoEquipo.PRESTADO, estado);
    }
    @Test void rechazaActorSinPermisoAntesDeEscribir() {
        falla = "permiso"; assertThrows(BusinessException.class, this::abrir);
        assertTrue(rollback); assertNull(mantenimiento); assertTrue(eventos.isEmpty());
    }
    @Test void validaLimitesSql() {
        assertThrows(BusinessException.class, () -> service.ingresarATaller(7, TipoMantenimiento.CORRECTIVO, 42, "x".repeat(301), ""));
        assertThrows(BusinessException.class, () -> service.ingresarATaller(7, TipoMantenimiento.CORRECTIVO, 42, "OK", "x".repeat(251)));
        ordenAbierta(); mantenimiento.setObservaciones("x".repeat(250));
        assertThrows(BusinessException.class, () -> cerrar(ResultadoMantenimiento.REPARADO)); assertTrue(rollback);
    }
}

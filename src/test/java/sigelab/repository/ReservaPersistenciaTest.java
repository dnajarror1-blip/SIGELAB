package sigelab.repository;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import sigelab.enums.EstadoReserva;
import sigelab.model.Reserva;
import static org.junit.jupiter.api.Assertions.*;

class ReservaPersistenciaTest {
    private boolean hayFila = true, stmtCerrado, rsCerrado;
    private int filas = 1;
    private String sql;
    private final Map<Integer,Object> parametros = new HashMap<>();
    private final ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},
            (p,m,a) -> switch(m.getName()) {
                case "next" -> hayFila;
                case "getInt" -> a[0].equals("id_usuario") ? 10 : a[0].equals("id_equipo") ? 7 : 71;
                case "getString" -> a[0].equals("estado") ? "COMPLETADA" : "Observaciones";
                case "getDate" -> java.sql.Date.valueOf("2026-11-01");
                case "getTimestamp" -> Timestamp.valueOf("2026-10-30 12:00:00");
                case "close" -> { rsCerrado = true; yield null; }
                default -> throw new AssertionError(m.getName());
            });
    private final PreparedStatement stmt = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},
            (p,m,a) -> switch(m.getName()) {
                case "setInt","setDate","setString","setNString","setTimestamp" -> { parametros.put((int)a[0],a[1]); yield null; }
                case "executeQuery","getGeneratedKeys" -> rs;
                case "executeUpdate" -> filas;
                case "close" -> { stmtCerrado = true; yield null; }
                default -> throw new AssertionError(m.getName());
            });
    private final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},
            (p,m,a) -> { assertEquals("prepareStatement",m.getName()); sql = (String)a[0]; return stmt; });
    private Reserva reserva() {
        return new Reserva(10,7,LocalDate.of(2026,11,1),LocalDate.of(2026,11,3),EstadoReserva.ACTIVA,
                LocalDateTime.of(2026,10,30,12,0),"Observación");
    }
    @Test void insertaReservaConClaveEstadoYFechaDeCreacion() throws Exception {
        assertEquals(71,new ReservaRepository().guardar(conn,reserva()));
        assertEquals("ACTIVA",parametros.get(5)); assertEquals(Timestamp.valueOf("2026-10-30 12:00:00"),parametros.get(6));
        assertTrue(stmtCerrado); assertTrue(rsCerrado);
    }
    @Test void noAceptaInsercionSinClave() {
        hayFila = false; assertThrows(SQLException.class, () -> new ReservaRepository().guardar(conn,reserva()));
        assertTrue(stmtCerrado); assertTrue(rsCerrado);
    }
    @Test void noAceptaInsercionSinFila() {
        filas = 0; assertThrows(SQLException.class, () -> new ReservaRepository().guardar(conn,reserva())); assertTrue(stmtCerrado);
    }
    @Test void consultaSolapamientoActivoConLimitesInclusivosYParametrosCorrectos() throws Exception {
        assertTrue(new ReservaRepository().existeSolapamiento(conn,7,reserva().getFechaInicio(),reserva().getFechaFin()));
        assertTrue(sql.contains("estado = 'ACTIVA'")); assertTrue(sql.contains("fecha_inicio <= ? AND fecha_fin >= ?"));
        assertEquals(java.sql.Date.valueOf("2026-11-03"),parametros.get(2));
        assertEquals(java.sql.Date.valueOf("2026-11-01"),parametros.get(3));
    }
    @Test void consultaSinSolapamientoDevuelveFalse() throws Exception {
        hayFila = false;
        assertFalse(new ReservaRepository().existeSolapamiento(conn,7,reserva().getFechaInicio(),reserva().getFechaFin()));
    }
    @Test void cancelarVerificaEstadoYFilasAfectadas() {
        filas = 0; assertThrows(SQLException.class, () -> new ReservaRepository().cancelar(conn,71));
        assertTrue(sql.contains("estado = 'CANCELADA'")); assertTrue(sql.contains("AND estado = 'ACTIVA'")); assertTrue(stmtCerrado);
    }
    @Test void buscarParaCancelarRetieneBloqueoYConservaEstadoCompletada() throws Exception {
        Reserva r = new ReservaRepository().buscarParaCancelar(conn,71).orElseThrow();
        assertEquals(EstadoReserva.COMPLETADA,r.getEstado()); assertEquals(71,r.getIdReserva());
        assertTrue(sql.contains("UPDLOCK, HOLDLOCK")); assertTrue(stmtCerrado); assertTrue(rsCerrado);
    }
    @Test void solicitanteSeConsultaActivoSinCargarCredenciales() throws Exception {
        assertTrue(new ReservaRepository().existeSolicitanteActivo(conn,10));
        assertTrue(sql.contains("estado = 'ACTIVO'")); assertFalse(sql.contains("password")); assertEquals(10,parametros.get(1));
    }
}

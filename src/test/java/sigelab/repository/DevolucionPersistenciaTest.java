package sigelab.repository;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import sigelab.enums.CondicionDevolucion;
import sigelab.model.Devolucion;
import static org.junit.jupiter.api.Assertions.*;

class DevolucionPersistenciaTest {
    private int filas = 1;
    private boolean clave = true, stmtCerrado, rsCerrado;
    private String sql;
    private final Map<Integer,Object> parametros = new HashMap<>();
    private final ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},
            (p,m,a) -> switch(m.getName()) {
                case "next" -> clave;
                case "getInt" -> 73;
                case "close" -> { rsCerrado = true; yield null; }
                default -> throw new AssertionError(m.getName());
            });
    private final PreparedStatement stmt = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},
            (p,m,a) -> switch(m.getName()) {
                case "setInt","setDate","setString","setNString" -> { parametros.put((int)a[0],a[1]); yield null; }
                case "executeUpdate" -> filas;
                case "getGeneratedKeys","executeQuery" -> rs;
                case "close" -> { stmtCerrado = true; yield null; }
                default -> throw new AssertionError(m.getName());
            });
    private final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},
            (p,m,a) -> { assertEquals("prepareStatement",m.getName()); sql = (String)a[0]; return stmt; });
    private Devolucion devolucion() {
        return new Devolucion(101,42,LocalDate.of(2026,10,30),CondicionDevolucion.DANADO,"Daño");
    }
    @Test void insertaCondicionCanonicaYFechaConParametrosYCierraRecursos() throws Exception {
        assertEquals(73,new DevolucionRepository().guardar(conn,devolucion()));
        assertEquals(101,parametros.get(1)); assertEquals(42,parametros.get(2));
        assertEquals(java.sql.Date.valueOf("2026-10-30"),parametros.get(3)); assertEquals("DANADO",parametros.get(4));
        assertTrue(stmtCerrado); assertTrue(rsCerrado);
    }
    @Test void insercionSinClaveDebeFallar() {
        clave = false; assertThrows(SQLException.class, () -> new DevolucionRepository().guardar(conn,devolucion()));
        assertTrue(stmtCerrado); assertTrue(rsCerrado);
    }
    @Test void insercionSinFilaDebeFallar() {
        filas = 0; assertThrows(SQLException.class, () -> new DevolucionRepository().guardar(conn,devolucion()));
        assertTrue(stmtCerrado);
    }
    @Test void cierreCompruebaEstadoPendientesYFilasAfectadas() {
        filas = 0; assertThrows(SQLException.class, () -> new PrestamoRepository().finalizarSinPendientes(conn,41));
        assertTrue(sql.contains("p.estado = 'ACTIVO'")); assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("dbo.Devolucion")); assertTrue(stmtCerrado);
    }
    @Test void lecturaDeEncabezadoBloqueaHastaFinDeTransaccion() throws Exception {
        clave = false; assertTrue(new PrestamoRepository().buscarParaDevolucion(conn,41).isEmpty());
        assertTrue(sql.contains("UPDLOCK, HOLDLOCK")); assertTrue(stmtCerrado); assertTrue(rsCerrado);
    }
    @Test void pendientesSeDeterminanPorAusenciaDeDevolucion() throws Exception {
        clave = false; assertTrue(new DetallePrestamoRepository().buscarPendientes(conn,41).isEmpty());
        assertTrue(sql.contains("NOT EXISTS")); assertTrue(sql.contains("dbo.Devolucion")); assertEquals(41,parametros.get(1));
    }
}

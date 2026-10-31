package sigelab.repository;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sigelab.enums.*;
import sigelab.model.Mantenimiento;
import static org.junit.jupiter.api.Assertions.*;

class MantenimientoPersistenciaTest {
    private int filas = 1;
    private boolean clave;
    private String sql;
    private boolean statementCerrado, resultSetCerrado;
    private final ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
            (p,m,a) -> switch (m.getName()) {
                case "next" -> clave;
                case "getInt" -> 10;
                case "close" -> { resultSetCerrado = true; yield null; }
                default -> throw new AssertionError(m.getName());
            });
    private final PreparedStatement stmt = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
            (p,m,a) -> switch (m.getName()) {
                case "executeUpdate" -> filas;
                case "getGeneratedKeys" -> rs;
                case "close" -> { statementCerrado = true; yield null; }
                case "setInt", "setDate", "setNull", "setString" -> null;
                default -> throw new AssertionError(m.getName());
            });
    private final Connection conn = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
            (p,m,a) -> {
                assertEquals("prepareStatement",m.getName()); sql = (String)a[0]; return stmt;
            });
    private Mantenimiento orden() {
        Mantenimiento m = new Mantenimiento(7,LocalDate.now(),TipoMantenimiento.CORRECTIVO,"Diagnóstico",42,
                LocalDate.now(),ResultadoMantenimiento.REPARADO,EstadoMantenimiento.CERRADO,"");
        m.setIdMantenimiento(10); return m;
    }
    @Test void cierreCondicionalRechazaCeroFilasYCierraStatement() {
        filas = 0;
        assertThrows(SQLException.class, () -> new MantenimientoRepository().actualizar(orden(),conn));
        assertTrue(sql.contains("AND estado = 'ABIERTO'")); assertTrue(statementCerrado);
    }
    @Test void insercionSinClaveFallaYCierraRecursos() {
        assertThrows(SQLException.class, () -> new MantenimientoRepository().guardar(orden(),conn));
        assertTrue(statementCerrado); assertTrue(resultSetCerrado);
    }
    @Test void insercionConClaveDevuelveIdentificador() throws Exception {
        clave = true;
        assertEquals(10,new MantenimientoRepository().guardar(orden(),conn).getIdMantenimiento());
        assertTrue(statementCerrado); assertTrue(resultSetCerrado);
    }
}

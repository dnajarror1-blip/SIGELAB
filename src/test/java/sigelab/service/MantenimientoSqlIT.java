package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.config.DatabaseConfig;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Mantenimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración que modifica EXCLUSIVAMENTE SIGELAB_TEST_MAINT.
 * No se ejecuta con el patrón normal de Surefire (*Test); invocar explícitamente.
 */
class MantenimientoSqlIT {

    private static final int ID_EQUIPO = 1;
    private static final int ID_USUARIO = 1;

    @Test
    void probarIngresoYCierreConAuditoria() throws Exception {
        assertEquals(
                "SIGELAB_TEST_MAINT",
                System.getenv("DB_NAME"),
                "Configure DB_NAME=SIGELAB_TEST_MAINT antes de ejecutar"
        );

        try (Connection conn = DatabaseConfig.getConnection()) {
            assertEquals(
                    "SIGELAB_TEST_MAINT",
                    conn.getCatalog(),
                    "La conexión no apunta a la base aislada de pruebas"
            );
        }

        assertEquals(
                "EQ-MAINT-TEST-001",
                consultarValor(
                        "SELECT codigo_inventario FROM dbo.Equipo WHERE id_equipo = ?",
                        ID_EQUIPO
                )
        );
        assertEquals(
                "DISPONIBLE",
                consultarValor("SELECT estado FROM dbo.Equipo WHERE id_equipo = ?", ID_EQUIPO)
        );
        assertEquals(
                "ACTIVO",
                consultarValor("SELECT estado FROM dbo.Usuario WHERE id_usuario = ?", ID_USUARIO)
        );

        MantenimientoService service = new MantenimientoService();

        Mantenimiento orden = service.ingresarATaller(
                ID_EQUIPO,
                TipoMantenimiento.PREVENTIVO,
                ID_USUARIO,
                "Prueba de integración SQL Server",
                "Registro automático de prueba"
        );

        assertNotNull(orden);
        assertTrue(orden.getIdMantenimiento() > 0);
        int idMantenimiento = orden.getIdMantenimiento();

        assertEquals(
                "MANTENIMIENTO",
                consultarValor("SELECT estado FROM dbo.Equipo WHERE id_equipo = ?", ID_EQUIPO)
        );
        assertTrue(existeAuditoria(idMantenimiento, "MANTENIMIENTO_INICIO"));

        service.finalizarMantenimiento(
                idMantenimiento,
                ResultadoMantenimiento.REPARADO,
                "Equipo reparado correctamente",
                ID_USUARIO
        );

        assertEquals(
                "DISPONIBLE",
                consultarValor("SELECT estado FROM dbo.Equipo WHERE id_equipo = ?", ID_EQUIPO)
        );
        assertEquals(
                "CERRADO",
                consultarValor(
                        "SELECT estado FROM dbo.Mantenimiento WHERE id_mantenimiento = ?",
                        idMantenimiento
                )
        );
        assertEquals(
                "REPARADO",
                consultarValor(
                        "SELECT resultado FROM dbo.Mantenimiento WHERE id_mantenimiento = ?",
                        idMantenimiento
                )
        );
        assertTrue(existeAuditoria(idMantenimiento, "MANTENIMIENTO_REPARADO"));
    }

    private String consultarValor(String sql, int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "No se encontró el registro esperado");
                return rs.getString(1);
            }
        }
    }

    private boolean existeAuditoria(int idMantenimiento, String accion) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM dbo.Auditoria
                WHERE id_registro = ?
                  AND accion = ?
                  AND entidad = 'Mantenimiento'
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMantenimiento);
            ps.setString(2, accion);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1) > 0;
            }
        }
    }
}

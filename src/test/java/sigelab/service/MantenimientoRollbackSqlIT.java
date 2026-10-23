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
 * Verifica el rollback del servicio cuando falla una auditoría durante el cierre.
 * Escribe solo en SIGELAB_TEST_MAINT y debe ejecutarse explícitamente.
 */
class MantenimientoRollbackSqlIT {

    private static final int ID_EQUIPO = 1;
    private static final int ID_USUARIO_VALIDO = 1;
    private static final int ID_USUARIO_INEXISTENTE = Integer.MAX_VALUE;

    @Test
    void errorDeAuditoriaRevierteCierreYEstadoDelEquipo() throws Exception {
        // Esta protección se evalúa antes de cualquier modificación.
        assertEquals("SIGELAB_TEST_MAINT", System.getenv("DB_NAME"),
                "Solo se permite ejecutar contra SIGELAB_TEST_MAINT");

        try (Connection conn = DatabaseConfig.getConnection()) {
            assertEquals("SIGELAB_TEST_MAINT", conn.getCatalog());
        }

        assertEquals("EQ-MAINT-TEST-001", consultarTexto(
                "SELECT codigo_inventario FROM dbo.Equipo WHERE id_equipo = ?", ID_EQUIPO));
        assertEquals("DISPONIBLE", estadoEquipo());
        assertEquals(0, consultarCantidad(
                "SELECT COUNT(*) FROM dbo.Usuario WHERE id_usuario = ?", ID_USUARIO_INEXISTENTE));

        MantenimientoService service = new MantenimientoService();
        Mantenimiento nuevo = service.ingresarATaller(
                ID_EQUIPO,
                TipoMantenimiento.PREVENTIVO,
                ID_USUARIO_VALIDO,
                "Prueba controlada de rollback de auditoría",
                "No usar en producción"
        );

        assertNotNull(nuevo);
        int idMantenimiento = nuevo.getIdMantenimiento();
        assertTrue(idMantenimiento > 0);

        try {
            assertEquals("ABIERTO", estadoMantenimiento(idMantenimiento));
            assertEquals("MANTENIMIENTO", estadoEquipo());

            // El cierre cambia registros, pero el usuario de auditoría no existe.
            // La FK de dbo.Auditoria debe hacer fallar la operación y provocar rollback.
            assertThrows(RuntimeException.class, () ->
                    service.finalizarMantenimiento(
                            idMantenimiento,
                            ResultadoMantenimiento.REPARADO,
                            "Cierre que debe revertirse",
                            ID_USUARIO_INEXISTENTE
                    )
            );

            // Las dos actualizaciones previas al intento de auditar se revierten.
            assertEquals("ABIERTO", estadoMantenimiento(idMantenimiento));
            assertEquals("MANTENIMIENTO", estadoEquipo());
            assertEquals(0, cantidadAuditoriasDeCierre(idMantenimiento));

        } finally {
            // Deja la orden y el equipo en estado utilizable incluso si falla
            // alguna de las aserciones anteriores, cuando la base lo permite.
            if ("ABIERTO".equals(estadoMantenimiento(idMantenimiento))) {
                service.finalizarMantenimiento(
                        idMantenimiento,
                        ResultadoMantenimiento.REPARADO,
                        "Cierre de limpieza tras prueba de rollback",
                        ID_USUARIO_VALIDO
                );
            }
        }

        assertEquals("CERRADO", estadoMantenimiento(idMantenimiento));
        assertEquals("DISPONIBLE", estadoEquipo());
        assertEquals(1, cantidadAuditoriasDeCierre(idMantenimiento));
    }

    private String estadoEquipo() throws SQLException {
        return consultarTexto("SELECT estado FROM dbo.Equipo WHERE id_equipo = ?", ID_EQUIPO);
    }

    private String estadoMantenimiento(int idMantenimiento) throws SQLException {
        return consultarTexto(
                "SELECT estado FROM dbo.Mantenimiento WHERE id_mantenimiento = ?",
                idMantenimiento
        );
    }

    private int cantidadAuditoriasDeCierre(int idMantenimiento) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM dbo.Auditoria
                WHERE entidad = 'Mantenimiento'
                  AND accion = 'MANTENIMIENTO_REPARADO'
                  AND id_registro = ?
                """;
        return consultarCantidad(sql, idMantenimiento);
    }

    private String consultarTexto(String sql, int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "No se encontró el registro esperado");
                return rs.getString(1);
            }
        }
    }

    private int consultarCantidad(String sql, int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }
}

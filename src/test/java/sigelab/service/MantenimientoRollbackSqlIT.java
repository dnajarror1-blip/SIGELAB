package sigelab.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import sigelab.config.DatabaseConfig;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Mantenimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MantenimientoRollbackSqlIT {

    private static final int ID_EQUIPO = 1;
    private static final int ID_USUARIO_VALIDO = 1;
    private static final int ID_USUARIO_INEXISTENTE = Integer.MAX_VALUE;

    private int idMantenimientoCreado = 0;
    private String descripcionPrueba;

    @AfterEach
    void limpiarDatosDePrueba() throws SQLException {

        if (idMantenimientoCreado > 0) {
            LimpiezaMantenimientoIT.limpiar(
                    idMantenimientoCreado,
                    ID_EQUIPO,
                    descripcionPrueba
            );
        }
    }

    @Test
    void errorDeAuditoriaRevierteCierreYEstadoDelEquipo()
            throws Exception {

        // Proteger la base de datos.
        assertEquals(
                "SIGELAB_TEST_MAINT",
                System.getenv("DB_NAME"),
                "Solo se permite ejecutar contra SIGELAB_TEST_MAINT"
        );

        try (Connection conn = DatabaseConfig.getConnection()) {
            assertEquals(
                    "SIGELAB_TEST_MAINT",
                    conn.getCatalog()
            );
        }

        // Verificar el equipo de prueba.
        assertEquals(
                "EQ-MAINT-TEST-001",
                consultarTexto(
                        "SELECT codigo_inventario FROM dbo.Equipo WHERE id_equipo = ?",
                        ID_EQUIPO
                )
        );

        assertEquals("DISPONIBLE", estadoEquipo());

        // Confirmar que el usuario inexistente realmente no existe.
        assertEquals(
                0,
                consultarCantidad(
                        "SELECT COUNT(*) FROM dbo.Usuario WHERE id_usuario = ?",
                        ID_USUARIO_INEXISTENTE
                )
        );

        // Crear una identificación única para esta prueba.
        descripcionPrueba = "IT-SIGELAB-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        MantenimientoService service = new MantenimientoService();

        // Iniciar mantenimiento.
        Mantenimiento nuevo = service.ingresarATaller(
                ID_EQUIPO,
                TipoMantenimiento.PREVENTIVO,
                ID_USUARIO_VALIDO,
                descripcionPrueba,
                "Prueba controlada de rollback de auditoría"
        );

        assertNotNull(nuevo);

        idMantenimientoCreado = nuevo.getIdMantenimiento();

        assertTrue(idMantenimientoCreado > 0);

        // Verificar el estado inicial del mantenimiento.
        assertEquals(
                "ABIERTO",
                estadoMantenimiento(idMantenimientoCreado)
        );

        assertEquals("MANTENIMIENTO", estadoEquipo());

        // Provocar un error durante la auditoría.
        // El usuario inexistente debe causar el fallo de la FK.
        assertThrows(
                RuntimeException.class,
                () -> service.finalizarMantenimiento(
                        idMantenimientoCreado,
                        ResultadoMantenimiento.REPARADO,
                        "Cierre que debe revertirse",
                        ID_USUARIO_INEXISTENTE
                )
        );

        // Verificar que se haya realizado el rollback.
        assertEquals(
                "ABIERTO",
                estadoMantenimiento(idMantenimientoCreado),
                "El mantenimiento debe continuar abierto"
        );

        assertEquals(
                "MANTENIMIENTO",
                estadoEquipo(),
                "El equipo debe continuar en mantenimiento"
        );

        assertEquals(
                0,
                cantidadAuditoriasDeCierre(idMantenimientoCreado),
                "No debe existir auditoría del cierre fallido"
        );

        // Al terminar, @AfterEach elimina los registros
        // creados y restablece el equipo a DISPONIBLE.
    }

    private String estadoEquipo() throws SQLException {
        return consultarTexto(
                "SELECT estado FROM dbo.Equipo WHERE id_equipo = ?",
                ID_EQUIPO
        );
    }

    private String estadoMantenimiento(int idMantenimiento)
            throws SQLException {

        return consultarTexto(
                "SELECT estado FROM dbo.Mantenimiento WHERE id_mantenimiento = ?",
                idMantenimiento
        );
    }

    private int cantidadAuditoriasDeCierre(int idMantenimiento)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM dbo.Auditoria
                WHERE entidad = 'Mantenimiento'
                  AND accion = 'MANTENIMIENTO_REPARADO'
                  AND id_registro = ?
                """;

        return consultarCantidad(sql, idMantenimiento);
    }

    private String consultarTexto(String sql, int id)
            throws SQLException {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                assertTrue(
                        rs.next(),
                        "No se encontró el registro esperado"
                );

                return rs.getString(1);
            }
        }
    }

    private int consultarCantidad(String sql, int id)
            throws SQLException {

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
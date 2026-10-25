
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

class MantenimientoSqlIT {

    private static final int ID_EQUIPO = 1;
    private static final int ID_USUARIO = 1;

    private int idMantenimientoCreado;
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
    void probarIngresoYCierreConAuditoria() throws Exception {

        // Bloquear la ejecucion fuera de la base de pruebas.
        assertEquals(
                "SIGELAB_TEST_MAINT",
                System.getenv("DB_NAME"),
                "Configure DB_NAME=SIGELAB_TEST_MAINT"
        );

        try (Connection conn = DatabaseConfig.getConnection()) {
            assertEquals(
                    "SIGELAB_TEST_MAINT",
                    conn.getCatalog(),
                    "La conexion no apunta a la base de pruebas"
            );
        }

        // Verificar los datos iniciales.
        assertEquals(
                "EQ-MAINT-TEST-001",
                consultarValor(
                        "SELECT codigo_inventario FROM dbo.Equipo WHERE id_equipo = ?",
                        ID_EQUIPO
                )
        );

        assertEquals(
                "DISPONIBLE",
                consultarValor(
                        "SELECT estado FROM dbo.Equipo WHERE id_equipo = ?",
                        ID_EQUIPO
                )
        );

        assertEquals(
                "ACTIVO",
                consultarValor(
                        "SELECT estado FROM dbo.Usuario WHERE id_usuario = ?",
                        ID_USUARIO
                )
        );

        // Identificador exclusivo para esta ejecucion.
        descripcionPrueba = "IT-SIGELAB-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12);

        MantenimientoService service = new MantenimientoService();

        // Registrar el ingreso del equipo al taller.
        Mantenimiento orden = service.ingresarATaller(
                ID_EQUIPO,
                TipoMantenimiento.PREVENTIVO,
                ID_USUARIO,
                descripcionPrueba,
                "Registro automatico de prueba"
        );

        assertNotNull(orden);

        // Guardar el ID inmediatamente para la limpieza.
        idMantenimientoCreado = orden.getIdMantenimiento();

        assertTrue(idMantenimientoCreado > 0);

        // Comprobar el estado y la auditoria de ingreso.
        assertEquals(
                "MANTENIMIENTO",
                consultarValor(
                        "SELECT estado FROM dbo.Equipo WHERE id_equipo = ?",
                        ID_EQUIPO
                )
        );

        assertTrue(
                existeAuditoria(
                        idMantenimientoCreado,
                        "MANTENIMIENTO_INICIO"
                )
        );

        // Finalizar el mantenimiento.
        service.finalizarMantenimiento(
                idMantenimientoCreado,
                ResultadoMantenimiento.REPARADO,
                "Equipo reparado correctamente",
                ID_USUARIO
        );

        // Comprobar el estado final.
        assertEquals(
                "DISPONIBLE",
                consultarValor(
                        "SELECT estado FROM dbo.Equipo WHERE id_equipo = ?",
                        ID_EQUIPO
                )
        );

        assertEquals(
                "CERRADO",
                consultarValor(
                        "SELECT estado FROM dbo.Mantenimiento WHERE id_mantenimiento = ?",
                        idMantenimientoCreado
                )
        );

        assertEquals(
                "REPARADO",
                consultarValor(
                        "SELECT resultado FROM dbo.Mantenimiento WHERE id_mantenimiento = ?",
                        idMantenimientoCreado
                )
        );

        assertTrue(
                existeAuditoria(
                        idMantenimientoCreado,
                        "MANTENIMIENTO_REPARADO"
                )
        );
    }

    private String consultarValor(String sql, int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "No se encontro el registro esperado");
                return rs.getString(1);
            }
        }
    }

    private boolean existeAuditoria(int idMantenimiento, String accion)
            throws SQLException {

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

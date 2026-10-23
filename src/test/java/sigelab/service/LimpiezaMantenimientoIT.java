
package sigelab.service;

import sigelab.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class LimpiezaMantenimientoIT {

    private static final String BASE_PRUEBAS = "SIGELAB_TEST_MAINT";
    private static final String CODIGO_EQUIPO = "EQ-MAINT-TEST-001";

    private LimpiezaMantenimientoIT() {
    }

    static void limpiar(int idMantenimiento,
                        int idEquipo,
                        String descripcionPrueba) throws SQLException {

        if (idMantenimiento <= 0) {
            return;
        }

        if (!BASE_PRUEBAS.equals(System.getenv("DB_NAME"))) {
            throw new IllegalStateException(
                    "Limpieza bloqueada: DB_NAME no corresponde a la base de pruebas"
            );
        }

        if (descripcionPrueba == null
                || !descripcionPrueba.startsWith("IT-SIGELAB-")) {
            throw new IllegalArgumentException(
                    "La orden no tiene una identificacion valida de prueba"
            );
        }

        try (Connection conn = DatabaseConfig.getConnection()) {

            if (!BASE_PRUEBAS.equals(conn.getCatalog())) {
                throw new IllegalStateException(
                        "Limpieza bloqueada: conexion a una base incorrecta"
                );
            }

            conn.setAutoCommit(false);

            try {
                // Confirmar que la orden pertenece a esta prueba.
                String sqlOrden = """
                        SELECT m.descripcion, e.codigo_inventario
                        FROM dbo.Mantenimiento m
                        INNER JOIN dbo.Equipo e
                            ON e.id_equipo = m.id_equipo
                        WHERE m.id_mantenimiento = ?
                          AND m.id_equipo = ?
                        """;

                try (PreparedStatement ps =
                             conn.prepareStatement(sqlOrden)) {

                    ps.setInt(1, idMantenimiento);
                    ps.setInt(2, idEquipo);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()
                                || !descripcionPrueba.equals(rs.getString(1))
                                || !CODIGO_EQUIPO.equals(rs.getString(2))) {
                            throw new IllegalStateException(
                                    "La orden no coincide con los datos de prueba"
                            );
                        }
                    }
                }

                // Eliminar auditorias asociadas a la orden creada.
                String sqlAuditoria = """
                        DELETE FROM dbo.Auditoria
                        WHERE entidad = 'Mantenimiento'
                          AND id_registro = ?
                          AND accion IN (
                              'MANTENIMIENTO_INICIO',
                              'MANTENIMIENTO_REPARADO'
                          )
                        """;

                try (PreparedStatement ps =
                             conn.prepareStatement(sqlAuditoria)) {
                    ps.setInt(1, idMantenimiento);
                    ps.executeUpdate();
                }

                // Eliminar exclusivamente la orden identificada.
                String sqlMantenimiento = """
                        DELETE FROM dbo.Mantenimiento
                        WHERE id_mantenimiento = ?
                          AND id_equipo = ?
                          AND descripcion = ?
                        """;

                try (PreparedStatement ps =
                             conn.prepareStatement(sqlMantenimiento)) {

                    ps.setInt(1, idMantenimiento);
                    ps.setInt(2, idEquipo);
                    ps.setString(3, descripcionPrueba);

                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException(
                                "No se pudo eliminar exactamente la orden de prueba"
                        );
                    }
                }

                // Restaurar el equipo de prueba a DISPONIBLE.
                String sqlEquipo = """
                        UPDATE dbo.Equipo
                        SET estado = 'DISPONIBLE'
                        WHERE id_equipo = ?
                          AND codigo_inventario = ?
                          AND estado IN ('MANTENIMIENTO', 'DISPONIBLE')
                          AND NOT EXISTS (
                              SELECT 1
                              FROM dbo.Mantenimiento
                              WHERE id_equipo = ?
                                AND estado = 'ABIERTO'
                          )
                        """;

                try (PreparedStatement ps =
                             conn.prepareStatement(sqlEquipo)) {

                    ps.setInt(1, idEquipo);
                    ps.setString(2, CODIGO_EQUIPO);
                    ps.setInt(3, idEquipo);

                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException(
                                "No fue posible restaurar el equipo de prueba"
                        );
                    }
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }
}

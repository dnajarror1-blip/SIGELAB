package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoPrestamo;
import sigelab.model.Prestamo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PrestamoServiceIT {

    @Test
    void creaPrestamoConDetalleYAuditoria() throws Exception {
        int idUsuario = 1;
        int idEquipo = 1;

        // Identifica este préstamo como una prueba.
        String marcaPrueba = "Prueba P4 " + UUID.randomUUID();

        // Verificamos que sean los datos de prueba esperados.
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                    SELECT estado
                    FROM dbo.Equipo
                    WHERE id_equipo = ?
                      AND codigo_inventario = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idEquipo);
                stmt.setString(2, "EQ-PRUEBA-P4-001");

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "No se encontró el equipo de prueba.");
                    assertEquals(
                            "DISPONIBLE",
                            rs.getString("estado"),
                            "El equipo de prueba debe estar disponible."
                    );
                }
            }

            String sqlUsuario = """
                    SELECT estado
                    FROM dbo.Usuario
                    WHERE id_usuario = ?
                      AND username = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sqlUsuario)) {
                stmt.setInt(1, idUsuario);
                stmt.setString(2, "prestamos.prueba.p4");

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "No se encontró el usuario de prueba.");
                    assertEquals("ACTIVO", rs.getString("estado"));
                }
            }
        }

        Prestamo prestamo = new Prestamo(
                idUsuario,
                idUsuario,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                EstadoPrestamo.ACTIVO,
                marcaPrueba
        );

        int idPrestamo = new PrestamoService()
                .crearPrestamo(prestamo, List.of(idEquipo));

        System.out.println("ID del préstamo de prueba: " + idPrestamo);
        System.out.println("Marca de la prueba: " + marcaPrueba);

        assertTrue(idPrestamo > 0);
        assertEquals(idPrestamo, prestamo.getIdPrestamo());
        assertEquals(EstadoPrestamo.ACTIVO, prestamo.getEstado());

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sqlPrestamo = """
                    SELECT estado, observaciones
                    FROM dbo.Prestamo
                    WHERE id_prestamo = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sqlPrestamo)) {
                stmt.setInt(1, idPrestamo);

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "No se encontró el préstamo.");
                    assertEquals("ACTIVO", rs.getString("estado"));
                    assertEquals(marcaPrueba, rs.getString("observaciones"));
                }
            }

            String sqlDetalle = """
                    SELECT COUNT(*)
                    FROM dbo.DetallePrestamo
                    WHERE id_prestamo = ? AND id_equipo = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sqlDetalle)) {
                stmt.setInt(1, idPrestamo);
                stmt.setInt(2, idEquipo);

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "Falta el detalle del préstamo.");
                }
            }

            String sqlEquipo = """
                    SELECT estado
                    FROM dbo.Equipo
                    WHERE id_equipo = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sqlEquipo)) {
                stmt.setInt(1, idEquipo);

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("PRESTADO", rs.getString("estado"));
                }
            }

            String sqlAuditoria = """
                    SELECT COUNT(*)
                    FROM dbo.Auditoria
                    WHERE id_registro = ?
                      AND entidad = 'Prestamo'
                      AND accion = 'PRESTAMO_CREADO'
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sqlAuditoria)) {
                stmt.setInt(1, idPrestamo);

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "Falta la auditoría.");
                }
            }
        }
    }
    @Test
    void rechazaEquipoQueYaEstaPrestado() {
        Prestamo prestamo = new Prestamo(
                1,
                1,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                EstadoPrestamo.ACTIVO,
                "Prueba de equipo ya prestado"
        );

        assertThrows(
                sigelab.exception.BusinessException.class,
                () -> new PrestamoService().crearPrestamo(prestamo, List.of(1))
        );

        assertNull(prestamo.getIdPrestamo());
    }
    @Test
    void encuentraPrestamoPorId() {
        PrestamoService servicio = new PrestamoService();

        Prestamo encontrado = servicio.buscarPorId(1)
                .orElseThrow(() -> new AssertionError(
                        "No se encontró el préstamo de prueba."
                ));

        assertEquals(1, encontrado.getIdPrestamo());
        assertEquals(EstadoPrestamo.ACTIVO, encontrado.getEstado());
        assertTrue(encontrado.getObservaciones().startsWith("Prueba P4 "));
    }
    @Test
    void encuentraPrestamoDePruebaEntreLosActivos() {
        List<Prestamo> activos = new PrestamoService().buscarActivos();

        assertFalse(activos.isEmpty(), "La lista de préstamos activos está vacía.");

        assertTrue(
                activos.stream().anyMatch(
                        prestamo -> Integer.valueOf(1).equals(prestamo.getIdPrestamo())
                                && prestamo.getEstado() == EstadoPrestamo.ACTIVO
                ),
                "No se encontró el préstamo de prueba entre los activos."
        );

        assertTrue(
                activos.stream().allMatch(
                        prestamo -> prestamo.getEstado() == EstadoPrestamo.ACTIVO
                ),
                "La lista contiene un préstamo que no está activo."
        );
    }
    @Test
    void consultaActivosDelSolicitanteDePrueba() {
        List<Prestamo> prestamos = new PrestamoService()
                .buscarActivosPorSolicitante(1);

        assertFalse(
                prestamos.isEmpty(),
                "Deben aparecer los préstamos del solicitante de prueba."
        );

        assertTrue(
                prestamos.stream().allMatch(
                        prestamo -> prestamo.getIdUsuarioSolicitante() == 1
                                && prestamo.getEstado() == EstadoPrestamo.ACTIVO
                ),
                "La consulta debe devolver solo préstamos activos del solicitante."
        );

        assertTrue(
                new PrestamoService()
                        .buscarActivosPorSolicitante(Integer.MAX_VALUE)
                        .isEmpty(),
                "Un solicitante sin préstamos debe obtener una lista vacía."
        );
    }
}
package sigelab.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoPrestamo;
import sigelab.exception.DataAccessException;
import sigelab.model.Prestamo;
import sigelab.repository.EquipoRepository;

import static org.junit.jupiter.api.Assertions.*;

class PrestamoRollbackSqlIT {

    @Test
    void falloTrasActualizarEquipoReviertePrestamoYDetalle()
            throws Exception {

        // Esta prueba utiliza únicamente la base local indicada.
        assertEquals("127.0.0.1", System.getenv("DB_HOST"));
        assertEquals("SIGELAB", System.getenv("DB_NAME"));

        int idEquipo;
        int idUsuario;

        try (Connection conn = DatabaseConfig.getConnection()) {
            assertEquals("SIGELAB", conn.getCatalog());

            idEquipo = consultarEntero(
                    conn,
                    """
                    SELECT id_equipo
                    FROM dbo.Equipo
                    WHERE codigo_inventario = ?
                    """,
                    "EQ-PRUEBA-P4-004"
            );

            idUsuario = consultarEntero(
                    conn,
                    """
                    SELECT id_usuario
                    FROM dbo.Usuario
                    WHERE username = ?
                      AND estado = 'ACTIVO'
                    """,
                    "prestamos.prueba.p4"
            );
        }

        assertEquals("DISPONIBLE", consultarEstado(idEquipo));

        int detallesAntes = contarDetalles(idEquipo);
        assertEquals(
                0,
                detallesAntes,
                "El equipo debe estar reservado exclusivamente para esta prueba."
        );

        String marca = "ROLLBACK-P4-" + UUID.randomUUID();

        // Confirma que el fallo ocurre después de ejecutar el UPDATE.
        boolean[] actualizacionEjecutada = {false};

        EquipoRepository repositorioConFallo = new EquipoRepository() {
            @Override
            public void actualizarEstado(
                    int id,
                    EstadoEquipo estado,
                    Connection conn
            ) throws SQLException {
                super.actualizarEstado(id, estado, conn);

                actualizacionEjecutada[0] = true;

                throw new SQLException("FALLO_CONTROLADO_ROLLBACK_P4");
            }
        };

        PrestamoService servicio =
                new PrestamoService(repositorioConFallo);

        Prestamo prestamo = new Prestamo(
                idUsuario,
                idUsuario,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                EstadoPrestamo.ACTIVO,
                marca
        );

        DataAccessException error = assertThrows(
                DataAccessException.class,
                () -> servicio.crearPrestamo(
                        prestamo,
                        List.of(idEquipo)
                )
        );

        assertTrue(
                actualizacionEjecutada[0],
                "La prueba debe alcanzar la actualización del equipo."
        );

        assertNotNull(error.getCause());
        assertEquals(
                "FALLO_CONTROLADO_ROLLBACK_P4",
                error.getCause().getMessage()
        );

        // El objeto recibido tampoco debe quedar marcado como registrado.
        assertNull(prestamo.getIdPrestamo());

        // Consultamos usando conexiones nuevas después del rollback.
        assertEquals(
                "DISPONIBLE",
                consultarEstado(idEquipo),
                "El cambio a PRESTADO debe revertirse."
        );

        assertEquals(
                detallesAntes,
                contarDetalles(idEquipo),
                "No debe quedar el detalle del préstamo fallido."
        );

        try (Connection conn = DatabaseConfig.getConnection()) {
            assertEquals(
                    0,
                    consultarEntero(
                            conn,
                            """
                            SELECT COUNT(*)
                            FROM dbo.Prestamo
                            WHERE observaciones = ?
                            """,
                            marca
                    ),
                    "No debe quedar el encabezado del préstamo fallido."
            );
        }

        System.out.println(
                "Rollback comprobado: sin préstamo ni detalle nuevos, "
                        + "equipo DISPONIBLE."
        );
    }

    private String consultarEstado(int idEquipo) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT estado FROM dbo.Equipo WHERE id_equipo = ?"
             )) {

            stmt.setInt(1, idEquipo);

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "No se encontró el equipo de prueba.");
                return rs.getString("estado");
            }
        }
    }

    private int contarDetalles(int idEquipo) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return consultarEntero(
                    conn,
                    """
                    SELECT COUNT(*)
                    FROM dbo.DetallePrestamo
                    WHERE id_equipo = ?
                    """,
                    idEquipo
            );
        }
    }

    private int consultarEntero(
            Connection conn,
            String sql,
            Object parametro
    ) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, parametro);

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(
                        rs.next(),
                        "No se encontró el dato de prueba requerido."
                );

                return rs.getInt(1);
            }
        }
    }
}
package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class ConexionBasePruebasIT {

    @Test
    void verificarConexionBaseDePruebas() throws Exception {

        // Evitar conexiones accidentales a la base principal
        assertEquals(
                "SIGELAB_TEST_MAINT",
                System.getenv("DB_NAME"),
                "La prueba debe utilizar SIGELAB_TEST_MAINT"
        );

        try (Connection conn = DatabaseConfig.getConnection()) {

            assertNotNull(conn);
            assertFalse(conn.isClosed());

            assertEquals(
                    "SIGELAB_TEST_MAINT",
                    conn.getCatalog(),
                    "Java se conectó a una base incorrecta"
            );

            String sql = "SELECT COUNT(*) FROM sys.tables";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                assertTrue(rs.next());

                int totalTablas = rs.getInt(1);

                assertEquals(14, totalTablas);

                System.out.println("Conexion exitosa con SQL Server");
                System.out.println("Base de datos: " + conn.getCatalog());
                System.out.println("Tablas encontradas: " + totalTablas);
            }
        }
    }
}
package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class FaceIdDatabaseIsolationIT {

    private static final String BASE_PERMITIDA =
            "SIGELAB_TEST_FACE";

    @Test
    void verificarConexionExclusivaBasePruebas() throws Exception {

        // Verificar la configuración antes de conectar.
        assertEquals(
                BASE_PERMITIDA,
                System.getenv("DB_NAME"),
                "Java debe configurarse para SIGELAB_TEST_FACE"
        );

        // Esta prueba no modifica información.
        try (
                Connection conexion =
                        DatabaseConfig.getConnection();

                Statement sentencia =
                        conexion.createStatement();

                ResultSet resultado =
                        sentencia.executeQuery("SELECT DB_NAME()")
        ) {

            assertTrue(
                    resultado.next(),
                    "SQL Server no devolvió el nombre de la base"
            );

            String baseActual = resultado.getString(1);

            System.out.println(
                    "Base de datos Java: " + baseActual
            );

            assertEquals(
                    BASE_PERMITIDA,
                    baseActual,
                    "La conexión Java apunta a una base no autorizada"
            );

            System.out.println(
                    "OK: Java conectado exclusivamente a SIGELAB_TEST_FACE"
            );
        }
    }
}

package sigelab.config;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionIT {

    @Test
    void verificarConexion() throws Exception {

        try (Connection connection = DatabaseConfig.getConnection()) {

            assertTrue(connection.isValid(5));
            assertEquals("SIGELAB", connection.getCatalog());

            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT 1");
                 ResultSet result = statement.executeQuery()) {

                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }
}

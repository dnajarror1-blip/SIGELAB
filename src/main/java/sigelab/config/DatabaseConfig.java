package sigelab.config;

import sigelab.exception.DatabaseConfigurationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {

        String host = getConfig("DB_HOST", "127.0.0.1");
        String port = getConfig("DB_PORT", "1433");
        String database = getConfig("DB_NAME", "SIGELAB");
        String user = getConfig("DB_USER", "sa");
        String password = LocalConfig.get("DB_PASSWORD");

        String encrypt = getConfig("DB_ENCRYPT", "true");
        String trustCertificate = getConfig(
                "DB_TRUST_SERVER_CERTIFICATE", "false"
        );

        if (password == null || password.isBlank()) {
            throw new DatabaseConfigurationException(
                    "Falta configurar DB_PASSWORD"
            );
        }

        String url = "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + database
                + ";encrypt=" + encrypt
                + ";trustServerCertificate=" + trustCertificate;

        return DriverManager.getConnection(url, user, password);
    }

    private static String getConfig(String name, String defaultValue) {
        String value = LocalConfig.get(name);
        return (value == null || value.isBlank())
                ? defaultValue
                : value;
    }
}

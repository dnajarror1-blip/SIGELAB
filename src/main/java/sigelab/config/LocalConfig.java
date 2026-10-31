package sigelab.config;

import com.google.gson.JsonParser;
import sigelab.exception.DatabaseConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Configuración local sin ejecutar shell ni expandir variables. Entorno real tiene prioridad. */
public final class LocalConfig {
    private LocalConfig() { }

    public static String get(String name) {
        String environment = System.getenv(name);
        if (environment != null) return environment;
        Path file = Path.of(".env"); // Directorio de ejecución: raíz de SIGELAB.
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return null;
        try {
            if (Files.isSymbolicLink(file) || !Files.isRegularFile(file) || Files.size(file) > 65536)
                throw new IOException();
            Map<String,String> values = new HashMap<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int equals = line.indexOf('=');
                if (equals < 1) throw new IllegalArgumentException();
                String key = line.substring(0, equals).strip();
                String value = line.substring(equals + 1).strip();
                if (!key.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException();
                if (value.startsWith("\"")) {
                    var parsed = JsonParser.parseString(value);
                    if (!parsed.isJsonPrimitive() || !parsed.getAsJsonPrimitive().isString())
                        throw new IllegalArgumentException();
                    value = parsed.getAsString();
                } else if (value.startsWith("'")) {
                    if (!value.endsWith("'") || value.length() < 2) throw new IllegalArgumentException();
                    value = value.substring(1, value.length() - 1);
                }
                if (values.putIfAbsent(key, value) != null) throw new IllegalArgumentException();
            }
            return values.get(name);
        } catch (IOException | RuntimeException error) {
            // No incluir líneas, valores ni la excepción de parseo en mensajes/logs.
            throw new DatabaseConfigurationException("No se pudo leer la configuración local .env. Revise formato y permisos.");
        }
    }
}

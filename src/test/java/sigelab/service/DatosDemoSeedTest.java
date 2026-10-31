package sigelab.service;

import java.nio.file.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import static org.junit.jupiter.api.Assertions.*;

class DatosDemoSeedTest {
    @Test void hashDelSeedEsBcryptValido() throws Exception {
        var sql = Files.readString(Path.of("database/seed/003_datos_demo.sql"));
        var match = Pattern.compile("\\$2a\\$10\\$[./A-Za-z0-9]{53}").matcher(sql);
        assertTrue(match.find());
        assertTrue(BCrypt.checkpw("SigelabDemo2026!", match.group()));
        assertFalse(BCrypt.checkpw("incorrecta", match.group()));
        assertFalse(sql.contains("SigelabDemo2026!"));
        assertFalse(sql.contains("INSERT INTO dbo.PerfilFacial"));
    }
}

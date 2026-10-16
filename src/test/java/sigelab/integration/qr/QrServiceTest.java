package sigelab.integration.qr;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class QrServiceTest {

    @Test
    void generarYLeerQR() throws Exception {
        QrService servicio = new QrService();
        File archivo = File.createTempFile("qr-test-", ".png");

        try {
            servicio.generar("INV-TEST-001", archivo);

            assertTrue(archivo.exists());
            assertTrue(archivo.length() > 0);
            assertEquals("INV-TEST-001", servicio.leer(archivo));
        } finally {
            archivo.delete();
        }
    }
}
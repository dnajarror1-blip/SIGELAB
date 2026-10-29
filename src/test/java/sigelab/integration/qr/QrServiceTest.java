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

    @Test
    void generarFallaSiCodigoEsVacio() {
        QrService servicio = new QrService();

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.generar(" ", new File("qr-test.png"))
        );
    }

    @Test
    void generarFallaSiDestinoEsNull() {
        QrService servicio = new QrService();

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.generar("INV-TEST-001", null)
        );
    }

    @Test
    void leerFallaSiImagenEsNull() {
        QrService servicio = new QrService();

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.leer(null)
        );
    }


    @Test
    void leerFallaSiImagenNoContieneQR() throws Exception {
        QrService servicio = new QrService();
        File archivo = File.createTempFile("imagen-sin-qr-", ".png");

        try {
            java.awt.image.BufferedImage imagen =
                    new java.awt.image.BufferedImage(
                            100,
                            100,
                            java.awt.image.BufferedImage.TYPE_INT_RGB
                    );

            javax.imageio.ImageIO.write(imagen, "png", archivo);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> servicio.leer(archivo)
            );
        } finally {
            archivo.delete();
        }
    }

}


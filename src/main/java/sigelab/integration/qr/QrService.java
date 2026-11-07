package sigelab.integration.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;

public class QrService {

    private static final int QR_SIZE = 250;

    public void generar(String codigoInventario, File destino) {
        if (codigoInventario == null || codigoInventario.isBlank()) {
            throw new IllegalArgumentException(
                    "El código de inventario no puede estar vacío."
            );
        }

        if (destino == null) {
            throw new IllegalArgumentException(
                    "El archivo de destino no puede ser null."
            );
        }

        try {
            QRCodeWriter escritor = new QRCodeWriter();

            Map<EncodeHintType, Object> configuracion = Map.of(
                    EncodeHintType.CHARACTER_SET,
                    StandardCharsets.UTF_8.name(),
                    EncodeHintType.MARGIN,
                    1
            );

            BitMatrix matriz = escritor.encode(
                    codigoInventario,
                    BarcodeFormat.QR_CODE,
                    QR_SIZE,
                    QR_SIZE,
                    configuracion
            );

            MatrixToImageWriter.writeToPath(
                    matriz,
                    "PNG",
                    destino.toPath()
            );

        } catch (WriterException | IOException e) {
            throw new IllegalStateException(
                    "No se pudo generar el código QR.",
                    e
            );
        }
    }

    public String leer(File imagen) {
        if (imagen == null || !imagen.isFile()) {
            throw new IllegalArgumentException(
                    "La imagen indicada no existe o no es un archivo."
            );
        }

        try {
            BufferedImage imagenLeida = ImageIO.read(imagen);

            if (imagenLeida == null) {
                throw new IllegalArgumentException(
                        "El archivo no contiene una imagen válida."
                );
            }

            BufferedImageLuminanceSource fuente =
                    new BufferedImageLuminanceSource(imagenLeida);

            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(fuente)
            );

            Result resultado = new MultiFormatReader().decode(bitmap,
                    Map.of(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE)));

            return resultado.getText();

        } catch (NotFoundException e) {
            throw new IllegalArgumentException(
                    "No se encontró un código QR legible en la imagen.",
                    e
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer el archivo de imagen.",
                    e
            );
        }
    }
}


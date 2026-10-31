package sigelab.view;

import sigelab.service.FacialAuthClient;

import javax.swing.*;

/**
 * Vista de prueba para el panel FaceID.
 *
 * No inicia sesiones.
 * No registra rostros.
 * No modifica información.
 */
public final class RegistroFacialPanelPreview {

    private RegistroFacialPanelPreview() {
    }

    public static void main(String[] args) {

        String claveApi =
                System.getenv("FACIAL_API_KEY");

        if (
                claveApi == null
                || claveApi.strip().length() < 32
        ) {

            System.err.println(
                    "FACIAL_API_KEY no está configurada."
            );

            return;
        }

        FacialAuthClient cliente = new FacialAuthClient(
                "http://127.0.0.1:8765",
                claveApi
        );

        SwingUtilities.invokeLater(() -> {

            JFrame ventana = new JFrame(
                    "SIGELAB FaceID - Diagnóstico"
            );

            ventana.setDefaultCloseOperation(
                    JFrame.DISPOSE_ON_CLOSE
            );

            ventana.setContentPane(
                    new RegistroFacialPanel(cliente)
            );

            ventana.setSize(950, 650);

            ventana.setLocationRelativeTo(null);

            ventana.setVisible(true);
        });
    }
}
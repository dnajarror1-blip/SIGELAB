package sigelab.view;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class PrestamoViewPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("SIGELAB - Préstamos");
            ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            ventana.setContentPane(new PrestamoView(1));
            ventana.setSize(1100, 600);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}

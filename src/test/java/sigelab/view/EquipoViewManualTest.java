package sigelab.view;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class EquipoViewManualTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("SIGELAB - Vista previa");
            ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            ventana.setContentPane(new EquipoView());
            ventana.setSize(1100, 700);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}

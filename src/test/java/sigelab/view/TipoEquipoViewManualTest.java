package sigelab.view;

import javax.swing.SwingUtilities;

public class TipoEquipoViewManualTest {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            TipoEquipoView view = new TipoEquipoView();
            view.setVisible(true);
        });
    }
}

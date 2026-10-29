package sigelab.view;

import javax.swing.SwingUtilities;

public class EquipoViewManualTest {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            EquipoView view =
                    new EquipoView();

            view.setVisible(true);
        });
    }
}

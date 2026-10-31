package sigelab.view;

import sigelab.model.Usuario;
import sigelab.service.Modulo;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/** Única ventana de trabajo después del login. */
public class MenuPrincipalForm extends JFrame {
    public MenuPrincipalForm(Usuario usuario, Set<Modulo> menu, String nombreRol) {
        setTitle("SIGELAB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new MenuPrincipalPanel(usuario, menu, nombreRol, this::cerrarSesion));
        setMinimumSize(new Dimension(1000, 650));
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void cerrarSesion() {
        dispose();
        new LoginForm().setVisible(true);
    }
}

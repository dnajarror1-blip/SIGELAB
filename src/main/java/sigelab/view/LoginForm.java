package sigelab.view;

import com.formdev.flatlaf.FlatLightLaf;
import sigelab.controller.LoginController;
import javax.swing.*;
import java.awt.*;

public class LoginForm extends JFrame {
    public LoginForm() {
        setTitle("SIGELAB"); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        LoginPanel formulario = new LoginPanel(new LoginController(), resultado -> {
            if (!isDisplayable()) return;
            new MenuPrincipalForm(resultado.usuario(), resultado.menu(), resultado.nombreRol()).setVisible(true);
            dispose();
        });
        setContentPane(formulario); getRootPane().setDefaultButton(formulario.botonEntrar());
        setMinimumSize(new Dimension(580, 680)); setSize(580, 680); setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 12); UIManager.put("Button.arc", 12); UIManager.put("TextComponent.arc", 12);
            new LoginForm().setVisible(true);
        });
    }
}

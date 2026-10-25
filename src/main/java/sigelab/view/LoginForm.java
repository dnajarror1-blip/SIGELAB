package sigelab.view;

import sigelab.controller.LoginController;

import javax.swing.*;
import java.awt.*;

public class LoginForm extends JFrame {

    // La pantalla solo habla con el controller, nunca con la base de datos
    private final LoginController controller = new LoginController();

    // Los campos que se ven en la ventana
    private final JTextField txtUsuario = new JTextField(15);
    private final JPasswordField txtPassword = new JPasswordField(15);
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);
    private final JButton btnEntrar = new JButton("Entrar");

    public LoginForm() {
        setTitle("SIGELAB - Iniciar sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Un panel con 5 filas: texto, campo, texto, campo, botón
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtPassword);
        panel.add(btnEntrar);
        panel.add(lblMensaje);
        add(panel);

        // Al hacer clic en "Entrar" intentamos iniciar sesión
        btnEntrar.addActionListener(e -> intentarEntrar());

        // Así también funciona con la tecla Enter
        getRootPane().setDefaultButton(btnEntrar);

        pack();                    // ajusta el tamaño a lo que hay adentro
        setLocationRelativeTo(null); // centra la ventana en la pantalla
    }

    private void intentarEntrar() {
        // Leemos lo que escribió el usuario
        String username = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        // Se lo pasamos al controller y esperamos su respuesta
        var resultado = controller.iniciarSesion(username, password);

        if (resultado.exito()) {
            lblMensaje.setForeground(new Color(0, 130, 0)); // verde
            lblMensaje.setText(resultado.mensaje());
            // TEMPORAL: aquí después se abre el menú principal
            System.out.println("Menú del usuario: " + resultado.menu());
        } else {
            lblMensaje.setForeground(Color.RED);
            lblMensaje.setText(resultado.mensaje());
        }

        // Por seguridad, borramos la contraseña del campo
        txtPassword.setText("");
    }

    // TEMPORAL: solo para probar la pantalla sola
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}
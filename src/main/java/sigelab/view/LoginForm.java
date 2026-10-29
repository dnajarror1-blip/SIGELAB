package sigelab.view;

import com.formdev.flatlaf.FlatLightLaf;
import sigelab.controller.LoginController;

import javax.swing.*;
import java.awt.*;

public class LoginForm extends JFrame {

    // Los colores en un solo lugar: si quieres cambiar el estilo, es aquí
    private static final Color FONDO = new Color(243, 244, 246);       // gris muy claro
    private static final Color BORDE = new Color(229, 231, 235);       // borde de la tarjeta
    private static final Color ACENTO = new Color(37, 99, 235);        // azul del botón
    private static final Color TEXTO = new Color(17, 24, 39);          // casi negro
    private static final Color TEXTO_SUAVE = new Color(107, 114, 128); // gris
    private static final Color ERROR = new Color(220, 38, 38);         // rojo

    // La pantalla solo habla con el controller, nunca con la base de datos
    private final LoginController controller = new LoginController();

    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JCheckBox chkMostrar = new JCheckBox("Mostrar contraseña");
    private final JLabel lblMensaje = new JLabel(" ");
    private final JButton btnEntrar = new JButton("Entrar");

    public LoginForm() {
        setTitle("SIGELAB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Título y subtítulo
        JLabel lblTitulo = new JLabel("SIGELAB");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 30f));
        lblTitulo.setForeground(TEXTO);

        JLabel lblSubtitulo = new JLabel("Gestión de laboratorios");
        lblSubtitulo.setFont(lblSubtitulo.getFont().deriveFont(14f));
        lblSubtitulo.setForeground(TEXTO_SUAVE);

        // Campos con texto de ayuda gris dentro
        txtUsuario.putClientProperty("JTextField.placeholderText", "Usuario");
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña");

        // Casilla para ver u ocultar la contraseña
        chkMostrar.setBackground(Color.WHITE);
        chkMostrar.setForeground(TEXTO_SUAVE);
        chkMostrar.setFocusPainted(false);
        char puntitos = txtPassword.getEchoChar(); // guardamos el símbolo normal (•)
        chkMostrar.addActionListener(e ->
                // el 0 significa "sin símbolo", así se ve el texto real
                txtPassword.setEchoChar(chkMostrar.isSelected() ? (char) 0 : puntitos));

        // Botón grande con el color de acento
        btnEntrar.setBackground(ACENTO);
        btnEntrar.setForeground(Color.WHITE);
        btnEntrar.setFont(btnEntrar.getFont().deriveFont(Font.BOLD, 15f));
        btnEntrar.setFocusPainted(false);

        // Mensaje pequeño debajo del botón
        lblMensaje.setFont(lblMensaje.getFont().deriveFont(13f));

        // La tarjeta blanca: todo va en una columna, alineado a la izquierda
        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(45, 45, 45, 45)));
        tarjeta.setPreferredSize(new Dimension(420, 490)); // tamaño fijo de la tarjeta

        alinear(lblTitulo, 40);
        alinear(lblSubtitulo, 22);
        alinear(txtUsuario, 44);
        alinear(txtPassword, 44);
        alinear(chkMostrar, 28);
        alinear(btnEntrar, 46);
        alinear(lblMensaje, 22);

        tarjeta.add(lblTitulo);
        tarjeta.add(lblSubtitulo);
        tarjeta.add(Box.createVerticalStrut(35));  // espacio en blanco
        tarjeta.add(txtUsuario);
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(txtPassword);
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(chkMostrar);
        tarjeta.add(Box.createVerticalStrut(18));
        tarjeta.add(btnEntrar);
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(lblMensaje);

        // El fondo gris ocupa toda la ventana y deja la tarjeta en el centro
        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(FONDO);
        fondo.add(tarjeta);
        setContentPane(fondo);

        // Al hacer clic en "Entrar" intentamos iniciar sesión
        btnEntrar.addActionListener(e -> intentarEntrar());

        // Así también funciona con la tecla Enter
        getRootPane().setDefaultButton(btnEntrar);

        // La ventana nunca puede ser más chica que la tarjeta
        setMinimumSize(new Dimension(520, 600));
        setSize(520, 600);
        setLocationRelativeTo(null);              // centra la ventana en la pantalla
        setExtendedState(JFrame.MAXIMIZED_BOTH);  // se abre ocupando toda la pantalla
    }

    // Deja un componente alineado a la izquierda, estirado a lo ancho y con su alto
    private void alinear(JComponent c, int alto) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        c.setPreferredSize(new Dimension(300, alto));
    }

    private void intentarEntrar() {
        // Leemos lo que escribió el usuario
        String username = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        // Se lo pasamos al controller y esperamos su respuesta
        var resultado = controller.iniciarSesion(username, password);

        if (resultado.exito()) {
            // Entró bien: abrimos la ventana principal con el menú de su rol
            // y cerramos el login
            new MenuPrincipalForm(resultado.usuario(), resultado.menu(), resultado.nombreRol()).setVisible(true);
            dispose();
            return;
        }

        // Si llegamos aquí es porque falló: mostramos el motivo en rojo
        lblMensaje.setForeground(ERROR);
        lblMensaje.setText(resultado.mensaje());

        // Por seguridad, borramos la contraseña del campo
        txtPassword.setText("");
    }

    // TEMPORAL: solo para probar la pantalla sola
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Activamos el estilo moderno ANTES de crear la ventana
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 12);      // esquinas redondeadas
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            new LoginForm().setVisible(true);
        });
    }
}
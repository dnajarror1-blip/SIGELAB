package sigelab.view;

import sigelab.controller.LoginController;
import sigelab.view.ui.UiTheme;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Formulario de acceso sin elección de rol; progreso publicado desde la autenticación real. */
public class LoginPanel extends JPanel {
    private final LoginController controller;
    private final Consumer<LoginController.ResultadoLogin> autenticado;
    private final JTextField usuario = new JTextField(26);
    private final JPasswordField password = new JPasswordField(26);
    private final JCheckBox mostrar = new JCheckBox("Mostrar contraseña");
    private final JButton entrar = new JButton("Iniciar sesión");
    private final JProgressBar progreso = new JProgressBar();
    private final JTextArea mensaje = new JTextArea(" ", 3, 28);
    private boolean ocupado;
    LoginPanel(LoginController controller, Consumer<LoginController.ResultadoLogin> autenticado) {
        super(new GridBagLayout());
        this.controller = controller; this.autenticado = autenticado;
        setBackground(UiTheme.FONDO);
        JPanel tarjeta = new JPanel(); tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(UiTheme.TARJETA); tarjeta.setBorder(BorderFactory.createCompoundBorder(UiTheme.bordeTarjeta(), BorderFactory.createEmptyBorder(25, 25, 25, 25)));
        JLabel titulo = new JLabel("SIGELAB"); titulo.setFont(UiTheme.TITULO);
        JLabel subtitulo = new JLabel("Sistema de Gestión de Laboratorios"); subtitulo.setFont(UiTheme.FUENTE_SECUNDARIA);
        JLabel ayuda = new JLabel("<html>El acceso y las funciones disponibles<br>dependen de tu rol.</html>"); ayuda.setForeground(UiTheme.SECUNDARIO);
        UiTheme.botonPrimario(entrar);
        mostrar.setOpaque(false);
        char echo = password.getEchoChar();
        mostrar.addActionListener(e -> password.setEchoChar(mostrar.isSelected() ? (char) 0 : echo));
        usuario.getAccessibleContext().setAccessibleName("Usuario"); password.getAccessibleContext().setAccessibleName("Contraseña");
        usuario.putClientProperty("JTextField.placeholderText", "Usuario"); password.putClientProperty("JTextField.placeholderText", "Contraseña");
        mensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        progreso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        mensaje.setEditable(false); mensaje.setLineWrap(true); mensaje.setWrapStyleWord(true);
        mensaje.setBackground(UiTheme.TARJETA); mensaje.setFont(UiTheme.FUENTE_SECUNDARIA);
        mensaje.getAccessibleContext().setAccessibleName("Estado del inicio de sesión");
        progreso.setIndeterminate(true); progreso.setVisible(false);
        for (Component componente : List.of(titulo, subtitulo, Box.createVerticalStrut(20), new JLabel("Usuario"), usuario,
                Box.createVerticalStrut(10), new JLabel("Contraseña"), password, mostrar, Box.createVerticalStrut(14), entrar,
                Box.createVerticalStrut(10), progreso, mensaje, Box.createVerticalStrut(10), ayuda)) {
            JComponent c = (JComponent) componente;
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (c == usuario || c == password || c == entrar) c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            tarjeta.add(c);
        }
        tarjeta.setPreferredSize(new Dimension(500, 500));
        add(tarjeta); entrar.addActionListener(e -> iniciar());
    }
    JButton botonEntrar() { return entrar; }
    private void ocupado(boolean valor) {
        ocupado = valor;
        entrar.setEnabled(!valor); usuario.setEnabled(!valor); password.setEnabled(!valor); mostrar.setEnabled(!valor);
        progreso.setVisible(valor);
    }
    private void iniciar() {
        if (ocupado) return;
        String nombre = usuario.getText().strip();
        char[] clave = password.getPassword(); password.setText("");
        if (nombre.isBlank() || clave.length == 0) { Arrays.fill(clave, '\0'); mensaje.setText("Ingresa usuario y contraseña."); return; }
        ocupado(true); mensaje.setForeground(UiTheme.SECUNDARIO); mensaje.setText("Validando credenciales...");
        new SwingWorker<LoginController.ResultadoLogin, String>() {
            protected LoginController.ResultadoLogin doInBackground() {
                try { return controller.iniciarSesion(nombre, new String(clave), () -> publish("Verificando identidad facial...")); }
                finally { Arrays.fill(clave, '\0'); }
            }
            protected void process(List<String> etapas) { mensaje.setText(etapas.getLast()); }
            protected void done() {
                try {
                    var resultado = get();
                    if (resultado.exito()) autenticado.accept(resultado);
                    else { mensaje.setForeground(new Color(185, 28, 28)); mensaje.setText(resultado.mensaje()); }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); mensaje.setText("Inicio de sesión interrumpido. Acceso denegado.");
                } catch (ExecutionException e) {
                    mensaje.setText("No fue posible completar el inicio de sesión. Intenta de nuevo.");
                } finally { ocupado(false); }
            }
        }.execute();
    }
}

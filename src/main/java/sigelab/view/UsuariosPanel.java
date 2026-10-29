package sigelab.view;

import sigelab.controller.UsuarioController;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Rol;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UsuariosPanel extends JPanel {

    // Colores, los mismos del resto de pantallas
    private static final Color TEXTO = new Color(17, 24, 39);
    private static final Color TEXTO_SUAVE = new Color(107, 114, 128);
    private static final Color ACENTO = new Color(37, 99, 235);
    private static final Color ERROR = new Color(220, 38, 38);
    private static final Color OK = new Color(22, 163, 74);
    private static final Color FONDO_FORM = new Color(249, 250, 251);
    private static final Color BORDE = new Color(229, 231, 235);

    // El controller sabe quién tiene la sesión abierta y revisa sus permisos
    private final UsuarioController controller;

    // La tabla y su contenido. El modelo es quien guarda las filas.
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nombre", "Usuario", "Correo", "Rol", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; // las celdas no se pueden escribir directo
        }
    };
    private final JTable tabla = new JTable(modelo);

    // Los usuarios que se están mostrando, en el mismo orden que las filas de la tabla
    private List<Usuario> usuarios = List.of();
    private List<Rol> roles = List.of();

    // Campos del formulario
    private final JTextField txtNombre = new JTextField();
    private final JTextField txtApellido = new JTextField();
    private final JTextField txtUsername = new JTextField();
    private final JTextField txtCorreo = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JComboBox<Rol> cmbRol = new JComboBox<>();

    private final JLabel lblMensaje = new JLabel(" ");

    public UsuariosPanel(Usuario usuarioActual) {
        this.controller = new UsuarioController(usuarioActual);

        cmbRol.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                           boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof Rol rol ? rol.getNombre() : "Selecciona un rol");
                return this;
            }
        });

        setLayout(new BorderLayout(25, 20));
        setOpaque(false);

        // ---------- Encabezado ----------
        JLabel titulo = new JLabel("Usuarios");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        titulo.setForeground(TEXTO);

        JLabel subtitulo = new JLabel("Crea usuarios y controla quién puede entrar al sistema.");
        subtitulo.setForeground(TEXTO_SUAVE);

        JPanel cabecera = new JPanel(new GridLayout(0, 1, 0, 6));
        cabecera.setOpaque(false);
        cabecera.add(titulo);
        cabecera.add(subtitulo);

        // ---------- Tabla (centro) ----------
        tabla.setRowHeight(34);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // una fila a la vez
        tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(tabla.getFont().deriveFont(Font.BOLD));

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));

        JButton btnCambiar = new JButton("Activar / Desactivar seleccionado");
        btnCambiar.setFocusPainted(false);
        btnCambiar.addActionListener(e -> alternarEstado());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botones.setOpaque(false);
        botones.add(btnCambiar);

        JPanel zonaTabla = new JPanel(new BorderLayout(0, 12));
        zonaTabla.setOpaque(false);
        zonaTabla.add(scroll, BorderLayout.CENTER);
        zonaTabla.add(botones, BorderLayout.SOUTH);

        // ---------- Formulario (derecha) ----------
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(FONDO_FORM);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)));
        form.setPreferredSize(new Dimension(300, 0));

        JLabel tituloForm = new JLabel("Nuevo usuario");
        tituloForm.setFont(tituloForm.getFont().deriveFont(Font.BOLD, 17f));
        tituloForm.setForeground(TEXTO);
        tituloForm.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtNombre.putClientProperty("JTextField.placeholderText", "Nombre");
        txtApellido.putClientProperty("JTextField.placeholderText", "Apellido");
        txtUsername.putClientProperty("JTextField.placeholderText", "Usuario");
        txtCorreo.putClientProperty("JTextField.placeholderText", "Correo");
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña (mínimo 6)");

        JButton btnCrear = new JButton("Crear usuario");
        btnCrear.setBackground(ACENTO);
        btnCrear.setForeground(Color.WHITE);
        btnCrear.setFont(btnCrear.getFont().deriveFont(Font.BOLD));
        btnCrear.setFocusPainted(false);
        btnCrear.addActionListener(e -> crear());

        form.add(tituloForm);
        form.add(Box.createVerticalStrut(16));
        for (JComponent campo : new JComponent[]{txtNombre, txtApellido, txtUsername,
                txtCorreo, txtPassword, cmbRol}) {
            campo.setAlignmentX(Component.LEFT_ALIGNMENT);
            campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            form.add(campo);
            form.add(Box.createVerticalStrut(10));
        }
        btnCrear.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCrear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(Box.createVerticalStrut(6));
        form.add(btnCrear);

        // ---------- Armamos el panel ----------
        add(cabecera, BorderLayout.NORTH);
        add(zonaTabla, BorderLayout.CENTER);
        add(form, BorderLayout.EAST);
        add(lblMensaje, BorderLayout.SOUTH);

        // Llenamos la tabla apenas se abre
        cargarRoles();
        cargarTabla();
    }

    private void cargarRoles() {
        var respuesta = controller.listarRoles();
        if (!respuesta.exito()) {
            mostrarMensaje(respuesta.mensaje(), true);
            return;
        }
        roles = respuesta.dato();
        cmbRol.removeAllItems();
        for (Rol rol : roles) {
            cmbRol.addItem(rol);
        }
        cmbRol.setSelectedIndex(-1);
    }

    // Pide los usuarios al controller y llena la tabla
    private void cargarTabla() {
        var respuesta = controller.listar();
        if (!respuesta.exito()) {
            mostrarMensaje(respuesta.mensaje(), true);
            return;
        }
        usuarios = respuesta.dato();

        modelo.setRowCount(0); // vaciamos la tabla
        for (Usuario u : usuarios) {
            modelo.addRow(new Object[]{
                    u.getIdUsuario(),
                    u.getNombre() + " " + u.getApellido(),
                    u.getUsername(),
                    u.getCorreo(),
                    nombreRol(u.getIdRol()),
                    u.getEstado()
            });
        }
    }

    // Se ejecuta al presionar "Crear usuario"
    private void crear() {
        Rol rol = (Rol) cmbRol.getSelectedItem();
        if (rol == null) {
            mostrarMensaje("Debe seleccionar un rol", true);
            return;
        }

        var respuesta = controller.crear(
                txtNombre.getText().trim(),
                txtApellido.getText().trim(),
                txtUsername.getText().trim(),
                new String(txtPassword.getPassword()),
                txtCorreo.getText().trim(),
                rol.getIdRol());

        mostrarMensaje(respuesta.mensaje(), !respuesta.exito());

        if (respuesta.exito()) {
            // Limpiamos el formulario y refrescamos la tabla
            txtNombre.setText("");
            txtApellido.setText("");
            txtUsername.setText("");
            txtCorreo.setText("");
            txtPassword.setText("");
            cargarTabla();
        }
    }

    // Se ejecuta al presionar "Activar / Desactivar seleccionado"
    private void alternarEstado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            mostrarMensaje("Primero selecciona un usuario de la tabla", true);
            return;
        }

        // Buscamos al usuario de esa fila y le ponemos el estado contrario
        Usuario u = usuarios.get(fila);
        EstadoUsuario nuevo = (u.getEstado() == EstadoUsuario.ACTIVO)
                ? EstadoUsuario.INACTIVO
                : EstadoUsuario.ACTIVO;

        var respuesta = controller.cambiarEstado(u.getIdUsuario(), nuevo);
        mostrarMensaje(respuesta.mensaje(), !respuesta.exito());
        cargarTabla();
    }

    // Muestra un mensaje pequeño abajo: rojo si es error, verde si salió bien
    private void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setForeground(esError ? ERROR : OK);
        lblMensaje.setText(texto);
    }

    private String nombreRol(int idRol) {
        return roles.stream().filter(rol -> rol.getIdRol() == idRol)
                .map(Rol::getNombre).findFirst().orElse("Rol no disponible");
    }
}

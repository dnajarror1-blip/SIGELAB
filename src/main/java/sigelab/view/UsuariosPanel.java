package sigelab.view;

import sigelab.controller.UsuarioController;
import sigelab.controller.FacialAdminController;
import sigelab.service.FacialAuthClient;
import sigelab.enums.EstadoUsuario;
import sigelab.model.Rol;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

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
    private final FacialAdminController facialController;
    private final JButton btnCambiar = new JButton("Activar / Desactivar seleccionado");
    private final JButton btnCrear = new JButton("Crear usuario");
    private final JButton btnEstadoFacial = new JButton("Estado facial");
    private final JButton btnRegistrarRostro = new JButton("Registrar rostro");
    private final JButton btnReemplazarRostro = new JButton("Reemplazar rostro");
    private final JButton btnEliminarRostro = new JButton("Eliminar rostro");
    private final JLabel lblEstadoFacial = new JLabel("Perfil facial: sin consultar");
    private boolean facialEnCurso;

    private enum AccionFacial { ESTADO, REGISTRAR, REEMPLAZAR, ELIMINAR }


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
        this(new UsuarioController(usuarioActual), new FacialAdminController(usuarioActual));
    }

    // Inyección de controladores para probar selección/asíncronía sin SQL ni HTTP.
    UsuariosPanel(UsuarioController controller, FacialAdminController facialController) {
        this.controller = Objects.requireNonNull(controller);
        this.facialController = Objects.requireNonNull(facialController);

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

        btnCambiar.setFocusPainted(false);
        btnCambiar.addActionListener(e -> alternarEstado());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botones.setOpaque(false);
        botones.add(btnCambiar);

        JPanel botonesFaciales = new JPanel(new GridLayout(0, 2, 8, 6));
        botonesFaciales.setOpaque(false);
        for (JButton boton : botonesFaciales()) {
            boton.setFocusPainted(false);
            botonesFaciales.add(boton);
        }
        btnEstadoFacial.addActionListener(e -> ejecutarAccionFacial(AccionFacial.ESTADO));
        btnRegistrarRostro.addActionListener(e -> ejecutarAccionFacial(AccionFacial.REGISTRAR));
        btnReemplazarRostro.addActionListener(e -> ejecutarAccionFacial(AccionFacial.REEMPLAZAR));
        btnEliminarRostro.addActionListener(e -> ejecutarAccionFacial(AccionFacial.ELIMINAR));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                lblEstadoFacial.setText("Perfil facial: sin consultar");
                actualizarAccionesFaciales();
            }
        });
        JPanel accionesTabla = new JPanel();
        accionesTabla.setLayout(new BoxLayout(accionesTabla, BoxLayout.Y_AXIS));
        accionesTabla.setOpaque(false);
        accionesTabla.add(botones);
        accionesTabla.add(Box.createVerticalStrut(8));
        accionesTabla.add(botonesFaciales);
        accionesTabla.add(Box.createVerticalStrut(6));
        accionesTabla.add(lblEstadoFacial);

        JPanel zonaTabla = new JPanel(new BorderLayout(0, 12));
        zonaTabla.setOpaque(false);
        zonaTabla.add(scroll, BorderLayout.CENTER);
        zonaTabla.add(accionesTabla, BorderLayout.SOUTH);

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
        actualizarAccionesFaciales();
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
        Usuario u = usuarioSeleccionado();
        if (u == null) {
            mostrarMensaje("Primero selecciona un usuario de la tabla", true);
            return;
        }

        // Buscamos al usuario de esa fila y le ponemos el estado contrario
        EstadoUsuario nuevo = (u.getEstado() == EstadoUsuario.ACTIVO)
                ? EstadoUsuario.INACTIVO
                : EstadoUsuario.ACTIVO;

        if (nuevo == EstadoUsuario.INACTIVO && JOptionPane.showConfirmDialog(this,
                "¿Desactivar a " + u.getNombre() + " " + u.getApellido() + "? No podrá iniciar sesión.",
                "Desactivar usuario", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        var respuesta = controller.cambiarEstado(u.getIdUsuario(), nuevo);
        mostrarMensaje(respuesta.mensaje(), !respuesta.exito());
        cargarTabla();
    }

    private Usuario usuarioSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return null;
        int indice = tabla.convertRowIndexToModel(fila);
        return indice >= 0 && indice < usuarios.size() ? usuarios.get(indice) : null;
    }

    private List<JButton> botonesFaciales() {
        return List.of(btnEstadoFacial, btnRegistrarRostro, btnReemplazarRostro, btnEliminarRostro);
    }

    private void actualizarAccionesFaciales() {
        boolean habilitar = !facialEnCurso && usuarioSeleccionado() != null;
        botonesFaciales().forEach(boton -> boton.setEnabled(habilitar));
    }

    // Sustituible en pruebas headless; en producción siempre solicita confirmación.
    boolean confirmarOperacionFacial(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Administración facial",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private record OperacionFacial(FacialAdminController.Resultado<?> resultado,
                                   FacialAdminController.Resultado<FacialAuthClient.EstadoPerfil> estado) { }

    private void ejecutarAccionFacial(AccionFacial accion) {
        if (facialEnCurso) return;
        Usuario objetivo = usuarioSeleccionado();
        if (objetivo == null || objetivo.getIdUsuario() == null || objetivo.getIdUsuario() <= 0) {
            mostrarMensaje("Primero selecciona un usuario válido de la tabla", true);
            return;
        }
        final int targetUserId = objetivo.getIdUsuario();
        if (accion == AccionFacial.REEMPLAZAR && !confirmarOperacionFacial(
                "¿Reemplazar el rostro del usuario #" + targetUserId + "? Se sustituirá su perfil facial.")) return;
        if (accion == AccionFacial.ELIMINAR && !confirmarOperacionFacial(
                "¿Eliminar el perfil facial del usuario #" + targetUserId
                        + "? No podrá iniciar sesión hasta registrar un nuevo rostro.")) return;

        facialEnCurso = true;
        actualizarAccionesFaciales();
        tabla.setEnabled(false);
        btnCambiar.setEnabled(false);
        btnCrear.setEnabled(false);
        lblEstadoFacial.setText("Perfil facial: consultando…");
        mostrarMensaje("Usuario #" + targetUserId + ": operación facial en curso…", false);
        new SwingWorker<OperacionFacial, Void>() {
            @Override
            protected OperacionFacial doInBackground() {
                if (accion == AccionFacial.ESTADO) {
                    var estado = facialController.consultarEstado(targetUserId);
                    return new OperacionFacial(estado, estado);
                }
                FacialAdminController.Resultado<?> resultado = switch (accion) {
                    case REGISTRAR -> facialController.registrarRostro(targetUserId);
                    case REEMPLAZAR -> facialController.reemplazarRostro(targetUserId);
                    case ELIMINAR -> facialController.eliminarRostro(targetUserId);
                    default -> throw new IllegalStateException("Acción facial no válida");
                };
                // Un fallo al refrescar no presenta como fallida una mutación ya confirmada.
                var estado = resultado.exito() ? facialController.consultarEstado(targetUserId) : null;
                return new OperacionFacial(resultado, estado);
            }

            @Override
            protected void done() {
                try {
                    var operacion = get();
                    mostrarMensaje("Usuario #" + targetUserId + ": " + operacion.resultado().mensaje(),
                            !operacion.resultado().exito());
                    var estado = operacion.estado();
                    if (estado != null && estado.exito()) {
                        var perfil = estado.dato();
                        lblEstadoFacial.setText("Usuario #" + targetUserId + " — Perfil facial: "
                                + (perfil.registrado() ? "registrado" : "no registrado")
                                + "; " + (perfil.activo() ? "activo" : "inactivo"));
                    } else {
                        lblEstadoFacial.setText("Perfil facial: estado no disponible. Consulta Estado facial para actualizarlo.");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    mostrarMensaje("La operación facial fue interrumpida. Consulta el estado antes de reintentar.", true);
                    lblEstadoFacial.setText("Perfil facial: estado no confirmado");
                } catch (ExecutionException error) {
                    mostrarMensaje("No fue posible completar la operación facial. Consulta el estado antes de reintentar.", true);
                    lblEstadoFacial.setText("Perfil facial: estado no confirmado");
                } finally {
                    facialEnCurso = false;
                    tabla.setEnabled(true);
                    btnCambiar.setEnabled(true);
                    btnCrear.setEnabled(true);
                    actualizarAccionesFaciales();
                }
            }
        }.execute();
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

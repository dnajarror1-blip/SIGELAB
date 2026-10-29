package sigelab.view;

import sigelab.controller.NotificacionController;
import sigelab.enums.TipoNotificacion;
import sigelab.model.Notificacion;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificacionesPanel extends JPanel {

    // Colores, los mismos del resto de pantallas
    private static final Color TEXTO = new Color(17, 24, 39);
    private static final Color TEXTO_SUAVE = new Color(107, 114, 128);
    private static final Color ERROR = new Color(220, 38, 38);
    private static final Color OK = new Color(22, 163, 74);
    private static final Color BORDE = new Color(229, 231, 235);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionController controller;

    // Se ejecuta cada vez que cambia algo (por ejemplo, para actualizar el numerito del menú)
    private final Runnable alCambiar;

    // La tabla y su contenido. El modelo es quien guarda las filas.
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Estado", "Tipo", "Título", "Mensaje", "Fecha"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; // las celdas no se pueden escribir directo
        }
    };
    private final JTable tabla = new JTable(modelo);

    // Las notificaciones que se están mostrando, en el mismo orden que las filas
    private List<Notificacion> notificaciones = List.of();

    private final JLabel subtitulo = new JLabel(" ");
    private final JLabel lblMensaje = new JLabel(" ");

    public NotificacionesPanel(Usuario usuarioActual) {
        this(usuarioActual, () -> { });
    }

    public NotificacionesPanel(Usuario usuarioActual, Runnable alCambiar) {
        this.controller = new NotificacionController(usuarioActual);
        this.alCambiar = alCambiar;

        setLayout(new BorderLayout(25, 20));
        setOpaque(false);

        // ---------- Encabezado ----------
        JLabel titulo = new JLabel("Notificaciones");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        titulo.setForeground(TEXTO);

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

        // Anchos de cada columna; el mensaje es el que más espacio necesita
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(380);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(130);

        // Las notificaciones sin leer se ven en negrita para que resalten
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor,
                                                           boolean seleccionada, boolean foco,
                                                           int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                int indice = t.convertRowIndexToModel(fila);
                boolean sinLeer = indice < notificaciones.size() && !notificaciones.get(indice).isLeida();
                c.setFont(t.getFont().deriveFont(sinLeer ? Font.BOLD : Font.PLAIN));
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));

        JButton btnLeida = new JButton("Marcar como leída");
        btnLeida.setFocusPainted(false);
        btnLeida.addActionListener(e -> marcarSeleccionada());

        JButton btnTodas = new JButton("Marcar todas como leídas");
        btnTodas.setFocusPainted(false);
        btnTodas.addActionListener(e -> marcarTodas());

        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.setFocusPainted(false);
        btnActualizar.addActionListener(e -> {
            cargarTabla();
            mostrarMensaje(" ", false);
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);
        botones.add(btnLeida);
        botones.add(btnTodas);
        botones.add(btnActualizar);

        JPanel zonaTabla = new JPanel(new BorderLayout(0, 12));
        zonaTabla.setOpaque(false);
        zonaTabla.add(scroll, BorderLayout.CENTER);
        zonaTabla.add(botones, BorderLayout.SOUTH);

        // ---------- Armamos el panel ----------
        add(cabecera, BorderLayout.NORTH);
        add(zonaTabla, BorderLayout.CENTER);
        add(lblMensaje, BorderLayout.SOUTH);

        // Llenamos la tabla apenas se abre
        cargarTabla();
    }

    // Pide las notificaciones al controller y llena la tabla
    private void cargarTabla() {
        var respuesta = controller.listar();
        if (!respuesta.exito()) {
            mostrarMensaje(respuesta.mensaje(), true);
            return;
        }
        notificaciones = respuesta.dato();

        modelo.setRowCount(0); // vaciamos la tabla
        for (Notificacion n : notificaciones) {
            modelo.addRow(new Object[]{
                    n.isLeida() ? "Leída" : "Nueva",
                    nombreTipo(n.getTipo()),
                    n.getTitulo(),
                    n.getMensaje(),
                    n.getFechaHora() != null ? n.getFechaHora().format(FORMATO_FECHA) : ""
            });
        }
        actualizarSubtitulo();
    }

    // Escribe debajo del título cuántas quedan sin leer
    private void actualizarSubtitulo() {
        var respuesta = controller.contarNoLeidas();
        if (!respuesta.exito()) {
            subtitulo.setText(" ");
            return;
        }
        int cuantas = respuesta.dato();
        if (cuantas == 0) {
            subtitulo.setText("No tienes notificaciones sin leer.");
        } else if (cuantas == 1) {
            subtitulo.setText("Tienes 1 notificación sin leer.");
        } else {
            subtitulo.setText("Tienes " + cuantas + " notificaciones sin leer.");
        }
    }

    // Se ejecuta al presionar "Marcar como leída"
    private void marcarSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            mostrarMensaje("Primero selecciona una notificación de la tabla", true);
            return;
        }

        Notificacion elegida = notificaciones.get(tabla.convertRowIndexToModel(fila));
        var respuesta = controller.marcarComoLeida(elegida.getIdNotificacion());
        mostrarMensaje(respuesta.mensaje(), !respuesta.exito());

        if (respuesta.exito()) {
            cargarTabla();
            alCambiar.run();
        }
    }

    // Se ejecuta al presionar "Marcar todas como leídas"
    private void marcarTodas() {
        var respuesta = controller.marcarTodasComoLeidas();
        mostrarMensaje(respuesta.mensaje(), !respuesta.exito());

        if (respuesta.exito()) {
            cargarTabla();
            alCambiar.run();
        }
    }

    // Muestra un mensaje pequeño abajo: rojo si es error, verde si salió bien
    private void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setForeground(esError ? ERROR : OK);
        lblMensaje.setText(texto);
    }

    // Nombres legibles para mostrar en la tabla
    private String nombreTipo(TipoNotificacion tipo) {
        return switch (tipo) {
            case RESERVA_CREADA -> "Reserva creada";
            case RESERVA_CANCELADA -> "Reserva cancelada";
            case RESERVA_COMPLETADA -> "Reserva completada";
            case PRESTAMO_VENCIDO -> "Préstamo vencido";
            case SISTEMA -> "Sistema";
        };
    }
}
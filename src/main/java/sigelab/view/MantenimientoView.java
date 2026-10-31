package sigelab.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import sigelab.controller.CatalogoSeleccionController;
import sigelab.service.CatalogoSeleccionService.EquipoOpcion;
import sigelab.service.Modulo;
import sigelab.view.ui.UiTheme;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import sigelab.enums.EstadoMantenimiento;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Mantenimiento;
import sigelab.service.MantenimientoService;
import sigelab.service.AuditService;
import sigelab.controller.MantenimientoController;

public class MantenimientoView extends JPanel {

    private final MantenimientoController mantenimientoService;

    private final CatalogoSeleccionController catalogos;
    private final JLabel estadoCarga = new JLabel(" ");
    private JTable tablaMantenimientos;
    private DefaultTableModel modeloTabla;
    private JComboBox<String> comboFiltroEstado;
    private JButton btnNuevoIngreso;
    private JButton btnFinalizar;
    private JButton btnRefrescar;

    public MantenimientoView(int idUsuarioActual) {
        this(new MantenimientoService(), idUsuarioActual);
    }

    public MantenimientoView(MantenimientoService mantenimientoService, int idUsuarioActual) {
        this(new MantenimientoController(mantenimientoService, new AuditService(), idUsuarioActual),
                new CatalogoSeleccionController(idUsuarioActual, Modulo.MANTENIMIENTO));
    }

    MantenimientoView(MantenimientoController controller, CatalogoSeleccionController catalogos) {
        this.catalogos = catalogos;
        this.mantenimientoService = controller;
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel Superior (Titulo y Filtros)
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        JLabel lblTitulo = new JLabel("Gestión de Mantenimiento Técnico (Taller)");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(18f));
        panelSuperior.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelFiltros.add(new JLabel("Filtrar por estado:"));
        comboFiltroEstado = new JComboBox<>(new String[]{"TODOS", "ABIERTO (En Taller)", "CERRADO (Finalizado)"});
        comboFiltroEstado.addActionListener(e -> cargarDatos());
        panelFiltros.add(comboFiltroEstado);

        btnRefrescar = new JButton("Actualizar");
        btnRefrescar.addActionListener(e -> cargarDatos());
        panelFiltros.add(btnRefrescar);

        panelSuperior.add(panelFiltros, BorderLayout.SOUTH);
        add(panelSuperior, BorderLayout.NORTH);

        // Panel Central (Tabla)
        String[] columnas = {"Número", "Equipo", "Fecha Ingreso", "Tipo", "Descripción", "Fecha Salida", "Resultado", "Estado", "Observaciones"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaMantenimientos = new JTable(modeloTabla);
        tablaMantenimientos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiTheme.tabla(tablaMantenimientos);
        tablaMantenimientos.setAutoCreateRowSorter(true);
        JScrollPane scrollTabla = new JScrollPane(tablaMantenimientos);
        add(scrollTabla, BorderLayout.CENTER);

        // Panel Inferior (Acciones)
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        btnNuevoIngreso = new JButton("Ingresar Equipo a Taller");
        btnNuevoIngreso.addActionListener(e -> mostrarDialogoIngreso());
        panelAcciones.add(btnNuevoIngreso);

        btnFinalizar = new JButton("Finalizar Mantenimiento Seleccionado");
        btnFinalizar.addActionListener(e -> mostrarDialogoFinalizacion());
        panelAcciones.add(btnFinalizar);
        panelAcciones.add(estadoCarga);

        add(panelAcciones, BorderLayout.SOUTH);
    }

    public void cargarDatos() {
        if (!btnRefrescar.isEnabled()) return;
        int filtroIndex = comboFiltroEstado.getSelectedIndex();
        btnRefrescar.setEnabled(false);
        comboFiltroEstado.setEnabled(false);
        btnNuevoIngreso.setEnabled(false);
        btnFinalizar.setEnabled(false);
        estadoCarga.setText("Cargando...");
        OperacionModulo.ejecutar(this, () -> {
            List<Mantenimiento> lista = filtroIndex == 1 ? mantenimientoService.listarMantenimientosActivos() : mantenimientoService.listarTodos();
            if (filtroIndex == 2) lista = lista.stream().filter(m -> m.getEstado() == EstadoMantenimiento.CERRADO).toList();
            return new Datos(lista, catalogos.listarEquipos(false).stream().collect(Collectors.toMap(e -> e.equipo().getIdEquipo(), Function.identity())));
        }, datos -> {
            modeloTabla.setRowCount(0);
            for (Mantenimiento m : datos.lista()) {
                EquipoOpcion equipo = datos.equipos().get(m.getIdEquipo());
                modeloTabla.addRow(new Object[]{m.getIdMantenimiento(), equipo == null ? "Equipo no disponible" : equipo,
                        m.getFechaIngreso(), m.getTipo(), m.getDescripcion(),
                        m.getFechaSalida(), m.getResultado(), m.getEstado(), m.getObservaciones()});
            }
            estadoCarga.setText(datos.lista().isEmpty() ? "No existen mantenimientos para este filtro." : datos.lista().size() + " mantenimientos.");
        }, () -> {
            btnRefrescar.setEnabled(true); comboFiltroEstado.setEnabled(true);
            btnNuevoIngreso.setEnabled(true); btnFinalizar.setEnabled(true);
            if ("Cargando...".equals(estadoCarga.getText())) estadoCarga.setText("No se pudo cargar. Pulsa Actualizar.");
        });
    }
    private record Datos(List<Mantenimiento> lista, Map<Integer, EquipoOpcion> equipos) { }

    private void mostrarDialogoIngreso() {
        JDialog dialog = new JDialog((Frame) null, "Ingreso de Equipo a Mantenimiento", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(640, 540);
        dialog.setLocationRelativeTo(this);

        IngresoMantenimientoPanel panelForm = new IngresoMantenimientoPanel();
        panelForm.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        dialog.add(panelForm, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Registrar Ingreso");
        JButton btnCancelar = new JButton("Cancelar");

        btnGuardar.addActionListener(e -> {
            try {
                EquipoOpcion seleccionado = panelForm.equipo.seleccionado();
                if (seleccionado == null) {
                    JOptionPane.showMessageDialog(dialog, "Selecciona un equipo de la lista."); return;
                }
                int idEquipo = seleccionado.equipo().getIdEquipo();
                TipoMantenimiento tipo = (TipoMantenimiento) panelForm.tipo.getSelectedItem();
                String desc = panelForm.descripcion.getText().trim();
                String obs = panelForm.observaciones.getText().trim();

                btnGuardar.setEnabled(false);
                btnCancelar.setEnabled(false);
                panelForm.equipo.setEnabled(false); panelForm.tipo.setEnabled(false);
                panelForm.descripcion.setEnabled(false); panelForm.observaciones.setEnabled(false);
                OperacionModulo.ejecutar(dialog,
                        () -> mantenimientoService.ingresarATaller(idEquipo, tipo, desc, obs), guardado -> {
                    JOptionPane.showMessageDialog(dialog, "Equipo ingresado exitosamente al taller.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    cargarDatos();
                }, () -> {
                    btnGuardar.setEnabled(true); btnCancelar.setEnabled(true);
                    panelForm.equipo.setEnabled(true); panelForm.tipo.setEnabled(true);
                    panelForm.descripcion.setEnabled(true); panelForm.observaciones.setEnabled(true);
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error al ingresar", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { btnCancelar.doClick(); }
        });
        btnCancelar.addActionListener(e -> {
            if (btnCancelar.isEnabled() && JOptionPane.showConfirmDialog(dialog, "¿Cancelar esta operación?",
                    "Cancelar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) dialog.dispose();
        });

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);
        dialog.add(panelBotones, BorderLayout.SOUTH);

        btnGuardar.setEnabled(false);
        panelForm.equipo.setEnabled(false);
        btnGuardar.setText("Cargando...");
        OperacionModulo.ejecutar(dialog, () -> catalogos.listarEquipos(false), lista -> {
            panelForm.equipos(lista); panelForm.equipo.setEnabled(true); btnGuardar.setEnabled(true);
        }, () -> btnGuardar.setText("Registrar ingreso"));
        dialog.setVisible(true);
    }

    private void mostrarDialogoFinalizacion() {
        int fila = tablaMantenimientos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un mantenimiento de la tabla para finalizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        fila = tablaMantenimientos.convertRowIndexToModel(fila);
        int idMantenimiento = (int) modeloTabla.getValueAt(fila, 0);
        EstadoMantenimiento estado = (EstadoMantenimiento) modeloTabla.getValueAt(fila, 7);

        if (estado == EstadoMantenimiento.CERRADO) {
            JOptionPane.showMessageDialog(this, "Este mantenimiento ya está cerrado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) null, "Finalizar mantenimiento #" + idMantenimiento, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 260);
        dialog.setLocationRelativeTo(this);

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<ResultadoMantenimiento> comboResultado = new JComboBox<>(ResultadoMantenimiento.values());
        JTextArea txtObservacionesFinales = new JTextArea(3, 15);
        txtObservacionesFinales.setLineWrap(true);

        gbc.gridx = 0; gbc.gridy = 0;
        panelForm.add(new JLabel("Resultado Técnico:"), gbc);
        gbc.gridx = 1;
        panelForm.add(comboResultado, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelForm.add(new JLabel("Detalle de Reparación:"), gbc);
        gbc.gridx = 1;
        panelForm.add(new JScrollPane(txtObservacionesFinales), gbc);

        dialog.add(panelForm, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Concluir Mantenimiento");
        JButton btnCancelar = new JButton("Cancelar");

        btnGuardar.addActionListener(e -> {
            try {
                ResultadoMantenimiento resultado = (ResultadoMantenimiento) comboResultado.getSelectedItem();
                String obs = txtObservacionesFinales.getText().trim();

                if (JOptionPane.showConfirmDialog(dialog, "¿Finalizar el mantenimiento con el resultado seleccionado?",
                        "Finalizar mantenimiento", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                btnGuardar.setEnabled(false);
                btnCancelar.setEnabled(false);
                OperacionModulo.ejecutar(dialog, () -> {
                    mantenimientoService.finalizarMantenimiento(idMantenimiento, resultado, obs);
                    return null;
                }, ignorado -> {
                    JOptionPane.showMessageDialog(dialog, "Mantenimiento finalizado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    cargarDatos();
                }, () -> { btnGuardar.setEnabled(true); btnCancelar.setEnabled(true); });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error al finalizar", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { btnCancelar.doClick(); }
        });
        btnCancelar.addActionListener(e -> {
            if (btnCancelar.isEnabled() && JOptionPane.showConfirmDialog(dialog, "¿Cancelar esta operación?",
                    "Cancelar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) dialog.dispose();
        });

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);
        dialog.add(panelBotones, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}

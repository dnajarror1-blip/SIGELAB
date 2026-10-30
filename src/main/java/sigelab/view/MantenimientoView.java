package sigelab.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
        this(new MantenimientoController(mantenimientoService, new AuditService(), idUsuarioActual));
    }

    MantenimientoView(MantenimientoController controller) {
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
        panelSuperior.add(lblTitulo, BorderLayout.WEST);

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelFiltros.add(new JLabel("Filtrar por estado:"));
        comboFiltroEstado = new JComboBox<>(new String[]{"TODOS", "ABIERTO (En Taller)", "CERRADO (Finalizado)"});
        comboFiltroEstado.addActionListener(e -> cargarDatos());
        panelFiltros.add(comboFiltroEstado);

        btnRefrescar = new JButton("Actualizar");
        btnRefrescar.addActionListener(e -> cargarDatos());
        panelFiltros.add(btnRefrescar);

        panelSuperior.add(panelFiltros, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // Panel Central (Tabla)
        String[] columnas = {"ID", "ID Equipo", "Fecha Ingreso", "Tipo", "Descripción", "Responsable", "Fecha Salida", "Resultado", "Estado", "Observaciones"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaMantenimientos = new JTable(modeloTabla);
        tablaMantenimientos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaMantenimientos.setRowHeight(24);
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

        add(panelAcciones, BorderLayout.SOUTH);
    }

    public void cargarDatos() {
        if (!btnRefrescar.isEnabled()) return;
        int filtroIndex = comboFiltroEstado.getSelectedIndex();
        btnRefrescar.setEnabled(false);
        comboFiltroEstado.setEnabled(false);
        OperacionModulo.ejecutar(this, () -> {
            if (filtroIndex == 1) return mantenimientoService.listarMantenimientosActivos();
            List<Mantenimiento> lista = mantenimientoService.listarTodos();
            return filtroIndex == 2 ? lista.stream()
                    .filter(m -> m.getEstado() == EstadoMantenimiento.CERRADO).toList() : lista;
        }, lista -> {
            modeloTabla.setRowCount(0);
            for (Mantenimiento m : lista) {
                modeloTabla.addRow(new Object[]{m.getIdMantenimiento(), m.getIdEquipo(),
                        m.getFechaIngreso(), m.getTipo(), m.getDescripcion(), m.getIdResponsable(),
                        m.getFechaSalida(), m.getResultado(), m.getEstado(), m.getObservaciones()});
            }
        }, () -> {
            btnRefrescar.setEnabled(true);
            comboFiltroEstado.setEnabled(true);
        });
    }

    private void mostrarDialogoIngreso() {
        JDialog dialog = new JDialog((Frame) null, "Ingreso de Equipo a Mantenimiento", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 320);
        dialog.setLocationRelativeTo(this);

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtIdEquipo = new JTextField(15);
        JComboBox<TipoMantenimiento> comboTipo = new JComboBox<>(TipoMantenimiento.values());
        JTextField txtDescripcion = new JTextField(15);
        JTextArea txtObservaciones = new JTextArea(3, 15);
        txtObservaciones.setLineWrap(true);

        gbc.gridx = 0; gbc.gridy = 0;
        panelForm.add(new JLabel("ID Equipo:"), gbc);
        gbc.gridx = 1;
        panelForm.add(txtIdEquipo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelForm.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        panelForm.add(comboTipo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panelForm.add(new JLabel("Diagnóstico / Motivo:"), gbc);
        gbc.gridx = 1;
        panelForm.add(txtDescripcion, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panelForm.add(new JLabel("Observaciones:"), gbc);
        gbc.gridx = 1;
        panelForm.add(new JScrollPane(txtObservaciones), gbc);

        dialog.add(panelForm, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Registrar Ingreso");
        JButton btnCancelar = new JButton("Cancelar");

        btnGuardar.addActionListener(e -> {
            try {
                int idEquipo = Integer.parseInt(txtIdEquipo.getText().trim());
                TipoMantenimiento tipo = (TipoMantenimiento) comboTipo.getSelectedItem();
                String desc = txtDescripcion.getText().trim();
                String obs = txtObservaciones.getText().trim();

                btnGuardar.setEnabled(false);
                OperacionModulo.ejecutar(dialog,
                        () -> mantenimientoService.ingresarATaller(idEquipo, tipo, desc, obs), guardado -> {
                    JOptionPane.showMessageDialog(dialog, "Equipo ingresado exitosamente al taller.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    cargarDatos();
                }, () -> btnGuardar.setEnabled(true));
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "El ID de equipo debe ser un número entero.", "Validación", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error al ingresar", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);
        dialog.add(panelBotones, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void mostrarDialogoFinalizacion() {
        int fila = tablaMantenimientos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un mantenimiento de la tabla para finalizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idMantenimiento = (int) modeloTabla.getValueAt(fila, 0);
        EstadoMantenimiento estado = (EstadoMantenimiento) modeloTabla.getValueAt(fila, 8);

        if (estado == EstadoMantenimiento.CERRADO) {
            JOptionPane.showMessageDialog(this, "Este mantenimiento ya está cerrado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) null, "Finalizar Mantenimiento ID #" + idMantenimiento, true);
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

                btnGuardar.setEnabled(false);
                OperacionModulo.ejecutar(dialog, () -> {
                    mantenimientoService.finalizarMantenimiento(idMantenimiento, resultado, obs);
                    return null;
                }, ignorado -> {
                    JOptionPane.showMessageDialog(dialog, "Mantenimiento finalizado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    cargarDatos();
                }, () -> btnGuardar.setEnabled(true));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error al finalizar", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);
        dialog.add(panelBotones, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}

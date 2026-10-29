package sigelab.view;

import sigelab.controller.EquipoController;
import sigelab.enums.EstadoEquipo;
import sigelab.model.Equipo;
import sigelab.model.Laboratorio;
import sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EquipoView extends JFrame {

    private final EquipoController controller;

    private JTextField txtCodigoInventario;
    private JTextField txtNombre;
    private JComboBox<TipoEquipo> cmbTipoEquipo;
    private JTextField txtMarca;
    private JTextField txtModelo;
    private JTextField txtNumeroSerie;
    private JComboBox<Laboratorio> cmbLaboratorio;
    private JTextField txtEstado;
    private JTextArea txtObservaciones;

    private JTable tablaEquipos;
    private DefaultTableModel modeloTabla;

    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnEliminar;
    private JButton btnLimpiar;

    private Integer idSeleccionado;

    private List<Equipo> equiposActuales =
            new ArrayList<>();


    public EquipoView() {

        controller =
                new EquipoController();

        configurarVentana();
        crearComponentes();
        cargarCatalogos();
        cargarEquipos();
    }


    private void configurarVentana() {

        setTitle(
                "SIGELAB - Equipos"
        );

        setSize(
                1200,
                700
        );

        setLocationRelativeTo(
                null
        );

        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        setLayout(
                new BorderLayout(
                        10,
                        10
                )
        );
    }


    private void crearComponentes() {

        JPanel panelFormulario =
                new JPanel(
                        new GridBagLayout()
                );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        4,
                        4,
                        4,
                        4
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;


        txtCodigoInventario =
                new JTextField(20);

        txtNombre =
                new JTextField(20);

        cmbTipoEquipo =
                new JComboBox<>();

        txtMarca =
                new JTextField(20);

        txtModelo =
                new JTextField(20);

        txtNumeroSerie =
                new JTextField(20);

        cmbLaboratorio =
                new JComboBox<>();

        txtEstado =
                new JTextField(20);

        // El estado no se modifica manualmente.
        txtEstado.setEditable(false);
        txtEstado.setText(
                EstadoEquipo.DISPONIBLE.name()
        );


        txtObservaciones =
                new JTextArea(
                        3,
                        20
                );

        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);


        agregarCampo(
                panelFormulario,
                gbc,
                0,
                "Código inventario:",
                txtCodigoInventario
        );

        agregarCampo(
                panelFormulario,
                gbc,
                1,
                "Nombre:",
                txtNombre
        );

        agregarCampo(
                panelFormulario,
                gbc,
                2,
                "Tipo de equipo:",
                cmbTipoEquipo
        );

        agregarCampo(
                panelFormulario,
                gbc,
                3,
                "Marca:",
                txtMarca
        );

        agregarCampo(
                panelFormulario,
                gbc,
                4,
                "Modelo:",
                txtModelo
        );

        agregarCampo(
                panelFormulario,
                gbc,
                5,
                "Número de serie:",
                txtNumeroSerie
        );

        agregarCampo(
                panelFormulario,
                gbc,
                6,
                "Laboratorio:",
                cmbLaboratorio
        );

        agregarCampo(
                panelFormulario,
                gbc,
                7,
                "Estado:",
                txtEstado
        );


        gbc.gridx = 0;
        gbc.gridy = 8;

        panelFormulario.add(
                new JLabel("Observaciones:"),
                gbc
        );


        gbc.gridx = 1;

        panelFormulario.add(
                new JScrollPane(
                        txtObservaciones
                ),
                gbc
        );


        configurarRenderers();


        JPanel panelBotones =
                new JPanel();


        btnGuardar =
                new JButton("Guardar");

        btnActualizar =
                new JButton("Actualizar");

        btnEliminar =
                new JButton("Eliminar");

        btnLimpiar =
                new JButton("Limpiar");


        panelBotones.add(btnGuardar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);


        String[] columnas = {
                "ID",
                "Código",
                "Nombre",
                "Tipo",
                "Marca",
                "Modelo",
                "Serie",
                "Laboratorio",
                "Estado",
                "Observaciones"
        };


        modeloTabla =
                new DefaultTableModel(
                        columnas,
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {
                        return false;
                    }
                };


        tablaEquipos =
                new JTable(
                        modeloTabla
                );


        tablaEquipos.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );


        JScrollPane scrollTabla =
                new JScrollPane(
                        tablaEquipos
                );


        JPanel panelSuperior =
                new JPanel(
                        new BorderLayout()
                );


        panelSuperior.add(
                panelFormulario,
                BorderLayout.CENTER
        );


        panelSuperior.add(
                panelBotones,
                BorderLayout.SOUTH
        );


        add(
                panelSuperior,
                BorderLayout.NORTH
        );


        add(
                scrollTabla,
                BorderLayout.CENTER
        );


        configurarEventos();
    }


    private void configurarRenderers() {

        // Muestra solamente el nombre del tipo.
        cmbTipoEquipo.setRenderer(
                new DefaultListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {

                        super.getListCellRendererComponent(
                                list,
                                value,
                                index,
                                isSelected,
                                cellHasFocus
                        );


                        if (value instanceof TipoEquipo tipo) {
                            setText(
                                    tipo.getNombre()
                            );
                        }

                        return this;
                    }
                }
        );


        // Muestra código y nombre del laboratorio.
        cmbLaboratorio.setRenderer(
                new DefaultListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {

                        super.getListCellRendererComponent(
                                list,
                                value,
                                index,
                                isSelected,
                                cellHasFocus
                        );


                        if (value instanceof Laboratorio laboratorio) {

                            setText(
                                    laboratorio.getCodigo()
                                            + " - "
                                            + laboratorio.getNombre()
                            );
                        }

                        return this;
                    }
                }
        );
    }


    private void agregarCampo(
            JPanel panel,
            GridBagConstraints gbc,
            int fila,
            String texto,
            JComponent componente
    ) {

        gbc.gridx = 0;
        gbc.gridy = fila;

        panel.add(
                new JLabel(texto),
                gbc
        );


        gbc.gridx = 1;

        panel.add(
                componente,
                gbc
        );
    }


    private void configurarEventos() {

        btnGuardar.addActionListener(
                e -> guardar()
        );


        btnActualizar.addActionListener(
                e -> actualizar()
        );


        btnEliminar.addActionListener(
                e -> eliminar()
        );


        btnLimpiar.addActionListener(
                e -> limpiarFormulario()
        );


        tablaEquipos
                .getSelectionModel()
                .addListSelectionListener(
                        e -> {

                            if (!e.getValueIsAdjusting()) {
                                cargarSeleccion();
                            }
                        }
                );
    }


    // Carga TipoEquipo y Laboratorio en los ComboBox.
    private void cargarCatalogos() {

        try {

            cmbTipoEquipo.removeAllItems();

            for (TipoEquipo tipo
                    : controller.listarTiposEquipo()) {

                cmbTipoEquipo.addItem(
                        tipo
                );
            }


            cmbLaboratorio.removeAllItems();

            for (Laboratorio laboratorio
                    : controller.listarLaboratorios()) {

                cmbLaboratorio.addItem(
                        laboratorio
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            mostrarError(
                    e.getMessage()
            );
        }
    }


    private void guardar() {

        TipoEquipo tipo =
                (TipoEquipo)
                        cmbTipoEquipo
                                .getSelectedItem();

        Laboratorio laboratorio =
                (Laboratorio)
                        cmbLaboratorio
                                .getSelectedItem();


        if (tipo == null) {

            mostrarError(
                    "Debe seleccionar un tipo de equipo."
            );

            return;
        }


        if (laboratorio == null) {

            mostrarError(
                    "Debe seleccionar un laboratorio."
            );

            return;
        }


        try {

            controller.crear(
                    txtCodigoInventario.getText(),
                    txtNombre.getText(),
                    tipo.getIdTipoEquipo(),
                    txtMarca.getText(),
                    txtModelo.getText(),
                    txtNumeroSerie.getText(),
                    laboratorio.getIdLaboratorio(),
                    txtObservaciones.getText()
            );


            JOptionPane.showMessageDialog(
                    this,
                    "Equipo registrado correctamente."
            );


            cargarEquipos();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    private void actualizar() {

        if (idSeleccionado == null) {

            mostrarError(
                    "Debe seleccionar un equipo."
            );

            return;
        }


        TipoEquipo tipo =
                (TipoEquipo)
                        cmbTipoEquipo
                                .getSelectedItem();

        Laboratorio laboratorio =
                (Laboratorio)
                        cmbLaboratorio
                                .getSelectedItem();


        if (tipo == null
                || laboratorio == null) {

            mostrarError(
                    "Debe seleccionar tipo de equipo y laboratorio."
            );

            return;
        }


        try {

            controller.actualizar(
                    idSeleccionado,
                    txtCodigoInventario.getText(),
                    txtNombre.getText(),
                    tipo.getIdTipoEquipo(),
                    txtMarca.getText(),
                    txtModelo.getText(),
                    txtNumeroSerie.getText(),
                    laboratorio.getIdLaboratorio(),
                    txtObservaciones.getText()
            );


            JOptionPane.showMessageDialog(
                    this,
                    "Equipo actualizado correctamente."
            );


            cargarEquipos();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    private void eliminar() {

        if (idSeleccionado == null) {

            mostrarError(
                    "Debe seleccionar un equipo."
            );

            return;
        }


        int respuesta =
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Desea eliminar el equipo seleccionado?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION
                );


        if (respuesta
                != JOptionPane.YES_OPTION) {

            return;
        }


        try {

            controller.eliminar(
                    idSeleccionado
            );


            JOptionPane.showMessageDialog(
                    this,
                    "Equipo eliminado correctamente."
            );


            cargarEquipos();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    private void cargarEquipos() {

        modeloTabla.setRowCount(0);

        try {

            equiposActuales =
                    controller.listar();


            for (Equipo equipo
                    : equiposActuales) {

                modeloTabla.addRow(
                        new Object[]{
                                equipo.getIdEquipo(),
                                equipo.getCodigoInventario(),
                                equipo.getNombre(),
                                buscarNombreTipo(
                                        equipo.getIdTipoEquipo()
                                ),
                                equipo.getMarca(),
                                equipo.getModelo(),
                                equipo.getNumeroSerie(),
                                buscarNombreLaboratorio(
                                        equipo.getIdLaboratorio()
                                ),
                                equipo.getEstado(),
                                equipo.getObservaciones()
                        }
                );
            }

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    private void cargarSeleccion() {

        int filaVista =
                tablaEquipos.getSelectedRow();


        if (filaVista == -1) {
            return;
        }


        int filaModelo =
                tablaEquipos.convertRowIndexToModel(
                        filaVista
                );


        Equipo equipo =
                equiposActuales.get(
                        filaModelo
                );


        idSeleccionado =
                equipo.getIdEquipo();


        txtCodigoInventario.setText(
                equipo.getCodigoInventario()
        );

        txtNombre.setText(
                equipo.getNombre()
        );

        txtMarca.setText(
                equipo.getMarca()
        );

        txtModelo.setText(
                equipo.getModelo()
        );

        txtNumeroSerie.setText(
                equipo.getNumeroSerie()
        );

        txtEstado.setText(
                equipo.getEstado().name()
        );

        txtObservaciones.setText(
                equipo.getObservaciones() == null
                        ? ""
                        : equipo.getObservaciones()
        );


        seleccionarTipo(
                equipo.getIdTipoEquipo()
        );


        seleccionarLaboratorio(
                equipo.getIdLaboratorio()
        );
    }


    private void seleccionarTipo(
            int idTipoEquipo
    ) {

        for (int i = 0;
             i < cmbTipoEquipo.getItemCount();
             i++) {

            TipoEquipo tipo =
                    cmbTipoEquipo.getItemAt(i);


            if (tipo.getIdTipoEquipo()
                    .equals(idTipoEquipo)) {

                cmbTipoEquipo.setSelectedIndex(i);
                return;
            }
        }
    }


    private void seleccionarLaboratorio(
            int idLaboratorio
    ) {

        for (int i = 0;
             i < cmbLaboratorio.getItemCount();
             i++) {

            Laboratorio laboratorio =
                    cmbLaboratorio.getItemAt(i);


            if (laboratorio
                    .getIdLaboratorio()
                    .equals(idLaboratorio)) {

                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }
    }


    private String buscarNombreTipo(
            int idTipoEquipo
    ) {

        for (int i = 0;
             i < cmbTipoEquipo.getItemCount();
             i++) {

            TipoEquipo tipo =
                    cmbTipoEquipo.getItemAt(i);


            if (tipo.getIdTipoEquipo()
                    .equals(idTipoEquipo)) {

                return tipo.getNombre();
            }
        }


        return String.valueOf(
                idTipoEquipo
        );
    }


    private String buscarNombreLaboratorio(
            int idLaboratorio
    ) {

        for (int i = 0;
             i < cmbLaboratorio.getItemCount();
             i++) {

            Laboratorio laboratorio =
                    cmbLaboratorio.getItemAt(i);


            if (laboratorio
                    .getIdLaboratorio()
                    .equals(idLaboratorio)) {

                return laboratorio.getCodigo()
                        + " - "
                        + laboratorio.getNombre();
            }
        }


        return String.valueOf(
                idLaboratorio
        );
    }


    private void limpiarFormulario() {

        idSeleccionado = null;

        txtCodigoInventario.setText("");
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtNumeroSerie.setText("");
        txtObservaciones.setText("");

        txtEstado.setText(
                EstadoEquipo.DISPONIBLE.name()
        );


        if (cmbTipoEquipo.getItemCount() > 0) {
            cmbTipoEquipo.setSelectedIndex(0);
        }


        if (cmbLaboratorio.getItemCount() > 0) {
            cmbLaboratorio.setSelectedIndex(0);
        }


        tablaEquipos.clearSelection();

        txtCodigoInventario.requestFocus();
    }


    private void mostrarError(
            String mensaje
    ) {

        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
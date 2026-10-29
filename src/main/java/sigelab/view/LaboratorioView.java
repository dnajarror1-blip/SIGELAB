package sigelab.view;

import sigelab.controller.LaboratorioController;
import sigelab.enums.EstadoLaboratorio;
import sigelab.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LaboratorioView extends JFrame {

    private final LaboratorioController controller;

    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtUbicacion;
    private JTextField txtCapacidad;

    private JComboBox<EstadoLaboratorio> cmbEstado;

    private JTable tablaLaboratorios;
    private DefaultTableModel modeloTabla;

    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnEliminar;
    private JButton btnLimpiar;

    private Integer idSeleccionado;


    public LaboratorioView() {

        controller =
                new LaboratorioController();

        configurarVentana();
        crearComponentes();
        cargarLaboratorios();
    }


    // Configuración general de la ventana.
    private void configurarVentana() {

        setTitle(
                "SIGELAB - Laboratorios"
        );

        setSize(
                900,
                550
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


    // Crea los componentes visuales.
    private void crearComponentes() {

        JPanel panelFormulario =
                new JPanel(
                        new GridBagLayout()
                );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        5,
                        5,
                        5,
                        5
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;


        txtCodigo =
                new JTextField(20);

        txtNombre =
                new JTextField(20);

        txtUbicacion =
                new JTextField(20);

        txtCapacidad =
                new JTextField(20);


        cmbEstado =
                new JComboBox<>(
                        EstadoLaboratorio.values()
                );


        agregarCampo(
                panelFormulario,
                gbc,
                0,
                "Código:",
                txtCodigo
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
                "Ubicación:",
                txtUbicacion
        );

        agregarCampo(
                panelFormulario,
                gbc,
                3,
                "Capacidad:",
                txtCapacidad
        );


        gbc.gridx = 0;
        gbc.gridy = 4;

        panelFormulario.add(
                new JLabel("Estado:"),
                gbc
        );


        gbc.gridx = 1;

        panelFormulario.add(
                cmbEstado,
                gbc
        );


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


        panelBotones.add(
                btnGuardar
        );

        panelBotones.add(
                btnActualizar
        );

        panelBotones.add(
                btnEliminar
        );

        panelBotones.add(
                btnLimpiar
        );


        String[] columnas = {
                "ID",
                "Código",
                "Nombre",
                "Ubicación",
                "Capacidad",
                "Estado"
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


        tablaLaboratorios =
                new JTable(
                        modeloTabla
                );


        tablaLaboratorios.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );


        JScrollPane scrollTabla =
                new JScrollPane(
                        tablaLaboratorios
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


    // Agrega un campo al formulario.
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


    // Configura los eventos de la ventana.
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


        tablaLaboratorios
                .getSelectionModel()
                .addListSelectionListener(
                        e -> {

                            if (!e.getValueIsAdjusting()) {
                                cargarSeleccion();
                            }
                        }
                );
    }


    // Registra un laboratorio.
    private void guardar() {

        try {

            int capacidad =
                    obtenerCapacidad();


            controller.crear(
                    txtCodigo.getText(),
                    txtNombre.getText(),
                    txtUbicacion.getText(),
                    capacidad,
                    (EstadoLaboratorio)
                            cmbEstado.getSelectedItem()
            );


            JOptionPane.showMessageDialog(
                    this,
                    "Laboratorio registrado correctamente."
            );


            cargarLaboratorios();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Actualiza el laboratorio seleccionado.
    private void actualizar() {

        if (idSeleccionado == null) {

            mostrarError(
                    "Debe seleccionar un laboratorio."
            );

            return;
        }


        try {

            int capacidad =
                    obtenerCapacidad();


            controller.actualizar(
                    idSeleccionado,
                    txtCodigo.getText(),
                    txtNombre.getText(),
                    txtUbicacion.getText(),
                    capacidad,
                    (EstadoLaboratorio)
                            cmbEstado.getSelectedItem()
            );


            JOptionPane.showMessageDialog(
                    this,
                    "Laboratorio actualizado correctamente."
            );


            cargarLaboratorios();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Elimina el laboratorio seleccionado.
    private void eliminar() {

        if (idSeleccionado == null) {

            mostrarError(
                    "Debe seleccionar un laboratorio."
            );

            return;
        }


        int respuesta =
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Desea eliminar el laboratorio seleccionado?",
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
                    "Laboratorio eliminado correctamente."
            );


            cargarLaboratorios();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Carga los laboratorios en la tabla.
    private void cargarLaboratorios() {

        modeloTabla.setRowCount(
                0
        );


        try {

            List<Laboratorio> laboratorios =
                    controller.listar();


            for (Laboratorio laboratorio
                    : laboratorios) {

                modeloTabla.addRow(
                        new Object[]{
                                laboratorio.getIdLaboratorio(),
                                laboratorio.getCodigo(),
                                laboratorio.getNombre(),
                                laboratorio.getUbicacion(),
                                laboratorio.getCapacidad(),
                                laboratorio.getEstado()
                        }
                );
            }

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Carga en el formulario una fila seleccionada.
    private void cargarSeleccion() {

        int fila =
                tablaLaboratorios
                        .getSelectedRow();


        if (fila == -1) {
            return;
        }


        idSeleccionado =
                Integer.parseInt(
                        modeloTabla
                                .getValueAt(
                                        fila,
                                        0
                                )
                                .toString()
                );


        txtCodigo.setText(
                modeloTabla
                        .getValueAt(
                                fila,
                                1
                        )
                        .toString()
        );


        txtNombre.setText(
                modeloTabla
                        .getValueAt(
                                fila,
                                2
                        )
                        .toString()
        );


        txtUbicacion.setText(
                modeloTabla
                        .getValueAt(
                                fila,
                                3
                        )
                        .toString()
        );


        txtCapacidad.setText(
                modeloTabla
                        .getValueAt(
                                fila,
                                4
                        )
                        .toString()
        );


        EstadoLaboratorio estado =
                EstadoLaboratorio.valueOf(
                        modeloTabla
                                .getValueAt(
                                        fila,
                                        5
                                )
                                .toString()
                );


        cmbEstado.setSelectedItem(
                estado
        );
    }


    // Convierte el campo capacidad a número.
    private int obtenerCapacidad() {

        String texto =
                txtCapacidad
                        .getText()
                        .trim();


        if (texto.isBlank()) {

            throw new IllegalArgumentException(
                    "La capacidad es obligatoria."
            );
        }


        try {

            return Integer.parseInt(
                    texto
            );

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "La capacidad debe ser un número entero."
            );
        }
    }


    // Limpia el formulario.
    private void limpiarFormulario() {

        idSeleccionado = null;

        txtCodigo.setText("");
        txtNombre.setText("");
        txtUbicacion.setText("");
        txtCapacidad.setText("");

        cmbEstado.setSelectedItem(
                EstadoLaboratorio.ACTIVO
        );

        tablaLaboratorios.clearSelection();

        txtCodigo.requestFocus();
    }


    // Muestra errores sin enseñar detalles SQL.
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

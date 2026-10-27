package sigelab.view;

import sigelab.controller.TipoEquipoController;
import sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TipoEquipoView extends JFrame {

    private final TipoEquipoController controller;

    private JTextField txtNombre;
    private JTextArea txtDescripcion;

    private JTable tablaTipos;
    private DefaultTableModel modeloTabla;

    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnEliminar;
    private JButton btnLimpiar;

    private Integer idSeleccionado;

    public TipoEquipoView() {

        this.controller = new TipoEquipoController();

        configurarVentana();
        crearComponentes();
        cargarTipos();
    }


    // Configuración general de la ventana.
    private void configurarVentana() {

        setTitle("SIGELAB - Tipos de Equipo");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
    }


    // Crea los componentes de la interfaz.
    private void crearComponentes() {

        JPanel panelFormulario = new JPanel(
                new GridBagLayout()
        );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets = new Insets(
                5,
                5,
                5,
                5
        );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;


        JLabel lblNombre =
                new JLabel("Nombre:");

        JLabel lblDescripcion =
                new JLabel("Descripción:");


        txtNombre =
                new JTextField(25);

        txtDescripcion =
                new JTextArea(3, 25);

        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);


        JScrollPane scrollDescripcion =
                new JScrollPane(txtDescripcion);


        gbc.gridx = 0;
        gbc.gridy = 0;

        panelFormulario.add(
                lblNombre,
                gbc
        );


        gbc.gridx = 1;

        panelFormulario.add(
                txtNombre,
                gbc
        );


        gbc.gridx = 0;
        gbc.gridy = 1;

        panelFormulario.add(
                lblDescripcion,
                gbc
        );


        gbc.gridx = 1;

        panelFormulario.add(
                scrollDescripcion,
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


        panelBotones.add(btnGuardar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);


        String[] columnas = {
                "ID",
                "Nombre",
                "Descripción"
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


        tablaTipos =
                new JTable(modeloTabla);

        tablaTipos.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );


        JScrollPane scrollTabla =
                new JScrollPane(tablaTipos);


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


    // Eventos de botones y tabla.
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


        tablaTipos
                .getSelectionModel()
                .addListSelectionListener(
                        e -> {

                            if (!e.getValueIsAdjusting()) {
                                cargarSeleccion();
                            }
                        }
                );
    }


    // Guarda un nuevo tipo de equipo.
    private void guardar() {

        try {

            controller.crear(
                    txtNombre.getText(),
                    txtDescripcion.getText()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Tipo de equipo registrado correctamente."
            );

            cargarTipos();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Actualiza el registro seleccionado.
    private void actualizar() {

        if (idSeleccionado == null) {

            mostrarError(
                    "Debe seleccionar un tipo de equipo."
            );

            return;
        }


        try {

            controller.actualizar(
                    idSeleccionado,
                    txtNombre.getText(),
                    txtDescripcion.getText()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Tipo de equipo actualizado correctamente."
            );

            cargarTipos();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Elimina el registro seleccionado.
    private void eliminar() {

        if (idSeleccionado == null) {

            mostrarError(
                    "Debe seleccionar un tipo de equipo."
            );

            return;
        }


        int respuesta =
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Desea eliminar el tipo de equipo seleccionado?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION
                );


        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }


        try {

            controller.eliminar(
                    idSeleccionado
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Tipo de equipo eliminado correctamente."
            );

            cargarTipos();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Carga los registros en la JTable.
    private void cargarTipos() {

        modeloTabla.setRowCount(0);

        try {

            List<TipoEquipo> tipos =
                    controller.listar();


            for (TipoEquipo tipo : tipos) {

                modeloTabla.addRow(
                        new Object[]{
                                tipo.getIdTipoEquipo(),
                                tipo.getNombre(),
                                tipo.getDescripcion()
                        }
                );
            }

        } catch (Exception e) {

            mostrarError(
                    e.getMessage()
            );
        }
    }


    // Carga en el formulario el registro seleccionado.
    private void cargarSeleccion() {

        int fila =
                tablaTipos.getSelectedRow();


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


        txtNombre.setText(
                String.valueOf(
                        modeloTabla.getValueAt(
                                fila,
                                1
                        )
                )
        );


        Object descripcion =
                modeloTabla.getValueAt(
                        fila,
                        2
                );


        txtDescripcion.setText(
                descripcion == null
                        ? ""
                        : descripcion.toString()
        );
    }


    // Limpia los campos del formulario.
    private void limpiarFormulario() {

        idSeleccionado = null;

        txtNombre.setText("");
        txtDescripcion.setText("");

        tablaTipos.clearSelection();

        txtNombre.requestFocus();
    }


    // Muestra mensajes de error comprensibles.
    private void mostrarError(String mensaje) {

        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
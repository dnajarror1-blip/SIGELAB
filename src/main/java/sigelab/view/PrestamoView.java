package sigelab.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import sigelab.controller.PrestamoController;
import sigelab.exception.BusinessException;
import sigelab.model.Prestamo;

public class PrestamoView extends JPanel {

    private final PrestamoController controller;
    private final int idUsuarioActual;

    private final JTextField txtIdPrestamo = new JTextField(8);

    private final JButton btnBuscar = new JButton("Buscar por ID");
    private final JButton btnActualizar = new JButton("Ver activos");
    private final JButton btnNuevo = new JButton("Nuevo préstamo");

    private final JLabel lblEstado = new JLabel(" ");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new String[]{
                            "ID",
                            "ID solicitante",
                            "ID registra",
                            "Fecha préstamo",
                            "Devolución esperada",
                            "Estado",
                            "Observaciones"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    public PrestamoView(int idUsuarioActual) {
        this(new PrestamoController(), idUsuarioActual);
    }

    public PrestamoView(
            PrestamoController controller,
            int idUsuarioActual
    ) {
        this.controller = controller;
        this.idUsuarioActual = idUsuarioActual;

        inicializarComponentes();
        cargarDatos();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("Gestión de préstamos");
        titulo.setFont(titulo.getFont().deriveFont(18f));
        panelSuperior.add(titulo, BorderLayout.NORTH);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("ID del préstamo:"));
        filtros.add(txtIdPrestamo);
        filtros.add(btnBuscar);
        filtros.add(btnActualizar);
        filtros.add(btnNuevo);

        panelSuperior.add(filtros, BorderLayout.CENTER);
        add(panelSuperior, BorderLayout.NORTH);

        JTable tabla = new JTable(modeloTabla);
        tabla.setRowHeight(24);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(
                    i == 6 ? 250 : 140
            );
        }

        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(lblEstado, BorderLayout.SOUTH);

        btnActualizar.addActionListener(e -> cargarDatos());
        btnNuevo.addActionListener(e -> mostrarRegistro());
        btnBuscar.addActionListener(e -> buscarPorId());
        txtIdPrestamo.addActionListener(e -> buscarPorId());
    }

    public void cargarDatos() {
        consultar(null);
    }

    private void buscarPorId() {
        try {
            int id = Integer.parseInt(txtIdPrestamo.getText().trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            consultar(id);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Escribe un ID entero mayor que cero.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void consultar(Integer idPrestamo) {
        cambiarEstadoCarga(true);
        lblEstado.setText("Consultando préstamos...");

        new SwingWorker<List<Prestamo>, Void>() {

            @Override
            protected List<Prestamo> doInBackground() {
                if (idPrestamo == null) {
                    return controller.buscarActivos();
                }

                return controller.buscarPorId(idPrestamo)
                        .map(List::of)
                        .orElseGet(List::of);
            }

            @Override
            protected void done() {
                try {
                    List<Prestamo> prestamos = get();
                    modeloTabla.setRowCount(0);

                    for (Prestamo prestamo : prestamos) {
                        modeloTabla.addRow(new Object[]{
                                prestamo.getIdPrestamo(),
                                prestamo.getIdUsuarioSolicitante(),
                                prestamo.getIdUsuarioRegistra(),
                                prestamo.getFechaPrestamo(),
                                prestamo.getFechaEsperadaDevolucion(),
                                prestamo.getEstado(),
                                prestamo.getObservaciones()
                        });
                    }

                    lblEstado.setText(
                            prestamos.isEmpty()
                                    ? "No se encontraron préstamos."
                                    : "Préstamos encontrados: " + prestamos.size()
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblEstado.setText("Consulta interrumpida.");

                } catch (ExecutionException e) {
                    lblEstado.setText("No se pudo completar la consulta.");

                    Throwable causa = e.getCause();

                    String mensaje = causa instanceof BusinessException
                            ? causa.getMessage()
                            : "No se pudieron consultar los préstamos. "
                            + "Revisa la conexión con la base de datos.";

                    JOptionPane.showMessageDialog(
                            PrestamoView.this,
                            mensaje,
                            "Consulta de préstamos",
                            JOptionPane.ERROR_MESSAGE
                    );

                } finally {
                    cambiarEstadoCarga(false);
                }
            }
        }.execute();
    }

    private void cambiarEstadoCarga(boolean cargando) {
        btnBuscar.setEnabled(!cargando);
        btnActualizar.setEnabled(!cargando);
        btnNuevo.setEnabled(!cargando);
        txtIdPrestamo.setEnabled(!cargando);
    }

    private void mostrarRegistro() {
        PrestamoRegistroDialog dialogo = new PrestamoRegistroDialog(
                SwingUtilities.getWindowAncestor(this),
                controller,
                idUsuarioActual
        );

        dialogo.setVisible(true);

        if (dialogo.fueRegistrado()) {
            cargarDatos();
        }
    }
}
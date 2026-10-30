package sigelab.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import sigelab.controller.DevolucionController;
import sigelab.enums.CondicionDevolucion;
import sigelab.repository.DevolucionRepository.PrestamoPendiente;
import sigelab.service.DevolucionService.Pendiente;

public class DevolucionView extends JPanel {
    private final DevolucionController controller;
    private final JComboBox<PrestamoPendiente> prestamos = new JComboBox<>();
    private final JButton actualizar = new JButton("Actualizar préstamos");
    private final JButton devolver = new JButton("Confirmar devolución");
    private final JTextField observaciones = new JTextField(25);
    private final JLabel estado = new JLabel(" ");
    private boolean ocupado;
    private List<Pendiente> pendientes = List.of();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Devolver", "Código de inventario", "Equipo", "Condición"}, 0) {
        public boolean isCellEditable(int fila, int columna) { return !ocupado && (columna == 0 || columna == 3); }
        public Class<?> getColumnClass(int columna) {
            return columna == 0 ? Boolean.class : columna == 3 ? CondicionDevolucion.class : String.class;
        }
    };
    private final JTable tabla = new JTable(modelo);

    public DevolucionView(int actor) { this(new DevolucionController(actor)); }
    DevolucionView(DevolucionController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 12));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel arriba = new JPanel(new BorderLayout(10, 10));
        JLabel titulo = new JLabel("Devoluciones · equipos pendientes");
        titulo.setFont(titulo.getFont().deriveFont(20f));
        arriba.add(titulo, BorderLayout.NORTH);
        prestamos.setRenderer(new DefaultListCellRenderer() {
            public java.awt.Component getListCellRendererComponent(JList<?> lista, Object valor,
                    int indice, boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                if (valor instanceof PrestamoPendiente p) setText("Préstamo " + p.idPrestamo() + " · "
                        + p.solicitante() + " · " + p.fechaPrestamo() + " · devolución prevista " + p.fechaEsperada());
                return this;
            }
        });
        arriba.add(prestamos, BorderLayout.CENTER);
        arriba.add(actualizar, BorderLayout.EAST);
        add(arriba, BorderLayout.NORTH);
        tabla.setRowHeight(26);
        tabla.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(
                new JComboBox<>(CondicionDevolucion.values())));
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout(8, 8));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(new JLabel("Observaciones:")); acciones.add(observaciones); acciones.add(devolver);
        abajo.add(acciones, BorderLayout.CENTER); abajo.add(estado, BorderLayout.SOUTH);
        add(abajo, BorderLayout.SOUTH);
        actualizar.addActionListener(e -> cargarPrestamos());
        prestamos.addActionListener(e -> { if (!ocupado) cargarPendientes(); });
        devolver.addActionListener(e -> confirmar());
        modelo.addTableModelListener(e -> actualizarAcciones());
        cargarPrestamos();
    }

    private void ocupado(boolean valor) {
        ocupado = valor;
        prestamos.setEnabled(!valor); actualizar.setEnabled(!valor);
        observaciones.setEnabled(!valor); tabla.setEnabled(!valor);
        actualizarAcciones();
    }

    private void actualizarAcciones() {
        boolean seleccion = false;
        for (int i = 0; i < modelo.getRowCount(); i++) seleccion |= Boolean.TRUE.equals(modelo.getValueAt(i, 0));
        devolver.setEnabled(!ocupado && seleccion && prestamos.getSelectedItem() != null);
    }

    private void limpiar() { pendientes = List.of(); modelo.setRowCount(0); }

    private void cargarPrestamos() {
        if (ocupado) return;
        ocupado(true); limpiar(); prestamos.removeAllItems(); estado.setText("Consultando préstamos...");
        OperacionModulo.ejecutar(this, controller::listarPrestamosPendientes, lista -> {
            prestamos.removeAllItems();
            for (PrestamoPendiente p : lista) prestamos.addItem(p);
            estado.setText(lista.isEmpty() ? "No hay préstamos con equipos pendientes." : "Seleccione equipos y su condición.");
        }, () -> { ocupado(false); cargarPendientes(); });
    }

    private void cargarPendientes() {
        PrestamoPendiente p = (PrestamoPendiente) prestamos.getSelectedItem();
        limpiar();
        if (p == null) return;
        ocupado(true);
        OperacionModulo.ejecutar(this, () -> controller.buscarPendientes(p.idPrestamo()), lista -> {
            pendientes = List.copyOf(lista);
            for (Pendiente d : lista) modelo.addRow(new Object[]{false, d.equipo().getCodigoInventario(),
                    d.equipo().getNombre(), CondicionDevolucion.BUENO});
            estado.setText(lista.size() + " equipo(s) pendiente(s). Fecha de devolución: fecha del sistema.");
        }, () -> ocupado(false));
    }

    private void confirmar() {
        if (ocupado) return;
        if (tabla.isEditing() && !tabla.getCellEditor().stopCellEditing()) return;
        PrestamoPendiente p = (PrestamoPendiente) prestamos.getSelectedItem();
        Map<Integer, CondicionDevolucion> lote = new LinkedHashMap<>();
        for (int i = 0; i < modelo.getRowCount(); i++) if (Boolean.TRUE.equals(modelo.getValueAt(i, 0)))
            lote.put(pendientes.get(i).detalle().getIdDetallePrestamo(), (CondicionDevolucion) modelo.getValueAt(i, 3));
        if (p == null || lote.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "¿Registrar la devolución de " + lote.size() + " equipo(s)?",
                "Confirmar devolución", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String obs = observaciones.getText();
        ocupado(true);
        OperacionModulo.ejecutar(this, () -> controller.registrar(p.idPrestamo(), lote, obs), lista -> {
            observaciones.setText("");
            estado.setText("Devolución registrada: " + lista.size() + " equipo(s).");
        }, () -> { ocupado(false); cargarPrestamos(); });
    }
}

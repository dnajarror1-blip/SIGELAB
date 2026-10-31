package sigelab.view;

import sigelab.service.CatalogoSeleccionService.EquipoOpcion;
import sigelab.view.ui.UiTheme;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Equipos disponibles y selección de préstamo; nunca solicita identificadores. */
class SeleccionEquiposPanel extends JPanel {
    private List<EquipoOpcion> disponibles = List.of();
    private final Map<Integer, EquipoOpcion> seleccionados = new LinkedHashMap<>();
    private final JTextField buscar = new JTextField(24);
    private final DefaultTableModel origen = modelo(), destino = modelo();
    private final JTable tabla = new JTable(origen), elegidos = new JTable(destino);
    private final JButton agregar = new JButton("Agregar"), quitar = new JButton("Quitar");
    private final JLabel estado = new JLabel("No hay equipos disponibles.");
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(origen);

    SeleccionEquiposPanel() {
        super(new BorderLayout(6, 6));
        buscar.putClientProperty("JTextField.placeholderText", "Buscar por código, nombre o marca");
        buscar.getAccessibleContext().setAccessibleName("Buscar equipos disponibles");
        tabla.setRowSorter(sorter);
        for (JTable t : List.of(tabla, elegidos)) {
            UiTheme.tabla(t); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            t.setPreferredScrollableViewportSize(new Dimension(600, 130));
        }
        JPanel arriba = new JPanel(new BorderLayout()); arriba.add(buscar); arriba.add(agregar, BorderLayout.EAST);
        add(arriba, BorderLayout.NORTH);
        JPanel listas = new JPanel(new GridLayout(2, 1, 0, 8));
        JScrollPane fuente = new JScrollPane(tabla); fuente.setBorder(BorderFactory.createTitledBorder("Equipos disponibles"));
        JPanel seleccion = new JPanel(new BorderLayout());
        seleccion.setBorder(BorderFactory.createTitledBorder("Equipos seleccionados"));
        seleccion.add(new JScrollPane(elegidos)); seleccion.add(quitar, BorderLayout.EAST);
        listas.add(fuente); listas.add(seleccion); add(listas); add(estado, BorderLayout.SOUTH);
        agregar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila >= 0) agregar(filasDisponibles().get(tabla.convertRowIndexToModel(fila)));
        });
        quitar.addActionListener(e -> {
            int fila = elegidos.getSelectedRow();
            if (fila >= 0) { seleccionados.remove(ids().get(fila)); pintar(); }
        });
        buscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
    }
    private static DefaultTableModel modelo() {
        return new DefaultTableModel(new String[]{"Código", "Nombre", "Tipo", "Marca", "Laboratorio", "Estado"}, 0) {
            public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }
    private List<EquipoOpcion> filasDisponibles() { return disponibles.stream().filter(e -> !seleccionados.containsKey(e.equipo().getIdEquipo())).toList(); }
    void disponibles(List<EquipoOpcion> lista) { disponibles = List.copyOf(lista); pintar(); }
    boolean agregar(EquipoOpcion opcion) {
        if (seleccionados.putIfAbsent(opcion.equipo().getIdEquipo(), opcion) != null) return false;
        pintar(); return true;
    }
    List<Integer> ids() { return List.copyOf(seleccionados.keySet()); }
    private void pintar() {
        origen.setRowCount(0); destino.setRowCount(0);
        filasDisponibles().forEach(e -> fila(origen, e)); seleccionados.values().forEach(e -> fila(destino, e)); filtrar();
    }
    private void fila(DefaultTableModel modelo, EquipoOpcion e) {
        modelo.addRow(new Object[]{e.equipo().getCodigoInventario(), e.equipo().getNombre(), e.tipo(), e.equipo().getMarca(), e.laboratorio(), e.equipo().getEstado()});
    }
    private void filtrar() {
        String filtro = buscar.getText().strip().toLowerCase(Locale.ROOT);
        sorter.setRowFilter(new RowFilter<>() {
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> fila) {
                return (fila.getStringValue(0) + " " + fila.getStringValue(1) + " " + fila.getStringValue(3)).toLowerCase(Locale.ROOT).contains(filtro);
            }
        });
        estado.setText(tabla.getRowCount() == 0 ? "No hay equipos disponibles para esta búsqueda." : "Selecciona un equipo y pulsa Agregar.");
    }
    @Override public void setEnabled(boolean valor) {
        super.setEnabled(valor);
        if (buscar != null) { buscar.setEnabled(valor); tabla.setEnabled(valor); elegidos.setEnabled(valor); agregar.setEnabled(valor); quitar.setEnabled(valor); }
    }
}

package sigelab.view.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** La búsqueda es texto libre; el valor elegido siempre procede del catálogo. */
public class SelectorBuscable<T> extends JPanel {
    private final JTextField buscar = new JTextField(22);
    private final JComboBox<T> opciones = new JComboBox<>();
    private final JLabel estado = new JLabel("Cargando...");
    private final Function<T, String> texto;
    private final Function<T, String> busqueda;
    private List<T> valores = List.of();

    public SelectorBuscable(String ayuda, Function<T, String> texto, Function<T, String> busqueda) {
        super(new BorderLayout(4, 4));
        this.texto = texto; this.busqueda = busqueda;
        buscar.putClientProperty("JTextField.placeholderText", ayuda);
        buscar.getAccessibleContext().setAccessibleName(ayuda);
        opciones.getAccessibleContext().setAccessibleName("Seleccionar resultado");
        opciones.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice, boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                @SuppressWarnings("unchecked") T opcion = (T) valor;
                setText(opcion == null ? "Selecciona una opción" : texto.apply(opcion));
                setToolTipText(getText());
                return this;
            }
        });
        add(buscar, BorderLayout.NORTH); add(opciones, BorderLayout.CENTER); add(estado, BorderLayout.SOUTH);
        buscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
    }
    public void valores(List<T> lista) { valores = List.copyOf(lista); filtrar(); }
    private void filtrar() {
        T previa = seleccionado();
        String filtro = buscar.getText().strip().toLowerCase(Locale.ROOT);
        opciones.removeAllItems();
        for (T valor : valores) if (busqueda.apply(valor).toLowerCase(Locale.ROOT).contains(filtro)) opciones.addItem(valor);
        opciones.setSelectedIndex(-1);
        for (int i = 0; i < opciones.getItemCount(); i++) if (opciones.getItemAt(i).equals(previa)) opciones.setSelectedIndex(i);
        estado.setText(opciones.getItemCount() == 0 ? "No se encontraron resultados." : "Selecciona un resultado.");
    }
    @SuppressWarnings("unchecked") public T seleccionado() { return (T) opciones.getSelectedItem(); }
    @Override public void setEnabled(boolean habilitado) {
        super.setEnabled(habilitado);
        if (buscar != null) { buscar.setEnabled(habilitado); opciones.setEnabled(habilitado); }
    }
}

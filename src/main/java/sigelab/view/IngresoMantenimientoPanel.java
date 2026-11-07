package sigelab.view;

import sigelab.enums.*;
import sigelab.service.CatalogoSeleccionService.EquipoOpcion;
import sigelab.view.ui.SelectorBuscable;
import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

class IngresoMantenimientoPanel extends JPanel {
    final SelectorBuscable<EquipoOpcion> equipo = new SelectorBuscable<>("Buscar equipo por código, nombre o marca",
            e -> e + " · " + e.equipo().getEstado(), EquipoOpcion::busqueda);
    final JComboBox<TipoMantenimiento> tipo = new JComboBox<>(TipoMantenimiento.values());
    final JTextField descripcion = new JTextField(25);
    final JTextArea observaciones = new JTextArea(3, 25);
    private List<EquipoOpcion> catalogo = List.of();
    IngresoMantenimientoPanel() {
        super(new GridBagLayout());
        observaciones.setLineWrap(true); observaciones.setWrapStyleWord(true);
        JComponent[] campos = {new JLabel("Equipo"), equipo, new JLabel("Tipo de mantenimiento"), tipo,
                new JLabel("Diagnóstico / Motivo"), descripcion, new JLabel("Observaciones"),
                new JScrollPane(observaciones), new JLabel("El servicio valida el estado del equipo al registrar el ingreso.")};
        for (int i = 0; i < campos.length; i++) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 1;
            gbc.insets = new Insets(4, 0, 4, 0); gbc.fill = GridBagConstraints.HORIZONTAL;
            if (i == 7) { gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH; }
            add(campos[i], gbc);
        }
        sigelab.view.ui.UiTheme.aplicar(this);
        tipo.addActionListener(e -> ordenar());
    }
    void equipos(List<EquipoOpcion> opciones) { catalogo = List.copyOf(opciones); ordenar(); }
    private void ordenar() {
        // Prioridad visual, sin excluir estados ni sustituir la validación del servicio.
        equipo.valores(tipo.getSelectedItem() == TipoMantenimiento.CORRECTIVO
                ? catalogo.stream().sorted(Comparator.comparing(e -> e.equipo().getEstado() != EstadoEquipo.DANADO)).toList() : catalogo);
    }
}

package sigelab.view;

import sigelab.controller.ReporteController;
import sigelab.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DashboardPanel extends JPanel {
    private final ReporteController controller;
    private final Map<String, JLabel> cantidades = new LinkedHashMap<>();
    private final JLabel estado = new JLabel(" ");
    private final JButton actualizar = new JButton("Actualizar resumen");

    public DashboardPanel(Usuario usuario) {
        this(new ReporteController(usuario));
    }

    DashboardPanel(ReporteController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        JPanel indicadores = new JPanel(new GridLayout(0, 2, 15, 15));
        indicadores.setOpaque(false);
        agregar(indicadores, "equipos_TOTAL", "Total de equipos");
        agregar(indicadores, "equipos_DISPONIBLE", "Equipos disponibles");
        agregar(indicadores, "equipos_PRESTADO", "Equipos prestados");
        agregar(indicadores, "equipos_DANADO", "Equipos dañados");
        agregar(indicadores, "equipos_MANTENIMIENTO", "Equipos en mantenimiento");
        agregar(indicadores, "equipos_BAJA", "Equipos de baja");
        agregar(indicadores, "prestamos_ACTIVO", "Préstamos activos");
        agregar(indicadores, "prestamos_VENCIDO", "Préstamos vencidos");
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.setOpaque(false);
        acciones.add(actualizar);
        acciones.add(estado);
        add(indicadores, BorderLayout.CENTER);
        add(acciones, BorderLayout.SOUTH);
        actualizar.addActionListener(e -> cargar());
        cargar();
    }

    private void agregar(JPanel panel, String clave, String titulo) {
        JPanel indicador = new JPanel(new BorderLayout(5, 5));
        indicador.setBorder(BorderFactory.createTitledBorder(titulo));
        JLabel cantidad = new JLabel("—", SwingConstants.CENTER);
        cantidad.setFont(cantidad.getFont().deriveFont(Font.BOLD, 24f));
        cantidades.put(clave, cantidad);
        indicador.add(cantidad, BorderLayout.CENTER);
        panel.add(indicador);
    }

    private void cargar() {
        actualizar.setEnabled(false);
        estado.setText("Cargando resumen...");
        cantidades.values().forEach(etiqueta -> etiqueta.setText("—"));
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                return controller.dashboard();
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> conteos = get();
                    cantidades.forEach((clave, etiqueta) -> etiqueta.setText(String.valueOf(conteos.get(clave))));
                    estado.setText("Resumen actualizado.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    estado.setText("Carga interrumpida.");
                } catch (ExecutionException e) {
                    estado.setText("No se pudo cargar el resumen. Puedes reintentarlo.");
                } finally {
                    actualizar.setEnabled(true);
                }
            }
        }.execute();
    }
}

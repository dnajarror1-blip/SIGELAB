package sigelab.view;

import sigelab.controller.ReporteController;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import sigelab.exception.BusinessException;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportesView extends JPanel {
    private final ReporteController controller;
    private final JComboBox<String> tipo = new JComboBox<>(new String[]{"Dashboard", "Equipos por estado",
            "Préstamos", "Préstamos por fechas", "Historial de equipos", "Historial de un equipo", "Historial de mantenimientos"});
    private final JComboBox<EstadoEquipo> equipos = new JComboBox<>(EstadoEquipo.values());
    private final JComboBox<EstadoMantenimiento> mantenimientos = new JComboBox<>(EstadoMantenimiento.values());
    private final JLabel etiquetaCodigo = new JLabel("Código de inventario:");
    private final JTextField codigoInventario = new JTextField(15);
    private final JPanel filtroEquipo = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JTextField desde = new JTextField(10);
    private final JTextField hasta = new JTextField(10);
    private final JButton consultar = new JButton("Consultar");
    private final JLabel resumen = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public ReportesView(Usuario usuario) {
        controller = new ReporteController(usuario);
        setLayout(new BorderLayout(10, 10));
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (Component componente : new Component[]{tipo, equipos, mantenimientos,
                new JLabel("Desde (AAAA-MM-DD):"), desde, new JLabel("Hasta:"), hasta, consultar}) {
            filtros.add(componente);
        }
        consultar.addActionListener(e -> cargar());
        tipo.addActionListener(e -> filtros());
        filtroEquipo.add(etiquetaCodigo);
        filtroEquipo.add(codigoInventario);
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(filtros, BorderLayout.NORTH);
        cabecera.add(filtroEquipo, BorderLayout.SOUTH);
        add(cabecera, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);
        filtros();
        cargar();
    }

    private void filtros() {
        String reporte = (String) tipo.getSelectedItem();
        equipos.setVisible("Equipos por estado".equals(reporte));
        mantenimientos.setVisible("Historial de mantenimientos".equals(reporte));
        filtroEquipo.setVisible("Historial de un equipo".equals(reporte));
        desde.setEnabled("Préstamos por fechas".equals(reporte));
        hasta.setEnabled(desde.isEnabled());
    }

    private void cargar() {
        String reporte = (String) tipo.getSelectedItem();
        EstadoEquipo estadoEquipo = (EstadoEquipo) equipos.getSelectedItem();
        EstadoMantenimiento estadoMantenimiento = (EstadoMantenimiento) mantenimientos.getSelectedItem();
        String codigo = codigoInventario.getText();
        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = "Préstamos por fechas".equals(reporte) ? LocalDate.parse(desde.getText().trim()) : null;
            fin = "Préstamos por fechas".equals(reporte) ? LocalDate.parse(hasta.getText().trim()) : null;
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Ingresa las fechas con formato AAAA-MM-DD.",
                    "Reportes", JOptionPane.ERROR_MESSAGE);
            return;
        }
        tipo.setEnabled(false);
        consultar.setEnabled(false);
        resumen.setText("Consultando...");
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return switch (reporte) {
                    case "Dashboard" -> indicadores(controller.dashboard());
                    case "Equipos por estado" -> controller.equiposPorEstado(estadoEquipo);
                    case "Préstamos" -> controller.prestamos();
                    case "Préstamos por fechas" -> controller.prestamosEntreFechas(inicio, fin);
                    case "Historial de un equipo" -> controller.historialEquipo(codigo);
                    case "Historial de equipos" -> controller.historialEquipos();
                    case "Historial de mantenimientos" -> controller.historialMantenimientos(estadoMantenimiento);
                    default -> List.of();
                };
            }

            @Override
            protected void done() {
                tipo.setEnabled(true);
                consultar.setEnabled(true);
                try {
                    List<Map<String, Object>> filas = get();
                    mostrar(filas);
                    resumen.setText(filas.isEmpty()
                            ? ("Historial de un equipo".equals(reporte) ? "El equipo no tiene historial de préstamos/devoluciones." : "Sin resultados.")
                            : filas.size() + " resultados.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    resumen.setText("Consulta interrumpida.");
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    Logger.getLogger(ReportesView.class.getName()).log(Level.WARNING,
                            "Falló una consulta de reportes ({0})", causa.getClass().getName());
                    String mensaje = causa instanceof BusinessException || causa instanceof IllegalArgumentException
                            ? causa.getMessage() : "No se pudo consultar la información. Contacta al administrador.";
                    modelo.setRowCount(0);
                    resumen.setText(mensaje);
                    JOptionPane.showMessageDialog(ReportesView.this, mensaje, "Reportes", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private List<Map<String, Object>> indicadores(Map<String, Object> conteos) {
        List<Map<String, Object>> filas = new ArrayList<>();
        conteos.forEach((nombre, valor) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("Indicador", nombre.replace('_', ' '));
            fila.put("Cantidad", valor);
            filas.add(fila);
        });
        return filas;
    }

    private void mostrar(List<Map<String, Object>> filas) {
        modelo.setRowCount(0);
        modelo.setColumnCount(0);
        if (filas.isEmpty()) {
            modelo.addColumn("Resultado");
            return;
        }
        List<String> columnas = new ArrayList<>(filas.getFirst().keySet());
        columnas.forEach(nombre -> modelo.addColumn(nombre.replace('_', ' ')));
        for (Map<String, Object> fila : filas) {
            modelo.addRow(columnas.stream().map(fila::get).toArray());
        }
    }
}

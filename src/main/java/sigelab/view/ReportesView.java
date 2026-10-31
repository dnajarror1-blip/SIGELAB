package sigelab.view;

import sigelab.controller.ReporteController;
import sigelab.controller.CatalogoSeleccionController;
import sigelab.service.Modulo;
import sigelab.service.CatalogoSeleccionService.EquipoOpcion;
import sigelab.view.ui.*;
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
    private final JLabel etiquetaCodigo = new JLabel("Equipo:");
    private final SelectorBuscable<EquipoOpcion> selectorEquipo = new SelectorBuscable<>(
            "Buscar equipo por código, nombre o marca", EquipoOpcion::toString, EquipoOpcion::busqueda);
    private final CatalogoSeleccionController catalogos;
    private final JButton actualizarEquipos = new JButton("Actualizar equipos");
    private final JPanel filtroFechas = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
        this(new ReporteController(usuario), new CatalogoSeleccionController(usuario.getIdUsuario(), Modulo.REPORTES_QR));
    }

    ReportesView(ReporteController controller, CatalogoSeleccionController catalogos) {
        this.controller = controller;
        this.catalogos = catalogos;
        setLayout(new BorderLayout(10, 10));
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (Component componente : new Component[]{tipo, equipos, mantenimientos, consultar}) {
            filtros.add(componente);
        }
        consultar.addActionListener(e -> cargar());
        tipo.addActionListener(e -> filtros());
        filtroEquipo.add(etiquetaCodigo);
        filtroEquipo.add(selectorEquipo);
        filtroEquipo.add(actualizarEquipos);
        actualizarEquipos.addActionListener(e -> cargarEquipos());
        desde.setText(LocalDate.now().withDayOfMonth(1).toString());
        hasta.setText(LocalDate.now().toString());
        desde.putClientProperty("JTextField.placeholderText", "AAAA-MM-DD");
        hasta.putClientProperty("JTextField.placeholderText", "AAAA-MM-DD");
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(filtros, BorderLayout.NORTH);
        filtroFechas.add(new JLabel("Desde (AAAA-MM-DD):")); filtroFechas.add(desde);
        filtroFechas.add(new JLabel("Hasta (AAAA-MM-DD):")); filtroFechas.add(hasta);
        JPanel detalles = new JPanel(); detalles.setLayout(new BoxLayout(detalles, BoxLayout.Y_AXIS));
        detalles.add(filtroFechas); detalles.add(filtroEquipo);
        cabecera.add(detalles, BorderLayout.SOUTH);
        add(cabecera, BorderLayout.NORTH);
        JTable tabla = new JTable(modelo);
        UiTheme.tabla(tabla); tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);
        filtros();
        cargar();
        cargarEquipos();
    }

    private void cargarEquipos() {
        actualizarEquipos.setEnabled(false); selectorEquipo.setEnabled(false);
        actualizarEquipos.setText("Cargando...");
        OperacionModulo.ejecutar(this, () -> catalogos.listarEquipos(false), selectorEquipo::valores,
                () -> { actualizarEquipos.setEnabled(true); selectorEquipo.setEnabled(true); actualizarEquipos.setText("Actualizar equipos"); });
    }

    private void filtros() {
        String reporte = (String) tipo.getSelectedItem();
        equipos.setVisible("Equipos por estado".equals(reporte));
        mantenimientos.setVisible("Historial de mantenimientos".equals(reporte));
        filtroEquipo.setVisible("Historial de un equipo".equals(reporte));
        filtroFechas.setVisible("Préstamos por fechas".equals(reporte));
        desde.setEnabled("Préstamos por fechas".equals(reporte));
        hasta.setEnabled(desde.isEnabled());
    }

    private void cargar() {
        String reporte = (String) tipo.getSelectedItem();
        EstadoEquipo estadoEquipo = (EstadoEquipo) equipos.getSelectedItem();
        EstadoMantenimiento estadoMantenimiento = (EstadoMantenimiento) mantenimientos.getSelectedItem();
        EquipoOpcion elegido = selectorEquipo.seleccionado();
        if ("Historial de un equipo".equals(reporte) && elegido == null) { resumen.setText("Selecciona un equipo de la lista."); return; }
        String codigo = elegido == null ? null : elegido.equipo().getCodigoInventario();
        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = "Préstamos por fechas".equals(reporte) ? LocalDate.parse(desde.getText().trim()) : null;
            fin = "Préstamos por fechas".equals(reporte) ? LocalDate.parse(hasta.getText().trim()) : null;
        } catch (DateTimeParseException e) {
            desde.putClientProperty("JComponent.outline", "error"); hasta.putClientProperty("JComponent.outline", "error");
            resumen.setText("Ingresa fechas válidas con formato AAAA-MM-DD.");
            return;
        }
        if (inicio != null && inicio.isAfter(fin)) {
            hasta.putClientProperty("JComponent.outline", "error"); resumen.setText("La fecha Hasta debe ser igual o posterior a Desde."); return;
        }
        desde.putClientProperty("JComponent.outline", null); hasta.putClientProperty("JComponent.outline", null);
        tipo.setEnabled(false);
        consultar.setEnabled(false);
        resumen.setText("Cargando...");
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
                            ? ("Historial de un equipo".equals(reporte) ? "El equipo no tiene historial de préstamos/devoluciones." : "No se encontraron resultados.")
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
        List<String> columnas = filas.getFirst().keySet().stream().filter(c -> !c.toLowerCase(java.util.Locale.ROOT).startsWith("id_")).toList();
        columnas.forEach(nombre -> modelo.addColumn(switch (nombre) {
            case "codigo_inventario" -> "Código";
            case "nombre_equipo" -> "Equipo";
            case "fecha_esperada_devolucion" -> "Fecha esperada";
            default -> nombre.replace('_', ' ');
        }));
        for (Map<String, Object> fila : filas) {
            modelo.addRow(columnas.stream().map(fila::get).toArray());
        }
    }
}

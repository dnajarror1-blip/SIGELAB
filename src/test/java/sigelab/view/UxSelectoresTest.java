package sigelab.view;

import org.junit.jupiter.api.Test;
import sigelab.controller.*;
import sigelab.enums.*;
import sigelab.model.*;
import sigelab.service.CatalogoSeleccionService.*;
import sigelab.service.Modulo;
import sigelab.service.QrEquipoService;
import sigelab.view.ui.SelectorBuscable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class UxSelectoresTest {
    static EquipoOpcion opcion(int id, String codigo, String nombre, String marca, EstadoEquipo estado) {
        Equipo e = new Equipo(); e.setIdEquipo(id); e.setCodigoInventario(codigo); e.setNombre(nombre); e.setMarca(marca); e.setEstado(estado);
        return new EquipoOpcion(e, "Portátil", "Laboratorio A");
    }
    private final EquipoOpcion lenovo = opcion(71, "CAM-001", "ThinkPad", "Lenovo", EstadoEquipo.DISPONIBLE);
    private final EquipoOpcion dell = opcion(92, "CAM-002", "Latitude", "Dell", EstadoEquipo.DANADO);

    @Test void solicitanteBuscableConservaIdSinPermitirEscribirUnValorArbitrario() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var selector = new SelectorBuscable<Solicitante>("Buscar solicitante", Solicitante::nombre, Solicitante::nombre);
            selector.valores(List.of(new Solicitante(808, "Juan Pérez · estudiante"), new Solicitante(909, "Ana · encargado")));
            JComboBox<?> combo = buscar(selector, JComboBox.class).getFirst();
            assertFalse(combo.isEditable()); assertNull(selector.seleccionado());
            buscar(selector, JTextField.class).getFirst().setText("Juan");
            assertEquals(1, combo.getItemCount()); combo.setSelectedIndex(0);
            assertEquals(808, selector.seleccionado().idUsuario());
            assertFalse(selector.seleccionado().toString().contains("808"));
            buscar(selector, JTextField.class).getFirst().setText("persona inexistente");
            assertEquals(0, combo.getItemCount()); assertNull(selector.seleccionado());
        });
    }
    @Test void prestamosAgregaQuitaYEvitaDuplicadosIncluidoElEquipoRecibidoPorQr() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeleccionEquiposPanel panel = new SeleccionEquiposPanel();
            panel.disponibles(List.of(lenovo, opcion(72,"CAM-003","Otro","HP",EstadoEquipo.DISPONIBLE)));
            List<JTable> tablas = buscar(panel,JTable.class);
            JTextField filtro = buscar(panel,JTextField.class).getFirst();
            filtro.setText("LENOVO"); assertEquals(1, tablas.getFirst().getRowCount());
            tablas.getFirst().setRowSelectionInterval(0,0); boton(panel,"Agregar").doClick(0);
            assertEquals(List.of(71),panel.ids()); assertFalse(panel.agregar(lenovo));
            assertEquals(1,tablas.get(1).getRowCount()); assertEquals(0,tablas.getFirst().getRowCount());
            assertFalse(tablas.get(1).isCellEditable(0,0));
            tablas.get(1).setRowSelectionInterval(0,0); boton(panel,"Quitar").doClick(0);
            assertTrue(panel.ids().isEmpty()); assertEquals(1,tablas.getFirst().getRowCount());
            filtro.setText("CAM-003"); assertEquals("CAM-003",tablas.getFirst().getValueAt(0,0));
            tablas.getFirst().setRowSelectionInterval(0,0); boton(panel,"Agregar").doClick(0);
            assertEquals(List.of(72),panel.ids(),"Debe convertir la fila filtrada a su modelo");
        });
    }
    @Test void mantenimientoPriorizaDanadosSinFiltrarEstadosNiPedirId() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            IngresoMantenimientoPanel panel = new IngresoMantenimientoPanel();
            panel.setSize(640, 480); distribuir(panel);
            JComboBox<?> opcionesVisibles = buscar(panel.equipo,JComboBox.class).getFirst();
            assertTrue(opcionesVisibles.getHeight() >= opcionesVisibles.getPreferredSize().height,
                    "La búsqueda no debe ocultar ni comprimir el ComboBox de equipos");
            panel.equipos(List.of(lenovo,dell)); panel.tipo.setSelectedItem(TipoMantenimiento.CORRECTIVO);
            JComboBox<?> combo = buscar(panel.equipo,JComboBox.class).getFirst();
            assertEquals(dell, combo.getItemAt(0)); assertEquals(2,combo.getItemCount());
            combo.setSelectedIndex(0); assertEquals(92,panel.equipo.seleccionado().equipo().getIdEquipo());
            panel.tipo.setSelectedItem(TipoMantenimiento.PREVENTIVO);
            assertEquals(2,combo.getItemCount());
            assertTrue(buscar(panel,JLabel.class).stream().noneMatch(l -> l.getText().contains("ID")));
        });
    }
    @Test void qrSeleccionaPorMarcaGeneraPorCodigoYPresentaResultadoSinCambiarEstado() throws Exception {
        AtomicReference<String> generado = new AtomicReference<>();
        QrController controller = new QrController(null,null,null) {
            public void generar(String codigo,File destino) { assertFalse(SwingUtilities.isEventDispatchThread()); generado.set(codigo); }
            public QrEquipoService.Resultado identificar(File imagen) { assertFalse(SwingUtilities.isEventDispatchThread()); return new QrEquipoService.Resultado("CAM-002",dell.equipo()); }
        };
        QrView[] vista = new QrView[1];
        SwingUtilities.invokeAndWait(() -> vista[0] = new QrView(controller,catalogos()));
        esperar(() -> boton(vista[0],"Generar QR").isEnabled());
        SwingUtilities.invokeAndWait(() -> {
            buscar(vista[0],JTextField.class).getFirst().setText("Lenovo");
            JComboBox<?> combo = buscar(vista[0],JComboBox.class).getFirst();
            assertEquals(1,combo.getItemCount()); combo.setSelectedIndex(0);
            vista[0].generarSeleccionado(new File("fixture-no-se-escribe.png"));
            assertFalse(boton(vista[0],"Generar QR").isEnabled());
        });
        esperar(() -> "QR generado.".equals(textos(vista[0]).stream().filter("QR generado."::equals).findFirst().orElse(null)));
        assertEquals("CAM-001",generado.get());
        SwingUtilities.invokeAndWait(() -> assertTrue(textos(vista[0]).contains("Disponible para préstamo")));
        SwingUtilities.invokeAndWait(() -> vista[0].identificar(new File("fixture-no-se-lee.png")));
        esperar(() -> textos(vista[0]).contains("Equipo identificado."));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(textos(vista[0]).containsAll(List.of("CAM-002","Latitude","Laboratorio A","No disponible para préstamo")));
            assertEquals(EstadoEquipo.DANADO,dell.equipo().getEstado());
        });
    }
    @Test void reportesConsultaElCodigoSeleccionadoYValidaFechasSinConsultar() throws Exception {
        AtomicReference<String> consultado = new AtomicReference<>(); AtomicInteger fechas = new AtomicInteger();
        ReporteController controller = new ReporteController(null,null,null) {
            public Map<String,Object> dashboard() { return Map.of(); }
            public List<Map<String,Object>> historialEquipo(String codigo) {
                assertFalse(SwingUtilities.isEventDispatchThread()); consultado.set(codigo);
                return List.of(Map.of("codigo_inventario",codigo,"nombre_equipo","ThinkPad","id_equipo",71));
            }
            public List<Map<String,Object>> prestamosEntreFechas(java.time.LocalDate desde,java.time.LocalDate hasta) { fechas.incrementAndGet(); return List.of(); }
        };
        ReportesView[] vista = new ReportesView[1];
        SwingUtilities.invokeAndWait(() -> vista[0] = new ReportesView(controller,catalogos()));
        esperar(() -> boton(vista[0],"Consultar").isEnabled() && boton(vista[0],"Actualizar equipos").isEnabled());
        SwingUtilities.invokeAndWait(() -> {
            @SuppressWarnings("unchecked") JComboBox<String> tipo = (JComboBox<String>) buscar(vista[0],JComboBox.class).getFirst();
            tipo.setSelectedItem("Historial de un equipo");
            SelectorBuscable<?> selector = buscar(vista[0],SelectorBuscable.class).getFirst();
            buscar(selector,JTextField.class).getFirst().setText("ThinkPad");
            buscar(selector,JComboBox.class).getFirst().setSelectedIndex(0);
            boton(vista[0],"Consultar").doClick(0);
        });
        esperar(() -> "CAM-001".equals(consultado.get()) && boton(vista[0],"Consultar").isEnabled());
        SwingUtilities.invokeAndWait(() -> {
            JTable tabla = buscar(vista[0],JTable.class).getFirst();
            assertEquals(2,tabla.getColumnCount()); assertFalse(tabla.isCellEditable(0,0));
            buscar(vista[0],JComboBox.class).getFirst().setSelectedItem("Préstamos por fechas");
            List<JTextField> campos = buscar(vista[0],JTextField.class);
            campos.get(0).setText("2026-10-31"); campos.get(1).setText("2026-10-01");
            boton(vista[0],"Consultar").doClick(0);
            assertTrue(textos(vista[0]).contains("La fecha Hasta debe ser igual o posterior a Desde."));
            campos.get(0).setText("fecha inválida"); boton(vista[0],"Consultar").doClick(0);
            assertEquals("error",campos.get(0).getClientProperty("JComponent.outline"));
            assertEquals(0,fechas.get());
        });
    }
    private CatalogoSeleccionController catalogos() {
        return new CatalogoSeleccionController(42,Modulo.REPORTES_QR,null,null) {
            public List<EquipoOpcion> listarEquipos(boolean disponibles) { assertFalse(SwingUtilities.isEventDispatchThread()); return List.of(lenovo,dell); }
        };
    }
    private static void distribuir(Container panel) {
        panel.doLayout();
        for (Component hijo : panel.getComponents()) if (hijo instanceof Container contenedor) distribuir(contenedor);
    }
    static void esperar(BooleanSupplier condicion) throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        javax.swing.Timer[] timer = new javax.swing.Timer[1];
        SwingUtilities.invokeAndWait(() -> {
            timer[0] = new javax.swing.Timer(10,e -> { if(condicion.getAsBoolean()) { ((javax.swing.Timer)e.getSource()).stop(); listo.countDown(); } });
            timer[0].start();
        });
        try { assertTrue(listo.await(5,TimeUnit.SECONDS),"La operación no terminó"); }
        finally { SwingUtilities.invokeAndWait(() -> timer[0].stop()); }
    }
    static List<String> textos(Container c) {
        List<String> textos = new ArrayList<>(buscar(c,JLabel.class).stream().map(JLabel::getText).toList());
        textos.addAll(buscar(c,JTextArea.class).stream().map(JTextArea::getText).toList());
        return textos;
    }
    static JButton boton(Container c,String texto) { return buscar(c,JButton.class).stream().filter(b -> texto.equals(b.getText())).findFirst().orElseThrow(); }
    static <T extends Component> List<T> buscar(Container c,Class<T> tipo) {
        List<T> lista = new ArrayList<>();
        for(Component hijo:c.getComponents()) { if(tipo.isInstance(hijo)) lista.add(tipo.cast(hijo)); if(hijo instanceof Container contenedor) lista.addAll(buscar(contenedor,tipo)); }
        return lista;
    }
}

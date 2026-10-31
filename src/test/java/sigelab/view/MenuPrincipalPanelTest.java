package sigelab.view;

import org.junit.jupiter.api.Test;
import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.view.ui.UiTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MenuPrincipalPanelTest {
    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario");
        usuario.setApellido("Demo");
        return usuario;
    }

    @Test
    void creaUnaVezCadaTarjetaYConservaElFormularioAlNavegar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Map<String, Integer> creaciones = new LinkedHashMap<>();
            Map<String, JTextField> campos = new LinkedHashMap<>();
            MenuPrincipalPanel panel = new MenuPrincipalPanel(usuario(), EnumSet.allOf(Modulo.class),
                    "ADMIN", () -> { }, nombre -> {
                creaciones.merge(nombre, 1, Integer::sum);
                JPanel modulo = new JPanel();
                JTextField campo = new JTextField();
                campos.put(nombre, campo);
                modulo.add(campo);
                return modulo;
            });
            assertEquals(Map.of("Inicio", 1), creaciones, "Los demás módulos deben ser lazy");
            List<String> esperados = List.of("Inicio", "Préstamos", "Devoluciones", "Mantenimiento",
                    "Equipos", "Laboratorios", "Tipos de equipo", "QR", "Reportes", "Usuarios", "Auditoría");
            assertEquals(esperados, botones(panel).stream().map(AbstractButton::getText)
                    .filter(texto -> !texto.equals("Cerrar sesión")).toList());
            for (String nombre : esperados) boton(panel, nombre).doClick(0);
            campos.get("Equipos").setText("borrador conservado");
            boton(panel, "Equipos").doClick(0);
            boton(panel, "Inicio").doClick(0);
            boton(panel, "Equipos").doClick(0);
            assertEquals("borrador conservado", campos.get("Equipos").getText());
            assertEquals(11, creaciones.size());
            assertTrue(creaciones.values().stream().allMatch(cantidad -> cantidad == 1));
            JPanel tarjetas = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            assertInstanceOf(CardLayout.class, tarjetas.getLayout());
            assertEquals(11, tarjetas.getComponentCount());
            assertEquals(1, java.util.Arrays.stream(tarjetas.getComponents()).filter(Component::isVisible).count());
            assertTrue(SwingUtilities.isDescendingFrom(campos.get("Equipos"), tarjetas));
            assertTrue(etiquetas(panel).contains("ADMINISTRADOR"));
        });
    }

    @Test
    void mantieneSeleccionDuranteHoverYCerrarSesionDelegaUnaVez() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean cerrar = new AtomicBoolean();
            MenuPrincipalPanel panel = new MenuPrincipalPanel(usuario(), Set.of(Modulo.REPORTES_QR),
                    "ENCARGADO", () -> cerrar.set(true), nombre -> new JPanel());
            JButton inicio = boton(panel, "Inicio");
            JButton qr = boton(panel, "QR");
            qr.doClick(0);
            assertFalse(inicio.isSelected());
            assertTrue(qr.isSelected());
            qr.dispatchEvent(new MouseEvent(qr, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false));
            assertEquals(UiTheme.ACENTO, qr.getBackground());
            inicio.dispatchEvent(new MouseEvent(inicio, MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0, 0, false));
            assertEquals(UiTheme.HOVER, inicio.getBackground());
            inicio.dispatchEvent(new MouseEvent(inicio, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false));
            assertEquals(UiTheme.LATERAL, inicio.getBackground());
            assertFalse(cerrar.get());
            boton(panel, "Cerrar sesión").doClick(0);
            assertTrue(cerrar.get());
        });
    }

    @Test
    void soloMuestraOpcionesAutorizadasYNoIncluyeExtras() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MenuPrincipalPanel estudiante = new MenuPrincipalPanel(usuario(),
                    Set.of(Modulo.CONSULTA_EQUIPOS, Modulo.CONSULTA_LABORATORIOS), "ESTUDIANTE",
                    () -> { }, nombre -> new JPanel());
            assertEquals(List.of("Inicio", "Equipos", "Laboratorios", "Cerrar sesión"),
                    botones(estudiante).stream().map(AbstractButton::getText).toList());
            assertFalse(etiquetas(estudiante).contains("ADMINISTRACIÓN"));
            assertTrue(etiquetas(estudiante).contains("ESTUDIANTE"));
            MenuPrincipalPanel encargado = new MenuPrincipalPanel(usuario(),
                    Set.of(Modulo.LABORATORIOS_EQUIPOS, Modulo.RESERVAS_PRESTAMOS,
                            Modulo.MANTENIMIENTO, Modulo.REPORTES_QR, Modulo.NOTIFICACIONES),
                    "ENCARGADO", () -> { }, nombre -> new JPanel());
            List<String> opciones = botones(encargado).stream().map(AbstractButton::getText).toList();
            assertFalse(opciones.contains("Usuarios"));
            assertFalse(opciones.contains("Auditoría"));
            assertFalse(opciones.contains("Reservas"));
            assertFalse(opciones.contains("Notificaciones"));
            assertTrue(opciones.containsAll(List.of("QR", "Reportes", "Tipos de equipo")));
            MenuPrincipalPanel auditor = new MenuPrincipalPanel(usuario(), Set.of(Modulo.AUDITORIA),
                    "ADMIN", () -> { }, nombre -> new JPanel());
            assertEquals(List.of("Inicio", "Auditoría", "Cerrar sesión"),
                    botones(auditor).stream().map(AbstractButton::getText).toList());
        });
    }

    @Test
    void losTresCatalogosSonPanelesYElTemaNoReemplazaSuContenido() throws Exception {
        assertTrue(JPanel.class.isAssignableFrom(LaboratorioView.class));
        assertTrue(JPanel.class.isAssignableFrom(TipoEquipoView.class));
        assertTrue(JPanel.class.isAssignableFrom(EquipoView.class));
        SwingUtilities.invokeAndWait(() -> {
            JTable tabla = new JTable(2, 2);
            JPanel modulo = new JPanel(new BorderLayout());
            modulo.add(new JScrollPane(tabla));
            JPanel pagina = UiTheme.modulo("Equipos", "Descripción", modulo);
            assertTrue(SwingUtilities.isDescendingFrom(modulo, pagina));
            assertEquals(UiTheme.ALTO_FILA, tabla.getRowHeight());
            assertEquals(2, tabla.getRowCount());
            assertTrue(etiquetas(pagina).containsAll(List.of("Equipos", "Descripción")));
        });
    }

    private static JButton boton(Container panel, String texto) {
        return botones(panel).stream().filter(b -> texto.equals(b.getText())).findFirst().orElseThrow();
    }

    private static List<JButton> botones(Container panel) {
        List<JButton> resultado = new ArrayList<>();
        for (Component hijo : panel.getComponents()) {
            if (hijo instanceof JButton boton && boton.getText() != null && !boton.getText().isBlank()) resultado.add(boton);
            else if (hijo instanceof Container contenedor) resultado.addAll(botones(contenedor));
        }
        return resultado;
    }

    private static List<String> etiquetas(Container panel) {
        List<String> resultado = new ArrayList<>();
        for (Component hijo : panel.getComponents()) {
            if (hijo instanceof JLabel etiqueta) resultado.add(etiqueta.getText());
            else if (hijo instanceof Container contenedor) resultado.addAll(etiquetas(contenedor));
        }
        return resultado;
    }
}

package sigelab.view;

import org.junit.jupiter.api.Test;
import sigelab.controller.ReporteController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DashboardPanelTest {
    @Test
    void muestraLosOchoConteosSinConsultarEnElEdt() throws Exception {
        Map<String, Object> conteos = Map.of("equipos_TOTAL", 15, "equipos_DISPONIBLE", 5,
                "equipos_PRESTADO", 4, "equipos_DANADO", 3, "equipos_MANTENIMIENTO", 2,
                "equipos_BAJA", 1, "prestamos_ACTIVO", 8, "prestamos_VENCIDO", 6);
        comprobarCarga(conteos, false);
    }

    @Test
    void falloDeConsultaPermiteAbrirElPanelYReintentar() throws Exception {
        comprobarCarga(Map.of(), true);
    }

    private void comprobarCarga(Map<String, Object> conteos, boolean fallar) throws Exception {
        CountDownLatch liberarConsulta = new CountDownLatch(1);
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicBoolean consultaEnEdt = new AtomicBoolean(true);
        AtomicBoolean actualizacionEnEdt = new AtomicBoolean(false);
        ReporteController controller = new ReporteController(null, null, null) {
            @Override
            public Map<String, Object> dashboard() {
                consultaEnEdt.set(SwingUtilities.isEventDispatchThread());
                try {
                    if (!liberarConsulta.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("La vista bloqueó el EDT");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                if (fallar) {
                    throw new IllegalStateException("Fallo simulado sin base de datos");
                }
                return conteos;
            }
        };
        DashboardPanel[] panel = new DashboardPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new DashboardPanel(controller);
                etiquetas(panel[0]).stream()
                        .filter(etiqueta -> "Cargando resumen...".equals(etiqueta.getText()))
                        .findFirst().orElseThrow().addPropertyChangeListener("text", evento -> {
                            actualizacionEnEdt.set(SwingUtilities.isEventDispatchThread());
                            terminado.countDown();
                        });
            });
        } finally {
            liberarConsulta.countDown();
        }
        assertTrue(terminado.await(5, TimeUnit.SECONDS));
        assertFalse(consultaEnEdt.get());
        assertTrue(actualizacionEnEdt.get());
        SwingUtilities.invokeAndWait(() -> {
            List<String> textos = etiquetas(panel[0]).stream().map(JLabel::getText).toList();
            if (fallar) {
                assertTrue(textos.contains("No se pudo cargar el resumen. Puedes reintentarlo."));
                assertEquals(8, textos.stream().filter("—"::equals).count());
            } else {
                assertTrue(textos.contains("Resumen actualizado."));
                for (Object cantidad : conteos.values()) {
                    assertTrue(textos.contains(cantidad.toString()));
                }
            }
            JPanel acciones = (JPanel) panel[0].getComponent(1);
            assertTrue(((JButton) acciones.getComponent(0)).isEnabled());
        });
    }

    private List<JLabel> etiquetas(Container contenedor) {
        List<JLabel> resultado = new ArrayList<>();
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JLabel etiqueta) {
                resultado.add(etiqueta);
            } else if (componente instanceof Container hijo) {
                resultado.addAll(etiquetas(hijo));
            }
        }
        return resultado;
    }
}

package sigelab.view;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import sigelab.controller.*;
import sigelab.enums.*;
import sigelab.model.*;
import static org.junit.jupiter.api.Assertions.*;

class MantenimientoAuditoriaViewTest {
    private final CountDownLatch liberar = new CountDownLatch(1), completado = new CountDownLatch(1);
    private final AtomicBoolean actualizaEnEdt = new AtomicBoolean();
    private void consulta() {
        assertFalse(SwingUtilities.isEventDispatchThread());
        try { assertTrue(liberar.await(5,TimeUnit.SECONDS)); }
        catch (InterruptedException e) { throw new AssertionError(e); }
    }
    @Test void mantenimientoAbreYCargaFueraDelEdt() throws Exception {
        Mantenimiento m = new Mantenimiento(7,LocalDate.now(),TipoMantenimiento.CORRECTIVO,"Prueba",42,
                null,null,EstadoMantenimiento.ABIERTO,"");
        m.setIdMantenimiento(10);
        MantenimientoController controller = new MantenimientoController(null,null,42) {
            public List<Mantenimiento> listarTodos() { consulta(); return List.of(m); }
        };
        verificar(() -> new MantenimientoView(controller),10);
    }
    @Test void auditoriaAbreYCargaFueraDelEdt() throws Exception {
        Auditoria a = new Auditoria(); a.setIdAuditoria(11); a.setAccion("LOGIN_EXITOSO");
        AuditoriaController controller = new AuditoriaController(null,42) {
            public List<Auditoria> obtenerHistorial() { consulta(); return List.of(a); }
        };
        verificar(() -> new AuditoriaView(controller),11);
    }
    private void verificar(Supplier<JPanel> crear, int id) throws Exception {
        JTable[] tabla = new JTable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                tabla[0] = buscarTabla(crear.get());
                tabla[0].getModel().addTableModelListener(e -> {
                    if (tabla[0].getRowCount() == 1) {
                        actualizaEnEdt.set(SwingUtilities.isEventDispatchThread()); completado.countDown();
                    }
                });
            });
        } finally { liberar.countDown(); }
        assertTrue(completado.await(5,TimeUnit.SECONDS)); assertTrue(actualizaEnEdt.get());
        SwingUtilities.invokeAndWait(() -> assertEquals(id,tabla[0].getValueAt(0,0)));
    }
    private JTable buscarTabla(Container c) {
        for (Component hijo:c.getComponents()) {
            if (hijo instanceof JTable tabla) return tabla;
            if (hijo instanceof Container contenedor) {
                JTable encontrada = buscarTabla(contenedor); if (encontrada != null) return encontrada;
            }
        }
        return null;
    }
}

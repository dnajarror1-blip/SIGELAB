package sigelab.view;

import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import sigelab.controller.*;
import sigelab.enums.*;
import sigelab.model.*;
import sigelab.repository.DevolucionRepository.PrestamoPendiente;
import sigelab.repository.ReservaRepository.*;
import sigelab.service.DevolucionService.Pendiente;
import static org.junit.jupiter.api.Assertions.*;

class DevolucionReservaViewTest {
    @Test void devolucionesCargaSoloPendientesFueraDelEdtYSeleccionEmpiezaVacia() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1), completo = new CountDownLatch(1);
        AtomicBoolean enEdt = new AtomicBoolean(); JTable[] tabla = new JTable[1];
        DevolucionController c = new DevolucionController(null,42) {
            public List<PrestamoPendiente> listarPrestamosPendientes() {
                assertFalse(SwingUtilities.isEventDispatchThread()); esperar(liberar);
                return List.of(new PrestamoPendiente(41,"Solicitante",LocalDate.now(),LocalDate.now()));
            }
            public List<Pendiente> buscarPendientes(int prestamo) {
                assertFalse(SwingUtilities.isEventDispatchThread()); assertEquals(41,prestamo);
                DetallePrestamo d = new DetallePrestamo(41,7); d.setIdDetallePrestamo(101);
                Equipo e = new Equipo(); e.setCodigoInventario("EQ-7"); e.setNombre("Equipo pendiente");
                return List.of(new Pendiente(d,e));
            }
        };
        try {
            SwingUtilities.invokeAndWait(() -> {
                DevolucionView vista = new DevolucionView(c); tabla[0] = buscar(vista,JTable.class).getFirst();
                assertFalse(buscarBoton(vista,"Confirmar devolución").isEnabled());
                tabla[0].getModel().addTableModelListener(e -> {
                    if (tabla[0].getRowCount()==1) { enEdt.set(SwingUtilities.isEventDispatchThread()); completo.countDown(); }
                });
            });
        } finally { liberar.countDown(); }
        assertTrue(completo.await(5,TimeUnit.SECONDS)); assertTrue(enEdt.get());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("EQ-7",tabla[0].getValueAt(0,1)); assertEquals(false,tabla[0].getValueAt(0,0));
            assertEquals(CondicionDevolucion.BUENO,tabla[0].getValueAt(0,3));
        });
    }
    @Test void reservasCargaFueraDelEdtMuestraCompletadaSinBotonCompletar() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1), completo = new CountDownLatch(1);
        AtomicBoolean enEdt = new AtomicBoolean(); JTable[] tabla = new JTable[1]; ReservaView[] vista = new ReservaView[1];
        ReservaController c = new ReservaController(null,42) {
            public List<Resumen> listar() {
                assertFalse(SwingUtilities.isEventDispatchThread()); esperar(liberar);
                Reserva r = new Reserva(10,7,LocalDate.now(),LocalDate.now(),EstadoReserva.COMPLETADA,java.time.LocalDateTime.now(),"");
                r.setIdReserva(71); return List.of(new Resumen(r,"Solicitante","EQ-7","Equipo"));
            }
            public List<Solicitante> listarSolicitantes() { assertFalse(SwingUtilities.isEventDispatchThread()); return List.of(new Solicitante(10,"Solicitante")); }
            public List<EquipoReservable> listarEquipos() {
                assertFalse(SwingUtilities.isEventDispatchThread()); return List.of(new EquipoReservable(7,"EQ-7","Equipo",EstadoEquipo.PRESTADO));
            }
        };
        try {
            SwingUtilities.invokeAndWait(() -> {
                vista[0] = new ReservaView(c); tabla[0] = buscar(vista[0],JTable.class).getFirst();
                assertTrue(buscar(vista[0],JButton.class).stream().noneMatch(b -> b.getText()!=null && b.getText().contains("Completar")));
                tabla[0].getModel().addTableModelListener(e -> {
                    if (tabla[0].getRowCount()==1) { enEdt.set(SwingUtilities.isEventDispatchThread()); completo.countDown(); }
                });
            });
        } finally { liberar.countDown(); }
        assertTrue(completo.await(5,TimeUnit.SECONDS)); assertTrue(enEdt.get());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(EstadoReserva.COMPLETADA,tabla[0].getValueAt(0,6)); tabla[0].setRowSelectionInterval(0,0);
            assertFalse(buscarBoton(vista[0],"Cancelar seleccionada").isEnabled());
        });
    }
    private static void esperar(CountDownLatch latch) {
        try { assertTrue(latch.await(5,TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new AssertionError(e); }
    }
    private static JButton buscarBoton(Container c,String texto) {
        return buscar(c,JButton.class).stream().filter(b -> texto.equals(b.getText())).findFirst().orElseThrow();
    }
    private static <T extends Component> List<T> buscar(Container c,Class<T> tipo) {
        List<T> lista = new ArrayList<>();
        for (Component hijo : c.getComponents()) {
            if (tipo.isInstance(hijo)) lista.add(tipo.cast(hijo));
            if (hijo instanceof Container contenedor) lista.addAll(buscar(contenedor,tipo));
        }
        return lista;
    }
}

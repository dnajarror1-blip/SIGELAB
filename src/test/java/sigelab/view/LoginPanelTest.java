package sigelab.view;

import org.junit.jupiter.api.Test;
import sigelab.controller.LoginController;
import javax.swing.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static sigelab.view.UxSelectoresTest.*;

class LoginPanelTest {
    @Test void muestraAmbasEtapasYRecuperaControlesCuandoFallaElRostro() throws Exception {
        CountDownLatch credenciales = new CountDownLatch(1), rostro = new CountDownLatch(1);
        LoginController controller = new LoginController() {
            public ResultadoLogin iniciarSesion(String usuario,String password,Runnable antesDelRostro) {
                assertFalse(SwingUtilities.isEventDispatchThread()); assertEquals("ana",usuario); assertEquals("fixture",password);
                esperarPaso(credenciales); antesDelRostro.run(); esperarPaso(rostro);
                return new ResultadoLogin(false,"La verificación facial no fue satisfactoria. Acceso denegado.",null,Set.of(),null);
            }
        };
        LoginPanel[] panel = new LoginPanel[1]; AtomicBoolean ingreso = new AtomicBoolean();
        try {
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new LoginPanel(controller,r -> ingreso.set(true));
                assertTrue(buscar(panel[0],JComboBox.class).isEmpty(),"El rol nunca se selecciona");
                buscar(panel[0],JTextField.class).stream().filter(c -> !(c instanceof JPasswordField)).findFirst().orElseThrow().setText("ana");
                JPasswordField campo = buscar(panel[0],JPasswordField.class).getFirst(); campo.setText("fixture");
                JCheckBox mostrar = buscar(panel[0],JCheckBox.class).getFirst(); mostrar.doClick(0); assertEquals(0,campo.getEchoChar()); mostrar.doClick(0); assertNotEquals(0,campo.getEchoChar());
                boton(panel[0],"Iniciar sesión").doClick(0);
                assertTrue(textos(panel[0]).contains("Validando credenciales..."));
                assertFalse(boton(panel[0],"Iniciar sesión").isEnabled());
                assertTrue(buscar(panel[0],JProgressBar.class).getFirst().isVisible());
                assertTrue(buscar(panel[0],JProgressBar.class).getFirst().isIndeterminate());
                assertEquals(0,campo.getPassword().length);
            });
            credenciales.countDown(); esperar(() -> textos(panel[0]).contains("Verificando identidad facial..."));
            assertFalse(ingreso.get()); rostro.countDown();
            esperar(() -> boton(panel[0],"Iniciar sesión").isEnabled());
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(buscar(panel[0],JProgressBar.class).getFirst().isVisible());
                assertTrue(textos(panel[0]).contains("La verificación facial no fue satisfactoria. Acceso denegado."));
                assertFalse(ingreso.get());
            });
        } finally { credenciales.countDown(); rostro.countDown(); }
    }
    @Test void errorInesperadoNoExponeDetallesTecnicos() throws Exception {
        LoginPanel[] panel = new LoginPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new LoginPanel(new LoginController() {
                public ResultadoLogin iniciarSesion(String usuario,String password,Runnable progreso) { throw new IllegalStateException("SQL HTTP detalle privado"); }
            }, r -> fail("No debe abrir sesión"));
            buscar(panel[0],JTextField.class).getFirst().setText("ana");
            buscar(panel[0],JPasswordField.class).getFirst().setText("fixture");
            boton(panel[0],"Iniciar sesión").doClick(0);
        });
        esperar(() -> boton(panel[0],"Iniciar sesión").isEnabled());
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(textos(panel[0]).contains("No fue posible completar el inicio de sesión. Intenta de nuevo."));
            assertTrue(textos(panel[0]).stream().noneMatch(t -> t != null && t.contains("detalle privado")));
        });
    }
    private static void esperarPaso(CountDownLatch paso) {
        try { assertTrue(paso.await(5,TimeUnit.SECONDS)); }
        catch (InterruptedException e) { throw new AssertionError(e); }
    }
}

package sigelab.view;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import sigelab.exception.BusinessException;

final class OperacionModulo {
    private OperacionModulo() { }

    static <T> void ejecutar(Component vista, Callable<T> tarea, Consumer<T> completada, Runnable terminada) {
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return tarea.call(); }
            protected void done() {
                try {
                    T resultado = get();
                    if (!(vista instanceof java.awt.Window ventana) || ventana.isDisplayable()) completada.accept(resultado);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    String mensaje = e.getCause() instanceof BusinessException
                            ? e.getCause().getMessage()
                            : "No se pudo completar la operación. Compruebe su estado antes de reintentar.";
                    if (!(vista instanceof java.awt.Window ventana) || ventana.isDisplayable())
                        JOptionPane.showMessageDialog(vista, mensaje, "SIGELAB", JOptionPane.ERROR_MESSAGE);
                } finally {
                    terminada.run();
                }
            }
        }.execute();
    }
}

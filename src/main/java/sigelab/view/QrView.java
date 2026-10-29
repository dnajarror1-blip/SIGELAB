package sigelab.view;

import sigelab.controller.QrController;
import sigelab.exception.BusinessException;
import sigelab.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QrView extends JPanel {
    private final QrController controller;
    private final JTextField codigo = new JTextField(20);
    private final JTextArea salida = new JTextArea(10, 45);
    private final JButton generar = new JButton("Generar QR");
    private final JButton leer = new JButton("Leer QR");

    public QrView(Usuario usuario) {
        controller = new QrController(usuario);
        setLayout(new BorderLayout(10, 10));
        JPanel acciones = new JPanel();
        acciones.add(new JLabel("Código de inventario:"));
        acciones.add(codigo);
        acciones.add(generar);
        acciones.add(leer);
        generar.addActionListener(e -> generar());
        leer.addActionListener(e -> leer());
        salida.setEditable(false);
        add(acciones, BorderLayout.NORTH);
        add(new JScrollPane(salida), BorderLayout.CENTER);
    }

    private void generar() {
        JFileChooser selector = new JFileChooser();
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".png");
        }
        if (archivo.exists() && JOptionPane.showConfirmDialog(this,
                "El archivo ya existe. ¿Deseas reemplazarlo?", "Generar QR",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        File destino = archivo;
        String inventario = codigo.getText();
        ejecutar(() -> {
            controller.generar(inventario, destino);
            return "QR generado para el equipo " + inventario.trim();
        });
    }

    private void leer() {
        JFileChooser selector = new JFileChooser();
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File imagen = selector.getSelectedFile();
        ejecutar(() -> {
            var resultado = controller.identificar(imagen);
            var equipo = resultado.equipo();
            String disponibilidad = resultado.disponibleParaUso() ? "Disponible para uso."
                    : resultado.bloqueadoPorPrestamo() ? "Bloqueado: el equipo está prestado."
                    : "El equipo no está disponible para uso.";
            return "Código: " + resultado.codigoInventario() + "\nEquipo: " + equipo.getNombre()
                    + "\nMarca: " + equipo.getMarca() + "\nModelo: " + equipo.getModelo()
                    + "\nEstado: " + equipo.getEstado() + "\n" + disponibilidad;
        });
    }

    private void ejecutar(Supplier<String> tarea) {
        generar.setEnabled(false);
        leer.setEnabled(false);
        salida.setText("Procesando...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return tarea.get();
            }

            @Override
            protected void done() {
                generar.setEnabled(true);
                leer.setEnabled(true);
                try {
                    salida.setText(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    salida.setText("Operación interrumpida.");
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    Logger.getLogger(QrView.class.getName()).log(Level.WARNING,
                            "Falló una operación QR ({0})", causa.getClass().getName());
                    String mensaje = causa instanceof BusinessException || causa instanceof IllegalArgumentException
                            ? causa.getMessage() : "No se pudo completar la operación. Contacta al administrador.";
                    salida.setText(mensaje);
                    JOptionPane.showMessageDialog(QrView.this, mensaje, "QR", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}

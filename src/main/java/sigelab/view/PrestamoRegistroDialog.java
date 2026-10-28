package sigelab.view;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.*;

import sigelab.controller.PrestamoController;
import sigelab.enums.EstadoPrestamo;
import sigelab.exception.BusinessException;
import sigelab.model.Prestamo;

public class PrestamoRegistroDialog extends JDialog {

    private final PrestamoController controller;
    private final int idUsuarioActual;

    private final JTextField txtSolicitante = new JTextField();
    private final JTextField txtFechaPrestamo =
            new JTextField(LocalDate.now().toString());
    private final JTextField txtFechaDevolucion =
            new JTextField(LocalDate.now().plusDays(3).toString());
    private final JTextField txtEquipos = new JTextField();
    private final JTextArea txtObservaciones = new JTextArea(4, 25);

    private final JButton btnGuardar = new JButton("Registrar préstamo");
    private final JButton btnCancelar = new JButton("Cancelar");
    private final JLabel lblEstado = new JLabel(" ");

    private boolean registrado;
    private boolean guardando;

    public PrestamoRegistroDialog(
            Window propietario,
            PrestamoController controller,
            int idUsuarioActual
    ) {
        super(
                propietario,
                "Registrar préstamo",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        this.controller = controller;
        this.idUsuarioActual = idUsuarioActual;

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel formulario = new JPanel(new GridLayout(0, 1, 5, 5));
        formulario.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 5, 15)
        );

        formulario.add(new JLabel("ID del usuario solicitante:"));
        formulario.add(txtSolicitante);

        formulario.add(new JLabel("Fecha del préstamo (AAAA-MM-DD):"));
        formulario.add(txtFechaPrestamo);

        formulario.add(new JLabel("Devolución esperada (AAAA-MM-DD):"));
        formulario.add(txtFechaDevolucion);

        formulario.add(new JLabel("IDs de equipos separados por comas:"));
        formulario.add(txtEquipos);

        formulario.add(new JLabel("Observaciones (máximo 250 caracteres):"));

        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);

        JPanel contenido = new JPanel(new BorderLayout(5, 5));
        contenido.add(formulario, BorderLayout.NORTH);

        JScrollPane scrollObservaciones =
                new JScrollPane(txtObservaciones);
        scrollObservaciones.setBorder(
                BorderFactory.createTitledBorder("Observaciones")
        );

        contenido.add(scrollObservaciones, BorderLayout.CENTER);
        add(contenido, BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout());

        JPanel botones = new JPanel();
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        inferior.add(lblEstado, BorderLayout.NORTH);
        inferior.add(botones, BorderLayout.SOUTH);
        inferior.setBorder(
                BorderFactory.createEmptyBorder(5, 15, 10, 15)
        );

        add(inferior, BorderLayout.SOUTH);

        btnGuardar.addActionListener(e -> registrarPrestamo());
        btnCancelar.addActionListener(e -> cerrarSiEsPosible());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cerrarSiEsPosible();
            }
        });

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    private void registrarPrestamo() {
        final Prestamo prestamo;
        final List<Integer> idsEquipo = new ArrayList<>();

        try {
            int idSolicitante =
                    Integer.parseInt(txtSolicitante.getText().trim());

            LocalDate fechaPrestamo =
                    LocalDate.parse(txtFechaPrestamo.getText().trim());

            LocalDate fechaDevolucion =
                    LocalDate.parse(txtFechaDevolucion.getText().trim());

            String equipos = txtEquipos.getText().trim();

            if (!equipos.isEmpty()) {
                for (String id : equipos.split(",", -1)) {
                    idsEquipo.add(Integer.parseInt(id.trim()));
                }
            }

            prestamo = new Prestamo(
                    idSolicitante,
                    idUsuarioActual,
                    fechaPrestamo,
                    fechaDevolucion,
                    EstadoPrestamo.ACTIVO,
                    txtObservaciones.getText().trim()
            );

        } catch (NumberFormatException e) {
            mostrarAviso(
                    "El solicitante y los equipos deben ser números enteros. "
                            + "Separa los equipos con comas, por ejemplo: 2, 3, 4."
            );
            return;

        } catch (DateTimeParseException e) {
            mostrarAviso(
                    "Escribe fechas válidas con el formato AAAA-MM-DD."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas registrar el préstamo con los datos ingresados?",
                "Confirmar préstamo",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        cambiarEstadoGuardado(true);
        lblEstado.setText("Registrando préstamo...");

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return controller.crearPrestamo(prestamo, idsEquipo);
            }

            @Override
            protected void done() {
                boolean permitirReintento = true;

                try {
                    int idPrestamo = get();
                    registrado = true;

                    JOptionPane.showMessageDialog(
                            PrestamoRegistroDialog.this,
                            "Préstamo registrado. ID: " + idPrestamo,
                            "Registro exitoso",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    dispose();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    permitirReintento = false;
                    mostrarErrorTecnico();

                } catch (ExecutionException e) {
                    if (e.getCause() instanceof BusinessException negocio) {
                        lblEstado.setText("Revisa los datos del préstamo.");
                        mostrarAviso(negocio.getMessage());
                    } else {
                        permitirReintento = false;
                        mostrarErrorTecnico();
                    }

                } finally {
                    cambiarEstadoGuardado(false);

                    if (!permitirReintento) {
                        btnGuardar.setEnabled(false);
                    }
                }
            }
        }.execute();
    }

    private void mostrarErrorTecnico() {
        lblEstado.setText("Comprueba el estado del préstamo antes de repetirlo.");

        JOptionPane.showMessageDialog(
                this,
                "No se pudo confirmar el resultado del registro. "
                        + "Cierra este formulario y consulta los préstamos "
                        + "antes de volver a intentarlo.",
                "Registro de préstamo",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void cambiarEstadoGuardado(boolean ocupado) {
        guardando = ocupado;

        btnGuardar.setEnabled(!ocupado);
        btnCancelar.setEnabled(!ocupado);

        txtSolicitante.setEnabled(!ocupado);
        txtFechaPrestamo.setEnabled(!ocupado);
        txtFechaDevolucion.setEnabled(!ocupado);
        txtEquipos.setEnabled(!ocupado);
        txtObservaciones.setEnabled(!ocupado);
    }

    private void cerrarSiEsPosible() {
        if (!guardando) {
            dispose();
        }
    }

    private void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Validación",
                JOptionPane.WARNING_MESSAGE
        );
    }

    public boolean fueRegistrado() {
        return registrado;
    }
}
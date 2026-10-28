package sigelab.view;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import sigelab.controller.PrestamoController;
import sigelab.enums.EstadoPrestamo;
import sigelab.exception.BusinessException;
import sigelab.model.Equipo;
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
    private final JTextArea txtObservaciones = new JTextArea(4, 30);

    private final JButton btnGuardar =
            new JButton("Registrar préstamo");

    private final JButton btnCancelar = new JButton("Cancelar");

    private final JButton btnQr =
            new JButton("Agregar equipo desde QR");

    private final JLabel lblEstado = new JLabel(" ");

    private boolean registrado;
    private boolean ocupado;
    private boolean resultadoIncierto;

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
        formulario.add(btnQr);

        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);

        JScrollPane scrollObservaciones =
                new JScrollPane(txtObservaciones);

        scrollObservaciones.setBorder(
                BorderFactory.createTitledBorder(
                        "Observaciones (máximo 250 caracteres)"
                )
        );

        JPanel contenido = new JPanel(new BorderLayout(5, 5));
        contenido.add(formulario, BorderLayout.NORTH);
        contenido.add(scrollObservaciones, BorderLayout.CENTER);

        add(contenido, BorderLayout.CENTER);

        JPanel botones = new JPanel();
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBorder(
                BorderFactory.createEmptyBorder(5, 15, 10, 15)
        );
        inferior.add(lblEstado, BorderLayout.NORTH);
        inferior.add(botones, BorderLayout.SOUTH);

        add(inferior, BorderLayout.SOUTH);

        btnGuardar.addActionListener(e -> registrarPrestamo());
        btnCancelar.addActionListener(e -> cerrarSiEsPosible());
        btnQr.addActionListener(e -> agregarEquipoDesdeQr());

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

    private List<Integer> leerIdsEquipo() {
        List<Integer> ids = new ArrayList<>();
        String texto = txtEquipos.getText().trim();

        if (!texto.isEmpty()) {
            for (String valor : texto.split(",", -1)) {
                int id = Integer.parseInt(valor.trim());

                if (id <= 0) {
                    throw new NumberFormatException();
                }

                ids.add(id);
            }
        }

        return ids;
    }

    private void registrarPrestamo() {
        final Prestamo prestamo;
        final List<Integer> idsEquipo;

        try {
            int idSolicitante =
                    Integer.parseInt(txtSolicitante.getText().trim());

            LocalDate fechaPrestamo =
                    LocalDate.parse(txtFechaPrestamo.getText().trim());

            LocalDate fechaDevolucion =
                    LocalDate.parse(txtFechaDevolucion.getText().trim());

            idsEquipo = leerIdsEquipo();

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
                    "Escribe un solicitante válido y equipos con IDs "
                            + "enteros positivos separados por comas."
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
                    mostrarErrorTecnico();

                } catch (ExecutionException e) {
                    if (e.getCause() instanceof BusinessException negocio) {
                        lblEstado.setText("Revisa los datos del préstamo.");
                        mostrarAviso(negocio.getMessage());
                    } else {
                        mostrarErrorTecnico();
                    }

                } finally {
                    cambiarEstadoGuardado(false);
                }
            }
        }.execute();
    }

    private void agregarEquipoDesdeQr() {
        final List<Integer> equiposActuales;

        try {
            equiposActuales = leerIdsEquipo();

        } catch (NumberFormatException e) {
            mostrarAviso(
                    "Corrige los IDs de equipos antes de agregar uno desde QR."
            );
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Selecciona la imagen del QR");
        selector.setFileFilter(
                new FileNameExtensionFilter(
                        "Imágenes PNG y JPG", "png", "jpg", "jpeg"
                )
        );

        if (selector.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File imagen = selector.getSelectedFile();

        cambiarEstadoGuardado(true);
        lblEstado.setText("Leyendo QR y consultando equipo...");

        new SwingWorker<Equipo, Void>() {
            @Override
            protected Equipo doInBackground() {
                return controller.identificarEquipoPorQr(imagen);
            }

            @Override
            protected void done() {
                try {
                    Equipo equipo = get();

                    if (equiposActuales.contains(equipo.getIdEquipo())) {
                        lblEstado.setText("El equipo ya estaba agregado.");
                        mostrarAviso(
                                "Ese equipo ya está incluido en el préstamo."
                        );
                        return;
                    }

                    int respuesta = confirmarEquipo(equipo);

                    if (respuesta != JOptionPane.YES_OPTION) {
                        lblEstado.setText("No se agregó el equipo.");
                        return;
                    }

                    equiposActuales.add(equipo.getIdEquipo());

                    txtEquipos.setText(
                            equiposActuales.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(", "))
                    );

                    lblEstado.setText(
                            "Equipo agregado: " + equipo.getCodigoInventario()
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblEstado.setText("Lectura interrumpida.");

                } catch (ExecutionException e) {
                    lblEstado.setText("No se agregó ningún equipo.");
                    Throwable causa = e.getCause();

                    if (causa instanceof BusinessException
                            || causa instanceof IllegalArgumentException) {
                        mostrarAviso(causa.getMessage());
                    } else {
                        mostrarAviso(
                                "No se pudo procesar el QR. Revisa la imagen "
                                        + "y la conexión con la base de datos."
                        );
                    }

                } finally {
                    cambiarEstadoGuardado(false);
                }
            }
        }.execute();
    }

    private int confirmarEquipo(Equipo equipo) {
        JPanel datos = new JPanel(new GridLayout(0, 2, 10, 6));

        agregarDato(datos, "ID:", equipo.getIdEquipo());
        agregarDato(datos, "Código de inventario:",
                equipo.getCodigoInventario());
        agregarDato(datos, "Nombre:", equipo.getNombre());
        agregarDato(datos, "Marca:", equipo.getMarca());
        agregarDato(datos, "Modelo:", equipo.getModelo());
        agregarDato(datos, "Estado:", equipo.getEstado());

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.add(datos, BorderLayout.CENTER);
        contenido.add(
                new JLabel("¿Agregar este equipo al préstamo?"),
                BorderLayout.SOUTH
        );

        return JOptionPane.showConfirmDialog(
                this,
                contenido,
                "Equipo identificado por QR",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
    }

    private void agregarDato(JPanel panel, String etiqueta, Object valor) {
        panel.add(new JLabel(etiqueta));

        String texto = valor == null || valor.toString().isBlank()
                ? "Sin dato"
                : valor.toString();

        JTextField campo = new JTextField(texto, 25);
        campo.setEditable(false);
        panel.add(campo);
    }

    private void mostrarErrorTecnico() {
        resultadoIncierto = true;

        lblEstado.setText(
                "Comprueba el estado del préstamo antes de repetirlo."
        );

        JOptionPane.showMessageDialog(
                this,
                "No se pudo confirmar el resultado del registro. "
                        + "Cierra este formulario y consulta los préstamos "
                        + "antes de volver a intentarlo.",
                "Registro de préstamo",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void cambiarEstadoGuardado(boolean enProceso) {
        ocupado = enProceso;

        boolean permitirEdicion = !enProceso && !resultadoIncierto;

        btnGuardar.setEnabled(permitirEdicion);
        btnQr.setEnabled(permitirEdicion);
        btnCancelar.setEnabled(!enProceso);

        txtSolicitante.setEnabled(permitirEdicion);
        txtFechaPrestamo.setEnabled(permitirEdicion);
        txtFechaDevolucion.setEnabled(permitirEdicion);
        txtEquipos.setEnabled(permitirEdicion);
        txtObservaciones.setEnabled(permitirEdicion);
    }

    private void cerrarSiEsPosible() {
        if (!ocupado) {
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
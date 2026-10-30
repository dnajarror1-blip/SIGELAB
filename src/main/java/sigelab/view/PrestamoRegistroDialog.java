package sigelab.view;

import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import sigelab.controller.PrestamoController;
import sigelab.enums.EstadoPrestamo;
import sigelab.exception.BusinessException;
import sigelab.model.Equipo;
import sigelab.model.Prestamo;

public class PrestamoRegistroDialog extends JDialog {

    private static final Color FONDO = new Color(242, 245, 249);
    private static final Color MARINO = new Color(24, 42, 65);
    private static final Color PETROLEO = new Color(20, 105, 103);
    private static final Color TEXTO = new Color(38, 52, 70);
    private static final Color SUAVE = new Color(100, 116, 139);
    private static final Color BORDE = new Color(218, 225, 234);

    private static final Font FUENTE =
            new Font("Segoe UI", Font.PLAIN, 13);

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

    private final JLabel lblEstado = new JLabel("Completa los datos del préstamo.");

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

        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBackground(FONDO);
        raiz.setBorder(new EmptyBorder(18, 18, 16, 18));
        setContentPane(raiz);

        JPanel cabecera = new JPanel(new GridLayout(0, 1, 0, 6));
        cabecera.setBackground(MARINO);
        cabecera.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel titulo = new JLabel("Nuevo préstamo");
        titulo.setFont(FUENTE.deriveFont(Font.BOLD, 23f));
        titulo.setForeground(Color.WHITE);

        JLabel descripcion = new JLabel(
                "Selecciona los equipos y define su fecha de devolución."
        );
        descripcion.setFont(FUENTE);
        descripcion.setForeground(new Color(204, 216, 230));

        cabecera.add(titulo);
        cabecera.add(descripcion);
        raiz.add(cabecera, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        new EmptyBorder(16, 18, 16, 18)
                )
        );

        prepararCampo(txtSolicitante);
        prepararCampo(txtFechaPrestamo);
        prepararCampo(txtFechaDevolucion);
        prepararCampo(txtEquipos);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        int fila = 0;

        agregarFila(formulario, gbc, fila++,
                etiqueta("Usuario solicitante · ID"));
        agregarFila(formulario, gbc, fila++, txtSolicitante);

        JPanel fechas = new JPanel(new GridLayout(1, 2, 14, 0));
        fechas.setOpaque(false);
        fechas.add(grupoCampo("Fecha de préstamo", txtFechaPrestamo));
        fechas.add(grupoCampo("Devolución prevista", txtFechaDevolucion));
        agregarFila(formulario, gbc, fila++, fechas);

        JLabel ayudaFechas = etiqueta("Formato de fechas: AAAA-MM-DD");
        ayudaFechas.setForeground(SUAVE);
        agregarFila(formulario, gbc, fila++, ayudaFechas);

        agregarFila(formulario, gbc, fila++,
                etiqueta("Equipos · IDs separados por comas"));
        agregarFila(formulario, gbc, fila++, txtEquipos);

        JLabel ayudaEquipos = etiqueta(
                "Ejemplo: 5, 6. Para quitar un equipo, borra su ID de la lista."
        );
        ayudaEquipos.setForeground(SUAVE);
        agregarFila(formulario, gbc, fila++, ayudaEquipos);

        estilizarBoton(btnQr, Color.WHITE, PETROLEO);
        agregarFila(formulario, gbc, fila++, btnQr);

        agregarFila(formulario, gbc, fila++,
                etiqueta("Observaciones · máximo 250 caracteres"));

        txtObservaciones.setFont(FUENTE);
        txtObservaciones.setForeground(TEXTO);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        txtObservaciones.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane observaciones = new JScrollPane(txtObservaciones);
        observaciones.setBorder(BorderFactory.createLineBorder(BORDE));
        agregarFila(formulario, gbc, fila, observaciones);

        JScrollPane scrollFormulario = new JScrollPane(formulario);
        scrollFormulario.setBorder(BorderFactory.createEmptyBorder());
        scrollFormulario.getViewport().setBackground(Color.WHITE);
        scrollFormulario.getVerticalScrollBar().setUnitIncrement(16);
        raiz.add(scrollFormulario, BorderLayout.CENTER);

        estilizarBoton(btnCancelar, Color.WHITE, MARINO);
        estilizarBoton(btnGuardar, PETROLEO, Color.WHITE);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        lblEstado.setFont(FUENTE);
        lblEstado.setForeground(SUAVE);

        JPanel inferior = new JPanel(new BorderLayout(0, 12));
        inferior.setOpaque(false);
        inferior.add(lblEstado, BorderLayout.NORTH);
        inferior.add(botones, BorderLayout.SOUTH);
        raiz.add(inferior, BorderLayout.SOUTH);

        btnGuardar.addActionListener(e -> registrarPrestamo());
        btnCancelar.addActionListener(e -> cerrarSiEsPosible());
        btnQr.addActionListener(e -> agregarEquipoDesdeQr());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cerrarSiEsPosible();
            }
        });

        Rectangle pantalla = getGraphicsConfiguration().getBounds();
        Insets margenes = Toolkit.getDefaultToolkit().getScreenInsets(
                getGraphicsConfiguration()
        );

        int anchoDisponible = pantalla.width - margenes.left - margenes.right;
        int altoDisponible = pantalla.height - margenes.top - margenes.bottom;

        setSize(
                Math.min(640, anchoDisponible),
                Math.min(720, altoDisponible)
        );
        setLocationRelativeTo(getOwner());
    }

    private JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    private JPanel grupoCampo(String titulo, JTextField campo) {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.add(etiqueta(titulo), BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        return panel;
    }

    private void agregarFila(
            JPanel panel,
            GridBagConstraints gbc,
            int fila,
            Component componente
    ) {
        gbc.gridy = fila;
        panel.add(componente, gbc);
    }

    private void prepararCampo(JTextField campo) {
        campo.setFont(FUENTE);
        campo.setForeground(TEXTO);
        campo.setBackground(Color.WHITE);
        campo.setCaretColor(PETROLEO);
        campo.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        new EmptyBorder(8, 10, 8, 10)
                )
        );
    }

    private void estilizarBoton(
            JButton boton,
            Color fondo,
            Color texto
    ) {
        boton.setFont(FUENTE.deriveFont(Font.BOLD));
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                fondo.equals(Color.WHITE) ? BORDE : fondo
                        ),
                        new EmptyBorder(10, 14, 10, 14)
                )
        );
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
            if (idsEquipo.isEmpty()) {
                mostrarAviso("El préstamo debe incluir al menos un equipo.");
                return;
            }
            if (new java.util.HashSet<>(idsEquipo).size() != idsEquipo.size()) {
                mostrarAviso("El mismo equipo no puede repetirse en el préstamo.");
                return;
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

        int respuesta = mostrarDialogo(
                "¿Deseas registrar el préstamo con los datos ingresados?",
                "Confirmar préstamo",
                JOptionPane.QUESTION_MESSAGE,
                new String[]{"Sí", "No"},
                "No"
        );

        if (respuesta != 0) {
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

                    mostrarDialogo(
                            "Préstamo registrado. Número: " + idPrestamo,
                            "Registro exitoso",
                            JOptionPane.INFORMATION_MESSAGE,
                            new String[]{"Aceptar"},
                            "Aceptar"
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
        selector.setLocale(Locale.forLanguageTag("es-GT"));
        selector.updateUI();
        selector.setDialogTitle("Seleccionar imagen del código QR");
        selector.setApproveButtonText("Abrir imagen");
        selector.setApproveButtonToolTipText("Leer el código QR de esta imagen");
        selector.setAcceptAllFileFilterUsed(false);
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

                    if (confirmarEquipo(equipo) != 0) {
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
        JPanel datos = new JPanel(new GridLayout(0, 2, 12, 8));
        datos.setBackground(Color.WHITE);

        agregarDato(datos, "ID del equipo", equipo.getIdEquipo());
        agregarDato(datos, "Código de inventario", equipo.getCodigoInventario());
        agregarDato(datos, "Nombre", equipo.getNombre());
        agregarDato(datos, "Marca", equipo.getMarca());
        agregarDato(datos, "Modelo", equipo.getModelo());
        agregarDato(datos, "Estado", "Disponible");

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBackground(Color.WHITE);
        contenido.add(datos, BorderLayout.CENTER);
        contenido.add(
                etiqueta("¿Agregar este equipo al préstamo?"),
                BorderLayout.SOUTH
        );

        return mostrarDialogo(
                contenido,
                "Equipo identificado por QR",
                JOptionPane.QUESTION_MESSAGE,
                new String[]{"Sí", "No"},
                "No"
        );
    }

    private void agregarDato(JPanel panel, String nombre, Object valor) {
        panel.add(etiqueta(nombre));

        String texto = valor == null || valor.toString().isBlank()
                ? "Sin dato"
                : valor.toString();

        JTextField campo = new JTextField(texto, 24);
        prepararCampo(campo);
        campo.setEditable(false);
        campo.setBackground(FONDO);
        panel.add(campo);
    }

    private int mostrarDialogo(
            Object contenido,
            String titulo,
            int tipo,
            String[] opciones,
            String opcionInicial
    ) {
        JOptionPane panel = new JOptionPane(
                contenido,
                tipo,
                JOptionPane.DEFAULT_OPTION,
                null,
                opciones,
                opcionInicial
        );

        aplicarEstiloAviso(panel);
        JDialog dialogo = panel.createDialog(this, titulo);
        dialogo.setVisible(true);

        Object seleccion = panel.getValue();
        dialogo.dispose();

        for (int i = 0; i < opciones.length; i++) {
            if (opciones[i].equals(seleccion)) {
                return i;
            }
        }

        return JOptionPane.CLOSED_OPTION;
    }

    private void aplicarEstiloAviso(Component componente) {
        componente.setFont(FUENTE);

        if (componente instanceof JPanel
                || componente instanceof JOptionPane) {
            componente.setBackground(Color.WHITE);
        }

        if (componente instanceof JLabel) {
            componente.setForeground(TEXTO);
        }

        if (componente instanceof JButton boton) {
            boolean principal = "Sí".equals(boton.getText())
                    || "Aceptar".equals(boton.getText());

            estilizarBoton(
                    boton,
                    principal ? PETROLEO : Color.WHITE,
                    principal ? Color.WHITE : MARINO
            );
        }

        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                aplicarEstiloAviso(hijo);
            }
        }
    }

    private void mostrarErrorTecnico() {
        resultadoIncierto = true;
        lblEstado.setText("Consulta los préstamos antes de volver a intentarlo.");

        mostrarDialogo(
                "No se pudo confirmar el resultado del registro. "
                        + "Cierra este formulario y consulta los préstamos "
                        + "antes de volver a intentarlo.",
                "Registro de préstamo",
                JOptionPane.ERROR_MESSAGE,
                new String[]{"Aceptar"},
                "Aceptar"
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
        mostrarDialogo(
                mensaje,
                "Revisar datos",
                JOptionPane.WARNING_MESSAGE,
                new String[]{"Aceptar"},
                "Aceptar"
        );
    }

    public boolean fueRegistrado() {
        return registrado;
    }
}
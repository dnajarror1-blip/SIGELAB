package sigelab.view;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import sigelab.controller.PrestamoController;
import sigelab.enums.EstadoPrestamo;
import sigelab.exception.BusinessException;
import sigelab.model.DetallePrestamo;
import sigelab.model.Prestamo;

public class PrestamoView extends JPanel {

    private static final Color FONDO = new Color(242, 245, 249);
    private static final Color MARINO = new Color(24, 42, 65);
    private static final Color PETROLEO = new Color(20, 105, 103);
    private static final Color TEXTO = new Color(38, 52, 70);
    private static final Color SUAVE = new Color(100, 116, 139);
    private static final Color BORDE = new Color(218, 225, 234);
    private static final Color FILA_ALTERNA = new Color(247, 249, 252);
    private static final Color SELECCION = new Color(218, 235, 242);

    private static final Font FUENTE =
            new Font("Segoe UI", Font.PLAIN, 13);

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PrestamoController controller;
    private final int idUsuarioActual;

    private final JTextField txtIdPrestamo = new JTextField(12);

    private final JButton btnBuscar = new JButton("Buscar préstamo");
    private final JButton btnActualizar = new JButton("Ver activos");
    private final JButton btnNuevo = new JButton("Nuevo préstamo");
    private final JButton btnVerEquipos = new JButton("Ver equipos");

    private final JLabel lblEstado = new JLabel("Preparando consulta...");
    private final JLabel lblCantidad = new JLabel("0 registros");

    private JTable tabla;
    private boolean cargando;

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new String[]{
                            "N.º préstamo",
                            "ID solicitante",
                            "ID de quien registra",
                            "Fecha de préstamo",
                            "Devolución prevista",
                            "Estado",
                            "Observaciones"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columna) {
                    if (columna <= 2) {
                        return Integer.class;
                    }
                    if (columna == 3 || columna == 4) {
                        return LocalDate.class;
                    }
                    return Object.class;
                }
            };

    public PrestamoView(int idUsuarioActual) {
        this(new PrestamoController(), idUsuarioActual);
    }

    public PrestamoView(
            PrestamoController controller,
            int idUsuarioActual
    ) {
        this.controller = controller;
        this.idUsuarioActual = idUsuarioActual;

        inicializarComponentes();
        cargarDatos();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(0, 18));
        setBackground(FONDO);
        setBorder(new EmptyBorder(22, 22, 18, 22));

        JPanel superior = new JPanel(new BorderLayout(0, 16));
        superior.setOpaque(false);

        JPanel cabecera = new JPanel(new BorderLayout(15, 10));
        cabecera.setBackground(MARINO);
        cabecera.setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 6));
        textos.setOpaque(false);

        JLabel titulo = new JLabel("Gestión de préstamos");
        titulo.setFont(FUENTE.deriveFont(Font.BOLD, 25f));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel(
                "Registro, consulta y seguimiento de equipos prestados"
        );
        subtitulo.setFont(FUENTE);
        subtitulo.setForeground(new Color(204, 216, 230));

        textos.add(titulo);
        textos.add(subtitulo);
        cabecera.add(textos, BorderLayout.CENTER);

        JPanel herramientas = new JPanel(new GridLayout(0, 1, 0, 8));
        herramientas.setBackground(Color.WHITE);
        herramientas.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        new EmptyBorder(12, 14, 12, 14)
                )
        );

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        busqueda.setOpaque(false);

        JLabel etiqueta = new JLabel("Número de préstamo");
        etiqueta.setFont(FUENTE);
        etiqueta.setForeground(TEXTO);

        txtIdPrestamo.setFont(FUENTE);
        txtIdPrestamo.setPreferredSize(new Dimension(140, 36));
        txtIdPrestamo.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        new EmptyBorder(7, 9, 7, 9)
                )
        );

        estilizarBoton(btnBuscar, MARINO, Color.WHITE);
        estilizarBoton(btnActualizar, Color.WHITE, MARINO);
        estilizarBoton(btnNuevo, PETROLEO, Color.WHITE);
        estilizarBoton(btnVerEquipos, Color.WHITE, MARINO);

        busqueda.add(etiqueta);
        busqueda.add(txtIdPrestamo);
        busqueda.add(btnBuscar);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnNuevo);
        acciones.add(btnActualizar);
        acciones.add(btnVerEquipos);

        herramientas.add(busqueda);
        herramientas.add(acciones);

        superior.add(cabecera, BorderLayout.NORTH);
        superior.add(herramientas, BorderLayout.CENTER);
        add(superior, BorderLayout.NORTH);

        JPanel tarjetaTabla = new JPanel(new BorderLayout());
        tarjetaTabla.setBackground(Color.WHITE);
        tarjetaTabla.setBorder(BorderFactory.createLineBorder(BORDE));

        JPanel tituloTabla = new JPanel(new BorderLayout());
        tituloTabla.setBackground(Color.WHITE);
        tituloTabla.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel listado = new JLabel("Listado de préstamos");
        listado.setFont(FUENTE.deriveFont(Font.BOLD, 15f));
        listado.setForeground(TEXTO);

        lblCantidad.setFont(FUENTE);
        lblCantidad.setForeground(SUAVE);

        tituloTabla.add(listado, BorderLayout.WEST);
        tituloTabla.add(lblCantidad, BorderLayout.EAST);

        tabla = new JTable(modeloTabla);
        configurarTabla(tabla);

        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] anchos = {110, 120, 160, 150, 155, 115, 330};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i)
                    .setPreferredWidth(anchos[i]);
        }

        tabla.getSelectionModel().addListSelectionListener(
                e -> actualizarBotonDetalles()
        );

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        tarjetaTabla.add(tituloTabla, BorderLayout.NORTH);
        tarjetaTabla.add(scroll, BorderLayout.CENTER);
        add(tarjetaTabla, BorderLayout.CENTER);

        lblEstado.setFont(FUENTE);
        lblEstado.setForeground(SUAVE);
        lblEstado.setBorder(new EmptyBorder(0, 2, 0, 2));
        add(lblEstado, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> buscarPorId());
        txtIdPrestamo.addActionListener(e -> buscarPorId());
        btnActualizar.addActionListener(e -> {
            txtIdPrestamo.setText("");
            cargarDatos();
        });
        btnNuevo.addActionListener(e -> mostrarRegistro());
        btnVerEquipos.addActionListener(e -> mostrarEquipos());

        actualizarBotonDetalles();
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
                        new EmptyBorder(9, 14, 9, 14)
                )
        );
    }

    private void configurarTabla(JTable tablaObjetivo) {
        tablaObjetivo.setFont(FUENTE);
        tablaObjetivo.setRowHeight(36);
        tablaObjetivo.setShowGrid(false);
        tablaObjetivo.setIntercellSpacing(new Dimension(0, 0));
        tablaObjetivo.setFillsViewportHeight(true);
        tablaObjetivo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaObjetivo.setAutoCreateRowSorter(true);
        tablaObjetivo.setSelectionBackground(SELECCION);
        tablaObjetivo.setSelectionForeground(MARINO);

        tablaObjetivo.getTableHeader().setFont(
                FUENTE.deriveFont(Font.BOLD)
        );
        tablaObjetivo.getTableHeader().setBackground(
                new Color(230, 236, 243)
        );
        tablaObjetivo.getTableHeader().setForeground(MARINO);
        tablaObjetivo.getTableHeader().setPreferredSize(
                new Dimension(0, 40)
        );
        tablaObjetivo.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer renderizador =
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tablaActual,
                            Object valor,
                            boolean seleccionado,
                            boolean tieneFoco,
                            int fila,
                            int columna
                    ) {
                        super.getTableCellRendererComponent(
                                tablaActual, valor, seleccionado,
                                tieneFoco, fila, columna
                        );

                        setBorder(new EmptyBorder(0, 12, 0, 12));
                        setFont(FUENTE);
                        setHorizontalAlignment(SwingConstants.LEFT);

                        if (valor instanceof LocalDate fecha) {
                            setText(fecha.format(FORMATO_FECHA));
                        } else if (valor == EstadoPrestamo.ACTIVO) {
                            setText("Activo");
                        } else if (valor == EstadoPrestamo.FINALIZADO) {
                            setText("Finalizado");
                        }

                        if (!seleccionado) {
                            setBackground(
                                    fila % 2 == 0
                                            ? Color.WHITE
                                            : FILA_ALTERNA
                            );
                            setForeground(
                                    valor == EstadoPrestamo.ACTIVO
                                            ? PETROLEO
                                            : TEXTO
                            );
                        }

                        return this;
                    }
                };

        tablaObjetivo.setDefaultRenderer(Object.class, renderizador);
        tablaObjetivo.setDefaultRenderer(Integer.class, renderizador);
        tablaObjetivo.setDefaultRenderer(LocalDate.class, renderizador);
    }

    public void cargarDatos() {
        consultar(null);
    }

    private void buscarPorId() {
        try {
            int id = Integer.parseInt(txtIdPrestamo.getText().trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            consultar(id);

        } catch (NumberFormatException e) {
            mostrarMensaje(
                    "Escribe un número de préstamo entero mayor que cero.",
                    "Revisar búsqueda",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void consultar(Integer idPrestamo) {
        cambiarEstadoCarga(true);
        lblEstado.setText("Consultando préstamos...");

        new SwingWorker<List<Prestamo>, Void>() {
            @Override
            protected List<Prestamo> doInBackground() {
                if (idPrestamo == null) {
                    return controller.buscarActivos();
                }

                return controller.buscarPorId(idPrestamo)
                        .map(List::of)
                        .orElseGet(List::of);
            }

            @Override
            protected void done() {
                try {
                    List<Prestamo> prestamos = get();
                    modeloTabla.setRowCount(0);

                    for (Prestamo prestamo : prestamos) {
                        modeloTabla.addRow(new Object[]{
                                prestamo.getIdPrestamo(),
                                prestamo.getIdUsuarioSolicitante(),
                                prestamo.getIdUsuarioRegistra(),
                                prestamo.getFechaPrestamo(),
                                prestamo.getFechaEsperadaDevolucion(),
                                prestamo.getEstado(),
                                prestamo.getObservaciones()
                        });
                    }

                    lblCantidad.setText(
                            prestamos.size() == 1
                                    ? "1 registro"
                                    : prestamos.size() + " registros"
                    );

                    lblEstado.setText(
                            prestamos.isEmpty()
                                    ? "No se encontraron préstamos."
                                    : "Consulta completada. Selecciona una fila "
                                    + "para ver sus equipos."
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblEstado.setText("Consulta interrumpida.");

                } catch (ExecutionException e) {
                    lblEstado.setText(
                            "La consulta falló. Se conserva el listado anterior."
                    );
                    mostrarError(
                            e.getCause(),
                            "No se pudieron consultar los préstamos. "
                                    + "Revisa la conexión con la base de datos."
                    );

                } finally {
                    cambiarEstadoCarga(false);
                }
            }
        }.execute();
    }

    private void mostrarRegistro() {
        PrestamoRegistroDialog dialogo = new PrestamoRegistroDialog(
                SwingUtilities.getWindowAncestor(this),
                controller,
                idUsuarioActual
        );

        dialogo.setVisible(true);

        if (dialogo.fueRegistrado()) {
            cargarDatos();
        }
    }

    private void mostrarEquipos() {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            mostrarMensaje(
                    "Selecciona un préstamo de la tabla.",
                    "Ver equipos",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(fila);
        int idPrestamo = ((Number) modeloTabla.getValueAt(filaModelo, 0))
                .intValue();

        cambiarEstadoCarga(true);
        lblEstado.setText("Consultando equipos del préstamo...");

        new SwingWorker<List<DetallePrestamo>, Void>() {
            @Override
            protected List<DetallePrestamo> doInBackground() {
                return controller.buscarDetalles(idPrestamo);
            }

            @Override
            protected void done() {
                try {
                    List<DetallePrestamo> detalles = get();

                    DefaultTableModel modelo = new DefaultTableModel(
                            new String[]{
                                    "N.º detalle", "N.º préstamo", "ID equipo"
                            },
                            0
                    ) {
                        @Override
                        public boolean isCellEditable(int fila, int columna) {
                            return false;
                        }

                        @Override
                        public Class<?> getColumnClass(int columna) {
                            return Integer.class;
                        }
                    };

                    for (DetallePrestamo detalle : detalles) {
                        modelo.addRow(new Object[]{
                                detalle.getIdDetallePrestamo(),
                                detalle.getIdPrestamo(),
                                detalle.getIdEquipo()
                        });
                    }

                    JTable tablaDetalles = new JTable(modelo);
                    configurarTabla(tablaDetalles);

                    JScrollPane scroll = new JScrollPane(tablaDetalles);
                    scroll.setPreferredSize(new Dimension(540, 230));
                    scroll.getViewport().setBackground(Color.WHITE);
                    scroll.setBorder(
                            BorderFactory.createLineBorder(BORDE)
                    );

                    lblEstado.setText(
                            "Préstamo " + idPrestamo + ": "
                                    + detalles.size() + " equipo(s)."
                    );

                    mostrarMensaje(
                            scroll,
                            "Equipos del préstamo " + idPrestamo,
                            JOptionPane.PLAIN_MESSAGE
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblEstado.setText("Consulta interrumpida.");

                } catch (ExecutionException e) {
                    lblEstado.setText(
                            "No se pudieron consultar los equipos."
                    );
                    mostrarError(
                            e.getCause(),
                            "No se pudieron consultar los equipos del préstamo. "
                                    + "Revisa la conexión con la base de datos."
                    );

                } finally {
                    cambiarEstadoCarga(false);
                }
            }
        }.execute();
    }

    private void mostrarError(Throwable causa, String mensajeGeneral) {
        String mensaje = causa instanceof BusinessException
                ? causa.getMessage()
                : mensajeGeneral;

        mostrarMensaje(
                mensaje,
                "Consulta de préstamos",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void mostrarMensaje(Object contenido, String titulo, int tipo) {
        JOptionPane.showOptionDialog(
                this,
                contenido,
                titulo,
                JOptionPane.DEFAULT_OPTION,
                tipo,
                null,
                new String[]{"Aceptar"},
                "Aceptar"
        );
    }

    private void cambiarEstadoCarga(boolean enProceso) {
        cargando = enProceso;

        btnBuscar.setEnabled(!enProceso);
        btnActualizar.setEnabled(!enProceso);
        btnNuevo.setEnabled(!enProceso);
        txtIdPrestamo.setEnabled(!enProceso);

        actualizarBotonDetalles();
    }

    private void actualizarBotonDetalles() {
        btnVerEquipos.setEnabled(
                !cargando && tabla != null && tabla.getSelectedRow() >= 0
        );
    }
}
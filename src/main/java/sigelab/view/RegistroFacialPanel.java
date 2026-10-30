package sigelab.view;

import sigelab.service.FacialAuthClient;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * SIGELAB - Panel de diagnóstico facial.
 *
 * Funcionalidades:
 * - Consultar la disponibilidad del servicio FaceID.
 * - Consultar los metadatos de un perfil facial.
 * - Recibir un usuario seleccionado desde otro módulo.
 * - Mostrar las respuestas del servicio Python.
 *
 * Restricciones:
 * - No registra rostros.
 * - No modifica perfiles faciales.
 * - No autoriza inicios de sesión.
 * - No ejecuta operaciones SQL directamente.
 *
 * La autorización de acceso al panel corresponde
 * al módulo de seguridad de SIGELAB.
 */
public final class RegistroFacialPanel extends JPanel {

    // ============================================
    // CLIENTE FACIAL
    // ============================================

    private final FacialAuthClient clienteFacial;

    // ============================================
    // COMPONENTES
    // ============================================

    private final JTextField txtIdUsuario =
            new JTextField(10);

    private final JButton btnConsultarServicio =
            new JButton("Comprobar servicio");

    private final JButton btnConsultarPerfil =
            new JButton("Consultar perfil");

    private final JTextArea txtResultado =
            new JTextArea();

    // ============================================
    // CONSTRUCTOR PRINCIPAL
    // ============================================

    public RegistroFacialPanel(
            FacialAuthClient clienteFacial
    ) {

        this.clienteFacial = Objects.requireNonNull(
                clienteFacial,
                "El cliente FaceID es obligatorio"
        );

        configurarInterfaz();
        configurarEventos();
    }

    // ============================================
    // CONSTRUCTOR CON USUARIO SELECCIONADO
    // ============================================

    /**
     * Permite abrir el panel con un usuario
     * previamente seleccionado.
     */
    public RegistroFacialPanel(
            FacialAuthClient clienteFacial,
            int idUsuario
    ) {

        this(clienteFacial);

        seleccionarUsuario(idUsuario);
    }

    // ============================================
    // SELECCIONAR USUARIO
    // ============================================

    /**
     * Establece el identificador del usuario.
     *
     * No realiza consultas HTTP ni operaciones SQL.
     */
    public void seleccionarUsuario(int idUsuario) {

        if (idUsuario <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del usuario debe ser positivo"
            );
        }

        txtIdUsuario.setText(
                Integer.toString(idUsuario)
        );
    }

    // ============================================
    // CONFIGURACIÓN DE LA INTERFAZ
    // ============================================

    private void configurarInterfaz() {

        setLayout(
                new BorderLayout(15, 15)
        );

        setBorder(
                BorderFactory.createEmptyBorder(
                        25,
                        25,
                        25,
                        25
                )
        );

        // ----------------------------------------
        // ENCABEZADO
        // ----------------------------------------

        JLabel lblTitulo =
                new JLabel("Administración FaceID");

        lblTitulo.setFont(
                lblTitulo.getFont().deriveFont(
                        Font.BOLD,
                        24f
                )
        );

        JLabel lblDescripcion = new JLabel(
                "Consultas de diagnóstico. "
                        + "El registro facial todavía no está habilitado."
        );

        JPanel encabezado = new JPanel();

        encabezado.setLayout(
                new BoxLayout(
                        encabezado,
                        BoxLayout.Y_AXIS
                )
        );

        encabezado.add(lblTitulo);

        encabezado.add(
                Box.createVerticalStrut(10)
        );

        encabezado.add(lblDescripcion);

        // ----------------------------------------
        // CONTROLES
        // ----------------------------------------

        JPanel controles = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        controles.add(
                btnConsultarServicio
        );

        controles.add(
                new JLabel("ID de usuario:")
        );

        controles.add(
                txtIdUsuario
        );

        controles.add(
                btnConsultarPerfil
        );

        // ----------------------------------------
        // ÁREA DE RESULTADOS
        // ----------------------------------------

        txtResultado.setEditable(false);

        txtResultado.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        13
                )
        );

        txtResultado.setLineWrap(true);

        txtResultado.setWrapStyleWord(true);

        txtResultado.setText(
                "FaceID listo para realizar consultas."
        );

        JScrollPane desplazamiento =
                new JScrollPane(txtResultado);

        // ----------------------------------------
        // ORGANIZACIÓN DE COMPONENTES
        // ----------------------------------------

        JPanel superior = new JPanel(
                new BorderLayout(10, 15)
        );

        superior.add(
                encabezado,
                BorderLayout.NORTH
        );

        superior.add(
                controles,
                BorderLayout.SOUTH
        );

        add(
                superior,
                BorderLayout.NORTH
        );

        add(
                desplazamiento,
                BorderLayout.CENTER
        );
    }

    // ============================================
    // CONFIGURACIÓN DE EVENTOS
    // ============================================

    private void configurarEventos() {

        btnConsultarServicio.addActionListener(
                evento -> ejecutarConsulta(
                        clienteFacial::consultarSalud
                )
        );

        btnConsultarPerfil.addActionListener(
                evento -> consultarPerfil()
        );
    }

    // ============================================
    // CONSULTAR PERFIL
    // ============================================

    private void consultarPerfil() {

        String texto =
                txtIdUsuario.getText().trim();

        int idUsuario;

        try {

            idUsuario = Integer.parseInt(texto);

            if (idUsuario <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException error) {

            txtResultado.setText(
                    "Debe ingresar un ID de usuario "
                            + "entero y positivo."
            );

            return;
        }

        final int idValidado = idUsuario;

        ejecutarConsulta(
                () -> clienteFacial.consultarEstado(
                        idValidado
                )
        );
    }

    // ============================================
    // COMUNICACIÓN CON FACEID
    // ============================================

    /**
     * Ejecuta las solicitudes HTTP fuera
     * del hilo principal de Swing.
     *
     * De esta manera la ventana no se congela
     * mientras espera la respuesta de Python.
     */
    private void ejecutarConsulta(
            Supplier<FacialAuthClient.RespuestaFacial> operacion
    ) {

        btnConsultarServicio.setEnabled(false);

        btnConsultarPerfil.setEnabled(false);

        txtResultado.setText(
                "Consultando servicio FaceID..."
        );

        SwingWorker<
                FacialAuthClient.RespuestaFacial,
                Void
        > trabajador = new SwingWorker<>() {

            @Override
            protected FacialAuthClient.RespuestaFacial
            doInBackground() {

                return operacion.get();
            }

            @Override
            protected void done() {

                try {

                    FacialAuthClient.RespuestaFacial respuesta =
                            get();

                    txtResultado.setText(
                            "Código HTTP: "
                                    + respuesta.codigoHttp()
                                    + "\n\n"
                                    + respuesta.cuerpo()
                    );

                    txtResultado.setCaretPosition(0);

                } catch (InterruptedException error) {

                    Thread.currentThread().interrupt();

                    txtResultado.setText(
                            "La consulta fue interrumpida."
                    );

                } catch (ExecutionException error) {

                    txtResultado.setText(
                            "No fue posible completar "
                                    + "la consulta FaceID.\n"
                                    + "Compruebe que el servicio esté disponible."
                    );

                } finally {

                    btnConsultarServicio.setEnabled(true);

                    btnConsultarPerfil.setEnabled(true);
                }
            }
        };

        trabajador.execute();
    }
}
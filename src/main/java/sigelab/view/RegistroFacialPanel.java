package sigelab.view;

import sigelab.service.FacialAuthClient;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Panel de diagnóstico facial de SIGELAB.
 *
 * Esta versión:
 * - Consulta la disponibilidad de FaceID.
 * - Consulta los metadatos de un perfil facial.
 * - No registra rostros.
 * - No modifica perfiles.
 * - No autoriza inicios de sesión.
 *
 * El acceso a este panel deberá controlarse
 * desde el módulo de seguridad de SIGELAB.
 */
public final class RegistroFacialPanel extends JPanel {

    private final FacialAuthClient clienteFacial;

    private final JTextField txtIdUsuario =
            new JTextField(10);

    private final JButton btnConsultarServicio =
            new JButton("Comprobar servicio");

    private final JButton btnConsultarPerfil =
            new JButton("Consultar perfil");

    private final JTextArea txtResultado =
            new JTextArea();

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

    private void configurarInterfaz() {

        setLayout(new BorderLayout(15, 15));

        setBorder(
                BorderFactory.createEmptyBorder(
                        25, 25, 25, 25
                )
        );

        // Encabezado.
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

        // Controles.
        JPanel controles =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        controles.add(btnConsultarServicio);

        controles.add(
                new JLabel("ID de usuario:")
        );

        controles.add(txtIdUsuario);
        controles.add(btnConsultarPerfil);

        // Área de resultados.
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

        // Organización principal.
        JPanel superior =
                new JPanel(new BorderLayout(10, 15));

        superior.add(
                encabezado,
                BorderLayout.NORTH
        );

        superior.add(
                controles,
                BorderLayout.SOUTH
        );

        add(superior, BorderLayout.NORTH);

        add(
                desplazamiento,
                BorderLayout.CENTER
        );
    }

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

    /**
     * Ejecuta la comunicación HTTP fuera
     * del hilo de interfaz de Swing.
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
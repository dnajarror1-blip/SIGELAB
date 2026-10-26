package sigelab.view;

import sigelab.model.Usuario;
import sigelab.service.Modulo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class MenuPrincipalForm extends JFrame {

    // Colores en un solo lugar, igual que en el login
    private static final Color LATERAL = new Color(17, 24, 39);        // barra oscura
    private static final Color HOVER = new Color(55, 65, 81);          // al pasar el mouse
    private static final Color TEXTO = new Color(17, 24, 39);
    private static final Color TEXTO_SUAVE = new Color(107, 114, 128);
    private static final Color TEXTO_CLARO = new Color(209, 213, 219); // gris claro para la barra

    // El usuario que tiene la sesión abierta
    private final Usuario usuario;

    // El área de la derecha: aquí se cambia lo que se muestra según el botón
    private final JPanel contenido = new JPanel(new BorderLayout());

    public MenuPrincipalForm(Usuario usuario, Set<Modulo> menu) {
        this.usuario = usuario;
        setTitle("SIGELAB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ---------- Barra lateral (izquierda) ----------
        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBackground(LATERAL);
        lateral.setBorder(BorderFactory.createEmptyBorder(30, 15, 25, 15));
        lateral.setPreferredSize(new Dimension(260, 0));

        JLabel logo = new JLabel("SIGELAB");
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 24f));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUsuario = new JLabel(usuario.getNombre() + " " + usuario.getApellido());
        lblUsuario.setForeground(Color.WHITE);
        lblUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblRol = new JLabel(nombreRol(usuario.getIdRol()));
        lblRol.setForeground(TEXTO_CLARO);
        lblRol.setFont(lblRol.getFont().deriveFont(12f));
        lblRol.setAlignmentX(Component.LEFT_ALIGNMENT);

        lateral.add(logo);
        lateral.add(Box.createVerticalStrut(18));
        lateral.add(lblUsuario);
        lateral.add(lblRol);
        lateral.add(Box.createVerticalStrut(30));

        // Un botón por cada opción que le toca a este rol
        for (Modulo m : menu) {
            JButton boton = crearBotonLateral(nombreVisible(m));
            boton.addActionListener(e -> mostrar(m));
            lateral.add(boton);
            lateral.add(Box.createVerticalStrut(6));
        }

        // Este espacio elástico empuja "Cerrar sesión" hasta abajo
        lateral.add(Box.createVerticalGlue());

        JButton btnSalir = crearBotonLateral("Cerrar sesión");
        btnSalir.addActionListener(e -> cerrarSesion());
        lateral.add(btnSalir);

        // ---------- Área de contenido (derecha) ----------
        contenido.setBackground(Color.WHITE);
        contenido.setBorder(BorderFactory.createEmptyBorder(40, 45, 40, 45));

        // Al abrir, mostramos un saludo
        cambiarContenido(crearTexto("Hola, " + usuario.getNombre(),
                "Elige una opción del menú de la izquierda."));

        // ---------- Armamos la ventana ----------
        setLayout(new BorderLayout());
        add(lateral, BorderLayout.WEST);
        add(contenido, BorderLayout.CENTER);

        setMinimumSize(new Dimension(1000, 650));
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // se abre ocupando toda la pantalla
    }

    // Decide qué se muestra a la derecha según el botón que se presionó
    private void mostrar(Modulo m) {
        if (m == Modulo.USUARIOS) {
            cambiarContenido(new UsuariosPanel(usuario));
        } else {
            // Las demás secciones las hacen otros integrantes; por ahora un aviso
            cambiarContenido(crearTexto(nombreVisible(m),
                    "Esta sección todavía no está conectada."));
        }
    }

    // Quita lo que había a la derecha y pone lo nuevo
    private void cambiarContenido(JComponent nuevo) {
        contenido.removeAll();
        contenido.add(nuevo, BorderLayout.CENTER);
        contenido.revalidate();
        contenido.repaint();
    }

    // Un título grande con una línea de texto gris debajo
    private JComponent crearTexto(String titulo, String detalle) {
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 28f));
        lblTitulo.setForeground(TEXTO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDetalle = new JLabel(detalle);
        lblDetalle.setFont(lblDetalle.getFont().deriveFont(15f));
        lblDetalle.setForeground(TEXTO_SUAVE);
        lblDetalle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel columna = new JPanel();
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.setOpaque(false);
        columna.add(lblTitulo);
        columna.add(Box.createVerticalStrut(8));
        columna.add(lblDetalle);

        // Lo pegamos arriba para que no quede estirado
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        envoltura.add(columna, BorderLayout.NORTH);
        return envoltura;
    }

    // Crea un botón plano para la barra lateral, con un fondo suave al pasar el mouse
    private JButton crearBotonLateral(String texto) {
        JButton b = new JButton(texto);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(Color.WHITE);
        b.setBackground(HOVER);
        b.setFont(b.getFont().deriveFont(14f));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);   // sin fondo mientras el mouse no esté encima
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setContentAreaFilled(false);
            }
        });
        return b;
    }

    // Cierra esta ventana y vuelve a mostrar el login
    private void cerrarSesion() {
        dispose();
        new LoginForm().setVisible(true);
    }

    // Los nombres con que se ve cada opción en pantalla
    private String nombreVisible(Modulo m) {
        return switch (m) {
            case USUARIOS -> "Usuarios";
            case ENROLLMENT_FACIAL -> "Registro facial";
            case AUDITORIA -> "Auditoría";
            case LABORATORIOS_EQUIPOS -> "Laboratorios y equipos";
            case RESERVAS_PRESTAMOS -> "Reservas y préstamos";
            case MANTENIMIENTO -> "Mantenimiento";
            case REPORTES_QR -> "Reportes y QR";
            case NOTIFICACIONES -> "Notificaciones";
        };
    }

    // El nombre del rol según su id (1 = ADMIN, 2 = ENCARGADO)
    private String nombreRol(int idRol) {
        return switch (idRol) {
            case 1 -> "Administrador";
            case 2 -> "Encargado";
            default -> "Usuario";
        };
    }
}
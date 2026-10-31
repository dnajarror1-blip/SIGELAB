package sigelab.view;

import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.view.ui.UiTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Navegación de la sesión, comprobable sin crear ventanas ni consultar servicios. */
public class MenuPrincipalPanel extends JPanel {
    private final Usuario usuario;
    private final Set<Modulo> menu;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final Map<String, JComponent> modulos = new LinkedHashMap<>();
    private final Map<String, JButton> botones = new LinkedHashMap<>();
    private final Function<String, JComponent> crearModulo;
    private JButton activo;

    public MenuPrincipalPanel(Usuario usuario, Set<Modulo> menu, String nombreRol, Runnable cerrarSesion) {
        this(usuario, menu, nombreRol, cerrarSesion, null);
    }

    // La fábrica alternativa permite probar navegación sin SQL, modelos ni cámara.
    MenuPrincipalPanel(Usuario usuario, Set<Modulo> menu, String nombreRol, Runnable cerrarSesion,
                       Function<String, JComponent> crearModulo) {
        super(new BorderLayout());
        this.usuario = usuario;
        this.menu = Set.copyOf(menu);
        this.crearModulo = crearModulo == null ? this::crearModuloReal : crearModulo;
        JPanel lateral = new JPanel(new BorderLayout(0, 14));
        lateral.setBackground(UiTheme.LATERAL);
        lateral.setBorder(BorderFactory.createEmptyBorder(22, 14, 18, 14));
        lateral.setPreferredSize(new Dimension(245, 0));
        JLabel logo = new JLabel("SIGELAB");
        logo.setFont(UiTheme.TITULO);
        logo.setForeground(Color.WHITE);
        lateral.add(logo, BorderLayout.NORTH);

        JPanel opciones = new JPanel();
        opciones.setLayout(new BoxLayout(opciones, BoxLayout.Y_AXIS));
        opciones.setBackground(UiTheme.LATERAL);
        seccion(opciones, "INICIO");
        opcion(opciones, "Inicio");
        if (menu.contains(Modulo.RESERVAS_PRESTAMOS) || menu.contains(Modulo.MANTENIMIENTO)) {
            seccion(opciones, "OPERACIONES");
            if (menu.contains(Modulo.RESERVAS_PRESTAMOS)) {
                opcion(opciones, "Préstamos");
                opcion(opciones, "Devoluciones");
            }
            if (menu.contains(Modulo.MANTENIMIENTO)) opcion(opciones, "Mantenimiento");
        }
        boolean catalogos = menu.contains(Modulo.LABORATORIOS_EQUIPOS);
        if (catalogos || menu.contains(Modulo.CONSULTA_EQUIPOS) || menu.contains(Modulo.CONSULTA_LABORATORIOS)) {
            seccion(opciones, "INVENTARIO");
            if (catalogos || menu.contains(Modulo.CONSULTA_EQUIPOS)) opcion(opciones, "Equipos");
            if (catalogos || menu.contains(Modulo.CONSULTA_LABORATORIOS)) opcion(opciones, "Laboratorios");
            if (catalogos) opcion(opciones, "Tipos de equipo");
        }
        if (menu.contains(Modulo.REPORTES_QR)) {
            seccion(opciones, "CONSULTAS");
            opcion(opciones, "QR");
            opcion(opciones, "Reportes");
        }
        if (menu.contains(Modulo.USUARIOS) || menu.contains(Modulo.AUDITORIA)) {
            seccion(opciones, "ADMINISTRACIÓN");
            if (menu.contains(Modulo.USUARIOS)) opcion(opciones, "Usuarios");
            if (menu.contains(Modulo.AUDITORIA)) opcion(opciones, "Auditoría");
        }
        JScrollPane scroll = new JScrollPane(opciones);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UiTheme.LATERAL);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        lateral.add(scroll, BorderLayout.CENTER);

        JPanel sesion = new JPanel();
        sesion.setLayout(new BoxLayout(sesion, BoxLayout.Y_AXIS));
        sesion.setOpaque(false);
        JLabel nombre = new JLabel(usuario.getNombre() + " " + usuario.getApellido());
        nombre.setForeground(Color.WHITE);
        nombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel rol = new JLabel("ADMIN".equals(nombreRol) ? "ADMINISTRADOR" : nombreRol);
        rol.setForeground(new Color(191, 219, 254));
        rol.setFont(UiTheme.FUENTE_SECUNDARIA.deriveFont(Font.BOLD));
        rol.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton salir = botonLateral("Cerrar sesión");
        salir.addActionListener(e -> cerrarSesion.run());
        sesion.add(nombre);
        sesion.add(Box.createVerticalStrut(5));
        sesion.add(rol);
        sesion.add(Box.createVerticalStrut(12));
        sesion.add(salir);
        lateral.add(sesion, BorderLayout.SOUTH);
        add(lateral, BorderLayout.WEST);
        add(contenido, BorderLayout.CENTER);
        mostrar("Inicio");
    }

    private void seccion(JPanel panel, String nombre) {
        JLabel titulo = new JLabel(nombre);
        titulo.setFont(UiTheme.FUENTE_SECUNDARIA.deriveFont(Font.BOLD, 11f));
        titulo.setForeground(new Color(156, 163, 175));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(Box.createVerticalStrut(14));
        panel.add(titulo);
        panel.add(Box.createVerticalStrut(6));
    }

    private void opcion(JPanel panel, String nombre) {
        JButton boton = botonLateral(nombre);
        botones.put(nombre, boton);
        boton.addActionListener(e -> mostrar(nombre));
        panel.add(boton);
        panel.add(Box.createVerticalStrut(3));
    }

    private JButton botonLateral(String texto) {
        JButton boton = new JButton(texto);
        boton.setFont(UiTheme.FUENTE_SECUNDARIA);
        boton.setForeground(Color.WHITE);
        boton.setBackground(UiTheme.LATERAL);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setFocusPainted(true);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        boton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (boton != activo) boton.setBackground(UiTheme.HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                boton.setBackground(boton == activo ? UiTheme.ACENTO : UiTheme.LATERAL);
            }
        });
        return boton;
    }

    private void mostrar(String nombre) {
        JComponent modulo = modulos.get(nombre);
        if (modulo == null) {
            modulo = crearModulo.apply(nombre);
            contenido.add(UiTheme.modulo(nombre, descripcion(nombre), modulo), nombre);
            modulos.put(nombre, modulo);
        } else {
            // Antes se obtenían datos nuevos al recrear estas vistas. Conservar esa posibilidad.
            if (modulo instanceof LaboratorioView vista) vista.refrescar();
            if (modulo instanceof TipoEquipoView vista) vista.refrescar();
            if (modulo instanceof EquipoView vista) vista.refrescar();
        }
        tarjetas.show(contenido, nombre);
        if (activo != null) {
            activo.setSelected(false);
            activo.setBackground(UiTheme.LATERAL);
        }
        activo = botones.get(nombre);
        activo.setSelected(true);
        activo.setBackground(UiTheme.ACENTO);
    }

    private JComponent crearModuloReal(String nombre) {
        boolean soloLectura = !menu.contains(Modulo.LABORATORIOS_EQUIPOS);
        return switch (nombre) {
            case "Inicio" -> menu.contains(Modulo.REPORTES_QR) ? new DashboardPanel(usuario)
                    : new JLabel("Hola, " + usuario.getNombre() + ". Elige una opción del menú.");
            case "Préstamos" -> new PrestamoView(usuario.getIdUsuario());
            case "Devoluciones" -> new DevolucionView(usuario.getIdUsuario());
            case "Mantenimiento" -> new MantenimientoView(usuario.getIdUsuario());
            case "Equipos" -> new EquipoView(soloLectura);
            case "Laboratorios" -> new LaboratorioView(soloLectura);
            case "Tipos de equipo" -> new TipoEquipoView();
            case "QR" -> new QrView(usuario);
            case "Reportes" -> new ReportesView(usuario);
            case "Usuarios" -> new UsuariosPanel(usuario);
            case "Auditoría" -> new AuditoriaView(usuario.getIdUsuario());
            default -> throw new IllegalArgumentException("Módulo no disponible");
        };
    }

    private String descripcion(String nombre) {
        return switch (nombre) {
            case "Inicio" -> "Consulta el resumen de actividad de SIGELAB.";
            case "Préstamos" -> "Registra y consulta los equipos prestados.";
            case "Devoluciones" -> "Registra la devolución de los equipos pendientes.";
            case "Mantenimiento" -> "Consulta y gestiona el mantenimiento de los equipos.";
            case "Equipos" -> "Administra y consulta el inventario de los laboratorios.";
            case "Laboratorios" -> "Consulta los espacios y sus datos de laboratorio.";
            case "Tipos de equipo" -> "Organiza los tipos de equipo del inventario.";
            case "QR" -> "Genera y consulta códigos QR de los equipos.";
            case "Reportes" -> "Consulta los reportes de actividad e inventario.";
            case "Usuarios" -> "Administra los usuarios y su acceso al sistema.";
            case "Auditoría" -> "Consulta el historial de acciones del sistema.";
            default -> "";
        };
    }
}

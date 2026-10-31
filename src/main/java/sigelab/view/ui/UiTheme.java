package sigelab.view.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** Estilo compartido por la navegación; conserva el Look and Feel FlatLaf. */
public final class UiTheme {
    public static final Color FONDO = new Color(245, 247, 250);
    public static final Color TARJETA = Color.WHITE;
    public static final Color LATERAL = new Color(17, 24, 39);
    public static final Color HOVER = new Color(38, 50, 68);
    public static final Color ACENTO = new Color(37, 99, 235);
    public static final Color TEXTO = new Color(17, 24, 39);
    public static final Color SECUNDARIO = new Color(107, 114, 128);
    public static final Color BORDE = new Color(225, 230, 237);
    public static final int PADDING = 20;
    public static final int ALTO_FILA = 32;
    public static final Font TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 28);
    public static final Font FUENTE_SECUNDARIA = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

    private UiTheme() { }

    public static Border bordeTarjeta() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    public static void botonPrimario(JButton boton) {
        botonSecundario(boton);
        boton.setBackground(ACENTO);
        boton.setForeground(Color.WHITE);
    }

    public static void botonSecundario(JButton boton) {
        boton.setFont(FUENTE_SECUNDARIA);
        boton.setBackground(TARJETA);
        boton.setForeground(TEXTO);
        boton.putClientProperty("JButton.buttonType", "roundRect");
        boton.setMargin(new Insets(7, 12, 7, 12));
    }

    public static void tabla(JTable tabla) {
        tabla.setRowHeight(ALTO_FILA);
        tabla.setFillsViewportHeight(true);
        tabla.setGridColor(BORDE);
        tabla.getTableHeader().setFont(FUENTE_SECUNDARIA.deriveFont(Font.BOLD));
    }

    public static void aplicar(Component componente) {
        if (componente instanceof JScrollPane scroll) {
            if (scroll.getViewport().getView() != null) aplicar(scroll.getViewport().getView());
            return; // No modificar botones internos de las barras de desplazamiento.
        }
        if (componente instanceof JTable tabla) {
            tabla(tabla);
            return;
        }
        if (componente instanceof JButton boton) {
            botonSecundario(boton);
            String texto = boton.getText();
            if (texto != null && (texto.startsWith("Guardar") || texto.startsWith("Registrar")
                    || texto.startsWith("Nuevo") || texto.startsWith("Crear"))) botonPrimario(boton);
        }
        if (componente instanceof JPanel panel && panel.isOpaque()) panel.setBackground(TARJETA);
        if (componente instanceof JPanel contenedor) {
            for (Component hijo : contenedor.getComponents()) aplicar(hijo);
        }
    }

    public static JPanel modulo(String titulo, String descripcion, JComponent contenido) {
        aplicar(contenido);
        JPanel pagina = new JPanel(new BorderLayout(0, 16));
        pagina.setBackground(FONDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        JPanel cabecera = new JPanel();
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.Y_AXIS));
        cabecera.setOpaque(false);
        JLabel nombre = new JLabel(titulo);
        nombre.setFont(TITULO);
        nombre.setForeground(TEXTO);
        JLabel detalle = new JLabel(descripcion);
        detalle.setFont(FUENTE_SECUNDARIA);
        detalle.setForeground(SECUNDARIO);
        nombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        detalle.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabecera.add(nombre);
        cabecera.add(Box.createVerticalStrut(6));
        cabecera.add(detalle);
        cabecera.add(Box.createVerticalStrut(14));
        JSeparator separador = new JSeparator();
        separador.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabecera.add(separador);
        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(TARJETA);
        tarjeta.setBorder(bordeTarjeta());
        tarjeta.add(contenido, BorderLayout.CENTER);
        pagina.add(cabecera, BorderLayout.NORTH);
        pagina.add(tarjeta, BorderLayout.CENTER);
        return pagina;
    }
}
